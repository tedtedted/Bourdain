package com.tedredington.bourdain.civicdata.internal;

import java.util.Optional;

import com.tedredington.bourdain.civicdata.SyncSource;
import com.tedredington.bourdain.civicdata.SyncStatus;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** {@code sync_run} bookkeeping: run lifecycle plus watermark lookup. */
@Repository
class SyncRuns implements SyncStatus {

    private final JdbcClient jdbc;

    SyncRuns(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Marks leftover RUNNING rows as failed. Safe because only one sync runs at
     * a time in a single instance; a RUNNING row at sync start means a previous
     * process died mid-run.
     */
    int failAbandoned() {
        return jdbc.sql("""
                        update sync_run
                        set finished_at = now(), status = 'FAILED', message = 'abandoned (process restart)'
                        where status = 'RUNNING'
                        """)
                .update();
    }

    long start(SyncSource source) {
        return jdbc.sql("""
                        insert into sync_run (source, started_at, status)
                        values (:source, now(), 'RUNNING') returning id
                        """)
                .param("source", source.name())
                .query(Long.class)
                .single();
    }

    void complete(long runId, int rowsUpserted, int rowsSkipped, String watermark) {
        jdbc.sql("""
                        update sync_run
                        set finished_at = now(), status = 'SUCCEEDED',
                            rows_upserted = :rows, rows_skipped = :skipped, watermark = :watermark
                        where id = :id
                        """)
                .param("rows", rowsUpserted)
                .param("skipped", rowsSkipped)
                .param("watermark", watermark)
                .param("id", runId)
                .update();
    }

    void fail(long runId, String message) {
        jdbc.sql("""
                        update sync_run
                        set finished_at = now(), status = 'FAILED', message = :message
                        where id = :id
                        """)
                .param("message", message)
                .param("id", runId)
                .update();
    }

    /** Watermark of the most recent successful run, if any. */
    Optional<String> lastWatermark(SyncSource source) {
        return jdbc.sql("""
                        select watermark from sync_run
                        where source = :source and status = 'SUCCEEDED' and watermark is not null
                        order by started_at desc limit 1
                        """)
                .param("source", source.name())
                .query(String.class)
                .optional();
    }

    @Override
    public Optional<LastSync> lastSuccessful(SyncSource source) {
        return jdbc.sql("""
                        select finished_at, rows_upserted from sync_run
                        where source = :source and status = 'SUCCEEDED'
                        order by started_at desc limit 1
                        """)
                .param("source", source.name())
                .query((rs, i) -> new LastSync(rs.getTimestamp("finished_at").toInstant(), rs.getInt("rows_upserted")))
                .optional();
    }

    @Override
    public Optional<SyncAttempt> lastAttempt(SyncSource source) {
        return jdbc.sql("""
                        select coalesce(finished_at, started_at) as at, status, message
                        from sync_run
                        where source = :source
                        order by started_at desc limit 1
                        """)
                .param("source", source.name())
                .query((rs, i) -> new SyncAttempt(
                        rs.getTimestamp("at").toInstant(),
                        rs.getString("status"),
                        rs.getString("message")))
                .optional();
    }
}
