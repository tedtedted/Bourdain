package com.tedredington.bourdain.establishment.internal;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

interface BusinessLicenseRepository extends Repository<BusinessLicense, String> {

    List<BusinessLicense> findAllById(Iterable<String> recordIds);

    BusinessLicense save(BusinessLicense license);

    /**
     * Marks every listed license that the given run, or a later one, didn't
     * list. Run ids only grow, so replaying an older run's completion can't
     * delist what a newer run saw. Marked rather than deleted: once the city
     * drops a license, this mirror is the only record it existed.
     */
    @Modifying
    @Query("""
            update BusinessLicense l set l.delistedAt = :now
            where l.delistedAt is null and (l.lastSeenSyncRunId is null or l.lastSeenSyncRunId < :syncRunId)
            """)
    int delistNotListedBy(long syncRunId, Instant now);
}
