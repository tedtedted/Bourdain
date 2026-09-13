package com.tedredington.bourdain.establishment.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only by design: writes go through the upsert and derivation SQL in the custom fragment. */
interface EstablishmentRepository extends Repository<Establishment, Long>, EstablishmentRepositoryCustom {

    Optional<Establishment> findById(Long licenseNumber);

    long count();

    List<Establishment> findByZipOrderByLastInspectedOnDesc(String zip, Limit limit);

    /**
     * Trigram similarity over the normalized name (backed by the GIN index)
     * with a plain substring fallback for addresses.
     */
    @Query("""
            select * from establishment e
            where e.normalized_name % :query
               or e.normalized_name ilike '%' || :query || '%'
               or e.address ilike '%' || :query || '%'
            order by similarity(e.normalized_name, :query) desc, e.last_inspected_on desc nulls last
            limit :limit
            """)
    List<Establishment> search(@Param("query") String query, @Param("limit") int limit);
}
