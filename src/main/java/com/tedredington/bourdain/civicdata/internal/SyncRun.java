package com.tedredington.bourdain.civicdata.internal;

import java.time.Instant;

import com.tedredington.bourdain.civicdata.SyncSource;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One sync attempt for one source; the latest successful run carries the next watermark. */
@Entity
@Table(name = "sync_run")
class SyncRun {

    enum Status {
        RUNNING, SUCCEEDED, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private SyncSource source;

    private Instant startedAt;
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    private Status status;

    private int rowsUpserted;
    private int rowsSkipped;
    private String watermark;
    private String message;

    protected SyncRun() {
    }

    static SyncRun start(SyncSource source, Instant now) {
        SyncRun run = new SyncRun();
        run.source = source;
        run.startedAt = now;
        run.status = Status.RUNNING;
        return run;
    }

    void succeed(int rowsUpserted, int rowsSkipped, String watermark, Instant now) {
        this.rowsUpserted = rowsUpserted;
        this.rowsSkipped = rowsSkipped;
        this.watermark = watermark;
        this.status = Status.SUCCEEDED;
        this.finishedAt = now;
    }

    void fail(String message, Instant now) {
        this.message = message;
        this.status = Status.FAILED;
        this.finishedAt = now;
    }

    long id() {
        return id;
    }

    Instant startedAt() {
        return startedAt;
    }

    Instant finishedAt() {
        return finishedAt;
    }

    Status status() {
        return status;
    }

    int rowsUpserted() {
        return rowsUpserted;
    }

    String watermark() {
        return watermark;
    }

    String message() {
        return message;
    }
}
