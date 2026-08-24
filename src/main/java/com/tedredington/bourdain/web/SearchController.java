package com.tedredington.bourdain.web;

import com.tedredington.bourdain.establishment.Establishments;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** htmx endpoint: returns just the results fragment for the search-as-you-type box. */
@Controller
class SearchController {

    private static final int MAX_RESULTS = 25;

    private final Establishments establishments;

    SearchController(Establishments establishments) {
        this.establishments = establishments;
    }

    @GetMapping("/search")
    String search(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("query", q);
        model.addAttribute("results", establishments.search(q, MAX_RESULTS));
        return "fragments/search-results :: results";
    }
}
