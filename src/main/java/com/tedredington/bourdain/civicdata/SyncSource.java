package com.tedredington.bourdain.civicdata;

/** The two upstream datasets Bourdain mirrors. */
public enum SyncSource {

    /** Food Inspections (4ijn-s7e5) — incremental by Socrata {@code :updated_at}. */
    INSPECTIONS,

    /** Business Licenses, Current Active (uupf-x98q) — full refresh each run. */
    LICENSES
}
