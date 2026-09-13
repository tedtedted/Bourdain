package com.tedredington.bourdain.civicdata.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.tedredington.bourdain.civicdata.SyncSource;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyncServiceTest {

    @Test
    void inspectionSyncQueriesWithWatermarkOverlapButPersistsOriginalMaxWatermark() {
        CivicDataSource source = mock(CivicDataSource.class);
        SyncRunRepository syncRuns = mock(SyncRunRepository.class);
        SyncService service = new SyncService(
                source,
                syncRuns,
                new SocrataProperties(
                        "https://data.example.org", null, 1000, Duration.ofSeconds(5), Duration.ofSeconds(30),
                        3, Duration.ZERO,
                        "insp-ds", "lic-ds", List.of("Retail Food Establishment")),
                new SyncProperties("0 30 6 * * *", "America/Chicago", true, Duration.ofMinutes(5)),
                mock(ApplicationEventPublisher.class),
                new TransactionTemplate(new NoOpTransactionManager()),
                Clock.fixed(Instant.parse("2026-09-06T11:30:00Z"), ZoneOffset.UTC));

        // Saving assigns the id the way the database would.
        when(syncRuns.save(any())).thenAnswer(invocation -> {
            SyncRun run = invocation.getArgument(0);
            ReflectionTestUtils.setField(run, "id", 42L);
            return run;
        });
        when(syncRuns.findLastWatermark(SyncSource.INSPECTIONS))
                .thenReturn(Optional.of("2026-09-05T12:00:00.000Z"));
        when(source.inspectionsPage("2026-09-05T11:55:00.000Z", null, 1000))
                .thenReturn(new CivicDataSource.InspectionPage(List.of(), null, null, 0));

        service.syncInspections();

        ArgumentCaptor<SyncRun> saved = ArgumentCaptor.forClass(SyncRun.class);
        verify(syncRuns, atLeastOnce()).save(saved.capture());
        SyncRun finished = saved.getValue();
        assertThat(finished.status()).isEqualTo(SyncRun.Status.SUCCEEDED);
        assertThat(finished.watermark()).isEqualTo("2026-09-05T12:00:00.000Z");
    }

    private static final class NoOpTransactionManager implements PlatformTransactionManager {

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
