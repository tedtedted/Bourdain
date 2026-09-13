package com.tedredington.bourdain.civicdata.internal;

import java.util.Optional;

import com.tedredington.bourdain.civicdata.SyncSource;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;

interface SyncRunRepository extends Repository<SyncRun, Long> {

    /**
     * Stamped by the database clock rather than the JVM's, because downstream
     * delisting compares it with rows stamped by {@code now()}.
     */
    @Query("""
            insert into sync_run (source, started_at, status)
            values (:source, now(), 'RUNNING')
            returning *
            """)
    SyncRun start(SyncSource source);

    /**
     * Marks leftover RUNNING rows as failed. Safe because only one sync runs at
     * a time in a single instance; a RUNNING row at sync start means a previous
     * process died mid-run.
     */
    @Modifying
    @Query("""
            update sync_run
            set finished_at = now(), status = 'FAILED', message = 'abandoned (process restart)'
            where status = 'RUNNING'
            """)
    int failAbandoned();

    @Modifying
    @Query("""
            update sync_run
            set finished_at = now(), status = 'SUCCEEDED',
                rows_upserted = :rowsUpserted, rows_skipped = :rowsSkipped, watermark = :watermark
            where id = :id
            """)
    void complete(long id, int rowsUpserted, int rowsSkipped, String watermark);

    @Modifying
    @Query("""
            update sync_run
            set finished_at = now(), status = 'FAILED', message = :message
            where id = :id
            """)
    void fail(long id, String message);

    Optional<SyncRun> findFirstBySourceOrderByStartedAtDesc(SyncSource source);

    Optional<SyncRun> findFirstBySourceAndStatusOrderByStartedAtDesc(SyncSource source, SyncRun.Status status);

    Optional<SyncRun> findFirstBySourceAndStatusAndWatermarkIsNotNullOrderByStartedAtDesc(
            SyncSource source, SyncRun.Status status);

    /** Watermark of the most recent successful run, if any. */
    default Optional<String> findLastWatermark(SyncSource source) {
        return findFirstBySourceAndStatusAndWatermarkIsNotNullOrderByStartedAtDesc(source, SyncRun.Status.SUCCEEDED)
                .map(SyncRun::watermark);
    }
}
