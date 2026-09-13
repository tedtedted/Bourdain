/**
 * Inspection history: results, types, and parsed violations. Read models are
 * exposed as records via {@link com.tedredington.bourdain.inspection.Inspections};
 * persistence stays internal: a Spring Data JDBC aggregate for reads, and batch
 * upsert SQL for writes.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Inspections",
        allowedDependencies = "civicdata")
package com.tedredington.bourdain.inspection;
