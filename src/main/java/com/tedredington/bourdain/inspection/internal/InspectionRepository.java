package com.tedredington.bourdain.inspection.internal;

import java.util.List;

import com.tedredington.bourdain.inspection.Inspections.RecentFailure;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;

interface InspectionRepository extends Repository<Inspection, Long>, InspectionRepositoryCustom {

    @Query("""
            select i.id as inspection_id, i.license_number, i.dba_name, i.inspected_on,
                   (select v.description from violation v
                    where v.inspection_id = i.id order by v.ordinal limit 1) as headline,
                   (select count(*) from violation v where v.inspection_id = i.id) as violation_count
            from inspection i
            where i.result = 'FAIL'
            order by i.inspected_on desc, i.id desc
            limit :limit
            """)
    List<RecentFailure> findRecentFailures(int limit);
}
