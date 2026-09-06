package com.tedredington.bourdain.web;

import com.tedredington.bourdain.civicdata.SyncStatus;
import com.tedredington.bourdain.establishment.Establishments;
import com.tedredington.bourdain.inspection.Inspections;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class HomeController {

    private static final int RECENT_FAILURES = 12;

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
        Provenance.addTo(model, syncStatus, establishments);
        return "index";
    }
}
