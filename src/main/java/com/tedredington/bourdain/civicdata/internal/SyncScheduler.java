package com.tedredington.bourdain.civicdata.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily scheduled sync plus an async catch-up on boot (mirrors vmb's
 * startup-sync habit): a restarted instance is current within minutes instead
 * of waiting for the next cron window.
 */
@Component
class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);

    private final SyncService syncService;
    private final SyncProperties properties;

    SyncScheduler(SyncService syncService, SyncProperties properties) {
        this.syncService = syncService;
        this.properties = properties;
    }

    @Scheduled(cron = "${bourdain.sync.cron}", zone = "${bourdain.sync.zone}")
    void scheduledSync() {
        syncService.syncAll();
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    void startupSync() {
        if (!properties.onStartup()) {
            return;
        }
        log.info("Startup catch-up sync");
        syncService.syncAll();
    }
}
