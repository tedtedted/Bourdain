package com.tedredington.bourdain.web;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.tedredington.bourdain.civicdata.SyncSource;
import com.tedredington.bourdain.civicdata.SyncStatus;
import com.tedredington.bourdain.establishment.Establishments;
import com.tedredington.bourdain.inspection.Inspections;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class HomeController {

    private static final int RECENT_FAILURES = 12;
    private static final DateTimeFormatter SYNC_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("America/Chicago"));

    private final Inspections inspections;
    private final Establishments establishments;
    private final SyncStatus syncStatus;

    HomeController(Inspections inspections, Establishments establishments, SyncStatus syncStatus) {
        this.inspections = inspections;
        this.establishments = establishments;
        this.syncStatus = syncStatus;
    }

    @GetMapping("/")
    String home(Model model) {
        model.addAttribute("recentFailures", inspections.recentFailures(RECENT_FAILURES));
        model.addAttribute("establishmentCount", establishments.count());
        model.addAttribute("lastSyncText", syncStatus.lastSuccessful(SyncSource.INSPECTIONS)
                .map(sync -> SYNC_TIME.format(sync.finishedAt()))
                .orElse(null));
        model.addAttribute("syncFailureText", syncStatus.lastAttempt(SyncSource.INSPECTIONS)
                .filter(SyncStatus.SyncAttempt::failed)
                .map(attempt -> SYNC_TIME.format(attempt.finishedAt()))
                .orElse(null));
        return "index";
    }
}
