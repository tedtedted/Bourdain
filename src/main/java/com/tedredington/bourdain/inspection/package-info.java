/**
 * Inspection history: results, types, and parsed violations. Read models are
 * exposed as records via {@link com.tedredington.bourdain.inspection.Inspections};
 * persistence stays internal, as JPA entities behind module-private repositories.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Inspections",
        allowedDependencies = "civicdata")
package com.tedredington.bourdain.inspection;
