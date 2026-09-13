package com.tedredington.bourdain.establishment.internal;

import java.time.Instant;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;

interface BusinessLicenseRepository extends Repository<BusinessLicense, String>, BusinessLicenseRepositoryCustom {

    /**
     * Marks every listed license not upserted since {@code cutoff}. Rows are
     * stamped with the database's {@code now()}, so the cutoff must come from
     * the same clock, not the JVM's; the two can disagree. Marked rather than
     * deleted: once the city drops a license, this mirror is the only record
     * it existed.
     */
    @Modifying
    @Query("""
            update business_license set delisted_at = now()
            where updated_at < :cutoff and delisted_at is null
            """)
    int delistNotUpsertedSince(Instant cutoff);
}
