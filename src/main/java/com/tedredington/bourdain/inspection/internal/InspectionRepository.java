package com.tedredington.bourdain.inspection.internal;

import java.util.List;

import com.tedredington.bourdain.inspection.InspectionResult;
import com.tedredington.bourdain.inspection.Inspections.RecentFailure;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

interface InspectionRepository extends Repository<Inspection, Long> {

    List<Inspection> findAllById(Iterable<Long> ids);

    Inspection save(Inspection inspection);

    /** Newest first, violations in the order the inspector listed them, all in one query. */
    @Query("""
            select i from Inspection i
            left join fetch i.violations v
            where i.licenseNumber = :licenseNumber
            order by i.inspectedOn desc, i.id desc, v.ordinal
            """)
    List<Inspection> findHistory(long licenseNumber);

    /** Ordinal 0 is the first violation the inspector listed, which the feed uses as a headline. */
    @Query("""
            select new com.tedredington.bourdain.inspection.Inspections$RecentFailure(
                   i.id, i.licenseNumber, i.dbaName, i.inspectedOn,
                   (select v.description from Violation v where v.inspection = i and v.ordinal = 0),
                   cast((select count(v) from Violation v where v.inspection = i) as Integer))
            from Inspection i
            where i.result = :result
            order by i.inspectedOn desc, i.id desc
            """)
    List<RecentFailure> findByResult(InspectionResult result, Limit limit);
}
