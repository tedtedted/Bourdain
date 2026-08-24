/**
 * Inspection history: results, types, and parsed violations. Read models are
 * exposed as records via {@link com.tedredington.bourdain.inspection.Inspections};
 * persistence stays internal (plain JDBC — this module is write-batch and
 * read-model shaped, with no entity graph to justify JPA).
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Inspections",
        allowedDependencies = "civicdata")
package com.tedredington.bourdain.inspection;
