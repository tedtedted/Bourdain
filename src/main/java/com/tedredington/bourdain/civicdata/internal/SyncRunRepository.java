package com.tedredington.bourdain.civicdata.internal;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;

import com.tedredington.bourdain.civicdata.SyncSource;
import com.tedredington.bourdain.civicdata.SyncStatus.LastSync;
import com.tedredington.bourdain.civicdata.SyncStatus.SyncAttempt;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** {@code sync_run} bookkeeping: run lifecycle plus watermark lookup. */
@Repository
class SyncRunRepository {

    /** A run's id and its start time by the database clock. */
    record StartedRun(long id, Instant startedAt) {
    }

    private final JdbcClient jdbc;

    SyncRunRepository(JdbcClient jdbc) {
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

    StartedRun start(SyncSource source) {
        return jdbc.sql("""
                        insert into sync_run (source, started_at, status)
                        values (:source, now(), 'RUNNING') returning id, started_at
                        """)
                .param("source", source.name())
                .query((rs, i) -> new StartedRun(
                        rs.getLong("id"),
                        rs.getObject("started_at", OffsetDateTime.class).toInstant()))
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
    Optional<String> findLastWatermark(SyncSource source) {
        return jdbc.sql("""
                        select watermark from sync_run
                        where source = :source and status = 'SUCCEEDED' and watermark is not null
                        order by started_at desc limit 1
                        """)
                .param("source", source.name())
                .query(String.class)
                .optional();
    }

    Optional<LastSync> findLastSuccessful(SyncSource source) {
        return jdbc.sql("""
                        select finished_at, rows_upserted from sync_run
                        where source = :source and status = 'SUCCEEDED'
                        order by started_at desc limit 1
                        """)
                .param("source", source.name())
                .query((rs, i) -> new LastSync(
                        rs.getObject("finished_at", OffsetDateTime.class).toInstant(),
                        rs.getInt("rows_upserted")))
                .optional();
    }

    Optional<SyncAttempt> findLastAttempt(SyncSource source) {
        return jdbc.sql("""
                        select coalesce(finished_at, started_at) as at, status, message
                        from sync_run
                        where source = :source
                        order by started_at desc limit 1
                        """)
                .param("source", source.name())
                .query((rs, i) -> new SyncAttempt(
                        rs.getObject("at", OffsetDateTime.class).toInstant(),
                        rs.getString("status"),
                        rs.getString("message")))
                .optional();
    }
}
