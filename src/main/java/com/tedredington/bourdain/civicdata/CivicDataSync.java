package com.tedredington.bourdain.civicdata;

/** Command-side API for starting a civic data sync. */
public interface CivicDataSync {

    /** Runs all configured civic-data sources; skips if a sync is already in flight. */
    void syncAll();
}
