package com.tedredington.bourdain.civicdata.internal;

import java.time.Instant;

import com.tedredington.bourdain.civicdata.SyncSource;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** One sync attempt for one source; the latest successful run carries the next watermark. */
@Table("sync_run")
record SyncRun(
        @Id long id,
        SyncSource source,
        Instant startedAt,
        Instant finishedAt,
        Status status,
        int rowsUpserted,
        int rowsSkipped,
        String watermark,
        String message) {

    enum Status {
        RUNNING, SUCCEEDED, FAILED
    }
}
