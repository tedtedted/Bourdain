package com.tedredington.bourdain.web;

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

    EstablishmentController(Establishments establishments, Inspections inspections) {
        this.establishments = establishments;
        this.inspections = inspections;
    }

    @GetMapping("/establishments/{licenseNumber}")
    String establishment(@PathVariable long licenseNumber, Model model) {
        var view = establishments.byLicenseNumber(licenseNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown establishment"));
        model.addAttribute("e", view);
        model.addAttribute("history", inspections.history(licenseNumber));
        return "establishment";
    }
}
