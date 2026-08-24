/**
 * Thymeleaf + htmx UI. Consumes the read-side APIs of the other modules;
 * nothing depends on this module.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Web UI",
        allowedDependencies = {"civicdata", "establishment", "inspection"})
package com.tedredington.bourdain.web;
