package com.tedredington.bourdain.civicdata.internal;

import java.util.List;
import java.util.Optional;

import com.tedredington.bourdain.civicdata.SyncSource;

import org.springframework.data.repository.Repository;

interface SyncRunRepository extends Repository<SyncRun, Long> {

    SyncRun save(SyncRun run);

    List<SyncRun> findByStatus(SyncRun.Status status);

    Optional<SyncRun> findFirstBySourceOrderByStartedAtDesc(SyncSource source);

    Optional<SyncRun> findFirstBySourceAndStatusOrderByStartedAtDesc(SyncSource source, SyncRun.Status status);

    Optional<SyncRun> findFirstBySourceAndStatusAndWatermarkNotNullOrderByStartedAtDesc(
            SyncSource source, SyncRun.Status status);

    /** Watermark of the most recent successful run, if any. */
    default Optional<String> findLastWatermark(SyncSource source) {
        return findFirstBySourceAndStatusAndWatermarkNotNullOrderByStartedAtDesc(source, SyncRun.Status.SUCCEEDED)
                .map(SyncRun::watermark);
    }
}
