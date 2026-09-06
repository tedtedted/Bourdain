package com.tedredington.bourdain.web;

import com.tedredington.bourdain.civicdata.SyncStatus;
import com.tedredington.bourdain.establishment.Establishments;
import com.tedredington.bourdain.inspection.Inspections;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
class HomeController {

    private static final int RECENT_FAILURES = 12;
    private static final int MAX_RESULTS = 25;

    private final Inspections inspections;
    private final Establishments establishments;
    private final SyncStatus syncStatus;

    HomeController(Inspections inspections, Establishments establishments, SyncStatus syncStatus) {
        this.inspections = inspections;
        this.establishments = establishments;
        this.syncStatus = syncStatus;
    }

    /**
     * {@code q} is the no-JavaScript path: htmx swaps the results fragment in
     * place, but a plain form submit lands here, so the same results are
     * rendered server-side. It also makes a search shareable as a URL.
     */
    @GetMapping("/")
    String home(@RequestParam(required = false) String q, Model model) {
        if (q != null) {
            model.addAttribute("query", q);
            model.addAttribute("results", establishments.search(q, MAX_RESULTS));
        }
        model.addAttribute("recentFailures", inspections.recentFailures(RECENT_FAILURES));
        Provenance.addTo(model, syncStatus, establishments);
        return "index";
    }
}
