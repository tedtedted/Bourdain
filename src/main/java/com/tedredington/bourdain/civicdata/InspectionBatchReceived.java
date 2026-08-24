package com.tedredington.bourdain.civicdata;

import java.util.List;

/**
 * Published for every fetched page, inside the page's transaction. Domain
 * modules upsert from these synchronously so a page either lands completely or
 * not at all.
 */
public record InspectionBatchReceived(List<InspectionRecord> records) {
}
