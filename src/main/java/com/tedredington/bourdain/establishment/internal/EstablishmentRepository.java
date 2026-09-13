package com.tedredington.bourdain.establishment.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

interface EstablishmentRepository extends Repository<Establishment, Long> {

    Optional<Establishment> findById(Long licenseNumber);

    List<Establishment> findAllById(Iterable<Long> licenseNumbers);

    long count();

    Establishment save(Establishment establishment);

    List<Establishment> findByZipOrderByLastInspectedOnDesc(String zip, Limit limit);

    /**
     * Trigram similarity over the normalized name (backed by the GIN index)
     * with a plain substring fallback for addresses.
     */
    @Query(value = """
            select * from establishment e
            where e.normalized_name % :query
               or e.normalized_name ilike '%' || :query || '%'
               or e.address ilike '%' || :query || '%'
            order by similarity(e.normalized_name, :query) desc, e.last_inspected_on desc nulls last
            limit :limit
            """, nativeQuery = true)
    List<Establishment> search(String query, int limit);

    /** Sets every establishment to OPEN or CLOSED from its latest result and clears relocations. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Establishment e
            set e.status = case
                    when e.latestResult = com.tedredington.bourdain.inspection.InspectionResult.OUT_OF_BUSINESS
                    then com.tedredington.bourdain.establishment.EstablishmentStatus.CLOSED
                    else com.tedredington.bourdain.establishment.EstablishmentStatus.OPEN end,
                e.relocatedToLicenseNumber = null, e.relocatedToAddress = null, e.relocatedSince = null
            """)
    void resetStatuses();

    @Query("""
            select new com.tedredington.bourdain.establishment.internal.ClosedNamesake(
                   e.licenseNumber, e.address, e.lastInspectedOn,
                   l.licenseNumber, l.address, l.licenseStartDate, l.expirationDate)
            from Establishment e
            join BusinessLicense l on l.normalizedName = e.normalizedName
            where e.status = com.tedredington.bourdain.establishment.EstablishmentStatus.CLOSED
              and l.licenseNumber <> e.licenseNumber
              and l.delistedAt is null
            """)
    List<ClosedNamesake> findClosedNamesakes();

    /** Each CLOSED establishment, with the listed licenses of other license numbers sharing its name. */
    default Map<RelocationMatcher.Closed, List<RelocationMatcher.Candidate>> findClosedWithNamesakes() {
        return findClosedNamesakes().stream().collect(groupingBy(
                ClosedNamesake::closed, LinkedHashMap::new, mapping(ClosedNamesake::candidate, toList())));
    }

    /**
     * Appends a timeline row for every establishment whose status or
     * relocation target changed since its last one. Set-based SQL: comparing
     * each establishment with its latest change needs a lateral join.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into establishment_status_change (license_number, status, relocated_to_license_number,
                                                     relocated_to_address, relocated_since)
            select e.license_number, e.status, e.relocated_to_license_number,
                   e.relocated_to_address, e.relocated_since
            from establishment e
            left join lateral (
                select c.status, c.relocated_to_license_number
                from establishment_status_change c
                where c.license_number = e.license_number
                order by c.id desc
                limit 1
            ) last on true
            where (e.status, e.relocated_to_license_number)
                  is distinct from (last.status, last.relocated_to_license_number)
            """, nativeQuery = true)
    int recordStatusChanges();
}
