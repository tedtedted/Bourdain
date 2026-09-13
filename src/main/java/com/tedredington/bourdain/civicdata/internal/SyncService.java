package com.tedredington.bourdain.civicdata.internal;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tedredington.bourdain.civicdata.CivicDataSync;
import com.tedredington.bourdain.civicdata.CivicDataSyncCompleted;
import com.tedredington.bourdain.civicdata.InspectionBatchReceived;
import com.tedredington.bourdain.civicdata.LicenseBatchReceived;
import com.tedredington.bourdain.civicdata.SyncSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Pulls both datasets page by page. Each page is committed in its own
 * transaction (publish → domain listeners upsert), so an interrupted run leaves
 * complete pages behind and the next run resumes from the stored watermark.
 * Upserts are idempotent, making re-reading overlap harmless.
 */
@Service
class SyncService implements CivicDataSync {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);
    private static final DateTimeFormatter SOCRATA_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private final CivicDataSource source;
    private final SyncRunRepository syncRuns;
    private final SocrataProperties socrataProperties;
    private final SyncProperties syncProperties;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transaction;
    private final AtomicBoolean running = new AtomicBoolean(false);

    SyncService(CivicDataSource source, SyncRunRepository syncRuns, SocrataProperties socrataProperties,
                SyncProperties syncProperties, ApplicationEventPublisher events, TransactionTemplate transaction) {
        this.source = source;
        this.syncRuns = syncRuns;
        this.socrataProperties = socrataProperties;
        this.syncProperties = syncProperties;
        this.events = events;
        this.transaction = transaction;
    }

    /** Runs both sources; skips silently if a sync is already in flight. */
    @Override
    public void syncAll() {
        if (!running.compareAndSet(false, true)) {
            log.info("Sync already running; skipping this trigger");
            return;
        }
        try {
            int abandoned = syncRuns.failAbandoned();
            if (abandoned > 0) {
                log.warn("Marked {} abandoned sync runs as failed", abandoned);
            }
            syncInspections();
            syncLicenses();
        } finally {
            running.set(false);
        }
    }

    void syncInspections() {
        var run = syncRuns.start(SyncSource.INSPECTIONS);
        String watermark = syncRuns.findLastWatermark(SyncSource.INSPECTIONS).orElse(null);
        String queryWatermark = overlappedWatermark(watermark);
        try {
            String lastRowId = null;
            String maxUpdatedAt = watermark;
            int upserted = 0;
            int skipped = 0;
            while (true) {
                var page = source.inspectionsPage(queryWatermark, lastRowId, socrataProperties.pageSize());
                if (!page.records().isEmpty()) {
                    transaction.executeWithoutResult(tx ->
                            events.publishEvent(new InspectionBatchReceived(page.records())));
                }
                upserted += page.records().size();
                skipped += page.skipped();
                if (page.maxUpdatedAt() != null
                        && (maxUpdatedAt == null || page.maxUpdatedAt().compareTo(maxUpdatedAt) > 0)) {
                    maxUpdatedAt = page.maxUpdatedAt();
                }
                if (page.lastRowId() == null) {
                    break; // empty page: scan complete
                }
                lastRowId = page.lastRowId();
            }
            finish(run, SyncSource.INSPECTIONS, upserted, skipped, maxUpdatedAt);
        } catch (RuntimeException e) {
            log.error("Inspection sync failed", e);
            syncRuns.fail(run.id(), e.getMessage());
        }
    }

    private String overlappedWatermark(String watermark) {
        Duration overlap = syncProperties.watermarkOverlap();
        if (watermark == null || overlap.isZero() || overlap.isNegative()) {
            return watermark;
        }
        try {
            return LocalDateTime.parse(watermark)
                    .minus(overlap)
                    .format(SOCRATA_TIMESTAMP);
        } catch (DateTimeParseException e) {
            log.warn("Could not apply inspection watermark overlap to {}", watermark, e);
            return watermark;
        }
    }

    void syncLicenses() {
        var run = syncRuns.start(SyncSource.LICENSES);
        try {
            String lastRowId = null;
            int upserted = 0;
            int skipped = 0;
            while (true) {
                var page = source.licensesPage(lastRowId, socrataProperties.pageSize());
                if (!page.records().isEmpty()) {
                    transaction.executeWithoutResult(tx ->
                            events.publishEvent(new LicenseBatchReceived(page.records())));
                }
                upserted += page.records().size();
                skipped += page.skipped();
                if (page.lastRowId() == null) {
                    break;
                }
                lastRowId = page.lastRowId();
            }
            finish(run, SyncSource.LICENSES, upserted, skipped, null);
        } catch (RuntimeException e) {
            log.error("License sync failed", e);
            syncRuns.fail(run.id(), e.getMessage());
        }
    }

    private void finish(SyncRun run, SyncSource syncSource, int upserted, int skipped,
                        String watermark) {
        // Completion is recorded and published in one transaction: the Modulith
        // JDBC registry stores the event alongside the run row, then delivers it
        // to module listeners after commit.
        transaction.executeWithoutResult(tx -> {
            syncRuns.complete(run.id(), upserted, skipped, watermark);
            events.publishEvent(new CivicDataSyncCompleted(syncSource, run.id(), run.startedAt(), upserted));
        });
        log.info("{} sync finished: {} rows upserted, {} skipped", syncSource, upserted, skipped);
    }
}
