/**
 * Establishment identity and lifecycle: who is at which license/address, their
 * derived {@code OPEN}/{@code CLOSED}/{@code RELOCATED} status, and the active
 * business-license mirror that powers relocation matching (the "moved, not
 * closed" problem — see {@code RelocationMatcher}).
 *
 * <p>Depends on {@code civicdata} for the ingest event stream and on
 * {@code inspection} only for the {@code InspectionResult} enum.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Establishments",
        allowedDependencies = {"civicdata", "inspection"})
package com.tedredington.bourdain.establishment;
