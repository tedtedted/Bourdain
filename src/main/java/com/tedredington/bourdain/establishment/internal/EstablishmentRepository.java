package com.tedredington.bourdain.establishment.internal;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface EstablishmentRepository extends JpaRepository<Establishment, Long> {

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
    List<Establishment> search(@Param("query") String query, @Param("limit") int limit);
}
