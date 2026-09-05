package com.tedredington.bourdain.civicdata.internal;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.tedredington.bourdain.civicdata.SyncSource;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyncServiceTest {

    @Test
    void inspectionSyncQueriesWithWatermarkOverlapButPersistsOriginalMaxWatermark() {
        CivicDataSource source = mock(CivicDataSource.class);
        SyncRuns syncRuns = mock(SyncRuns.class);
        SyncService service = new SyncService(
                source,
                syncRuns,
                new SocrataProperties(
                        "https://data.example.org", null, 1000, Duration.ofSeconds(5), Duration.ofSeconds(30),
                        "insp-ds", "lic-ds", List.of("Retail Food Establishment")),
                new SyncProperties("0 30 6 * * *", "America/Chicago", true, Duration.ofMinutes(5)),
                mock(ApplicationEventPublisher.class),
                new TransactionTemplate(new NoOpTransactionManager()),
                mock(JdbcClient.class));

        when(syncRuns.start(SyncSource.INSPECTIONS)).thenReturn(42L);
        when(syncRuns.lastWatermark(SyncSource.INSPECTIONS))
                .thenReturn(Optional.of("2026-09-05T12:00:00.000"));
        when(source.inspectionsPage("2026-09-05T11:55:00.000", null, 1000))
                .thenReturn(new CivicDataSource.InspectionPage(List.of(), null, null, 0));

        service.syncInspections();

        verify(syncRuns).complete(42L, 0, 0, "2026-09-05T12:00:00.000");
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
