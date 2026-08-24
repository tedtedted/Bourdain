package com.tedredington.bourdain.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.tedredington.bourdain.civicdata.SyncStatus;
import com.tedredington.bourdain.establishment.EstablishmentStatus;
import com.tedredington.bourdain.establishment.EstablishmentView;
import com.tedredington.bourdain.establishment.FacilityCategory;
import com.tedredington.bourdain.establishment.Establishments;
import com.tedredington.bourdain.establishment.Risk;
import com.tedredington.bourdain.inspection.InspectionResult;
import com.tedredington.bourdain.inspection.Inspections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({HomeController.class, SearchController.class, EstablishmentController.class})
class WebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Establishments establishments;
    @MockitoBean
    private Inspections inspections;
    @MockitoBean
    private SyncStatus syncStatus;

    private static EstablishmentView relocatedDuke() {
        return new EstablishmentView(18158, "THE DUKE OF PERTH", "THE DUKE OF PERTH",
                "2913 N CLARK ST", "CHICAGO", "60657", FacilityCategory.RESTAURANT, "Restaurant",
                Risk.HIGH, EstablishmentStatus.RELOCATED, InspectionResult.OUT_OF_BUSINESS,
                LocalDate.of(2024, 7, 16),
                new EstablishmentView.Relocation(2252464, "2827 N BROADWAY  1", LocalDate.of(2025, 2, 16)));
    }

    @Test
    void homeRendersRecentFailures() throws Exception {
        when(inspections.recentFailures(anyInt())).thenReturn(List.of(
                new Inspections.RecentFailure(102, 999, "HOT DOG HOUSE", LocalDate.of(2026, 8, 1),
                        "INSECTS & RODENTS", 2)));
        when(establishments.count()).thenReturn(41_000L);
        when(syncStatus.lastSuccessful(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("HOT DOG HOUSE")))
                .andExpect(content().string(containsString("No successful sync yet")));
    }

    @Test
    void searchReturnsResultsFragment() throws Exception {
        when(establishments.search(eq("duke"), anyInt())).thenReturn(List.of(relocatedDuke()));

        mockMvc.perform(get("/search").param("q", "duke"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("THE DUKE OF PERTH")))
                .andExpect(content().string(containsString("RELOCATED")));
    }

    @Test
    void establishmentPageShowsRelocationNotice() throws Exception {
        when(establishments.byLicenseNumber(18158)).thenReturn(Optional.of(relocatedDuke()));
        when(inspections.history(18158)).thenReturn(List.of());

        mockMvc.perform(get("/establishments/18158"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Moved, not closed")))
                .andExpect(content().string(containsString("2827 N BROADWAY")));
    }

    @Test
    void unknownEstablishmentIs404() throws Exception {
        when(establishments.byLicenseNumber(anyLong())).thenReturn(Optional.empty());

        mockMvc.perform(get("/establishments/1")).andExpect(status().isNotFound());
    }
}
