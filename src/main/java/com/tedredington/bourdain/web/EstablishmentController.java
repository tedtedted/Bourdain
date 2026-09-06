package com.tedredington.bourdain.web;

import com.tedredington.bourdain.civicdata.SyncStatus;
import com.tedredington.bourdain.establishment.Establishments;
import com.tedredington.bourdain.inspection.Inspections;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
class EstablishmentController {

    private final Establishments establishments;
    private final Inspections inspections;
    private final SyncStatus syncStatus;

    EstablishmentController(Establishments establishments, Inspections inspections,
            SyncStatus syncStatus) {
        this.establishments = establishments;
        this.inspections = inspections;
        this.syncStatus = syncStatus;
    }

    @GetMapping("/establishments/{licenseNumber}")
    String establishment(@PathVariable long licenseNumber, Model model) {
        var view = establishments.byLicenseNumber(licenseNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown establishment"));
        model.addAttribute("page", EstablishmentPage.of(view, inspections.history(licenseNumber)));
        Provenance.addTo(model, syncStatus, establishments);
        return "establishment";
    }
}
