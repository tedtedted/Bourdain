package com.tedredington.bourdain.civicdata.internal;

import java.util.Optional;

import com.tedredington.bourdain.civicdata.SyncSource;
import com.tedredington.bourdain.civicdata.SyncStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class SyncStatusImpl implements SyncStatus {

    private final SyncRunRepository syncRuns;

    SyncStatusImpl(SyncRunRepository syncRuns) {
        this.syncRuns = syncRuns;
    }

    @Override
    public Optional<LastSync> lastSuccessful(SyncSource source) {
        return syncRuns.findFirstBySourceAndStatusOrderByStartedAtDesc(source, SyncRun.Status.SUCCEEDED)
                .map(run -> new LastSync(run.finishedAt(), run.rowsUpserted()));
    }

    @Override
    public Optional<SyncAttempt> lastAttempt(SyncSource source) {
        return syncRuns.findFirstBySourceOrderByStartedAtDesc(source)
                .map(run -> new SyncAttempt(
                        run.finishedAt() != null ? run.finishedAt() : run.startedAt(),
                        run.status().name(),
                        run.message()));
    }
}
