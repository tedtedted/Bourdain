package com.tedredington.bourdain.establishment.internal;

import java.time.Clock;

import com.tedredington.bourdain.civicdata.CivicDataSyncCompleted;
import com.tedredington.bourdain.civicdata.SyncSource;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class RelocationServiceTest {

    @Test
    void ignoresInspectionSyncCompletionUntilLicensesAreRefreshed() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        RelocationService service = new RelocationService(jdbc, Clock.systemUTC());

        service.on(new CivicDataSyncCompleted(SyncSource.INSPECTIONS, 1L, 10));

        verifyNoInteractions(jdbc);
    }
}
