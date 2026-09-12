package com.tedredington.bourdain.establishment.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.tedredington.bourdain.establishment.EstablishmentView;
import com.tedredington.bourdain.establishment.Establishments;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class EstablishmentsImpl implements Establishments {

    /**
     * Rows fetched per result slot, so collapsing a business's extra licenses
     * still fills the page.
     */
    private static final int OVERFETCH = 4;

    private final EstablishmentRepository repository;

    EstablishmentsImpl(EstablishmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<EstablishmentView> byLicenseNumber(long licenseNumber) {
        return repository.findById(licenseNumber).map(Establishment::toView);
    }

    @Override
    public List<EstablishmentView> search(String query, int limit) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        int fetch = limit * OVERFETCH;
        List<Establishment> matches = trimmed.matches("\\d{5}")
                ? repository.findByZipOrderByLastInspectedOnDesc(trimmed, Limit.of(fetch))
                : repository.search(NameNormalizer.normalize(trimmed), fetch);
        return onePerPlace(matches.stream().map(Establishment::toView).toList(), limit);
    }

    /**
     * One business often holds several licenses at one address (a food license
     * and a caterer's liquor license), and each is its own establishment row.
     * Keeps the first row per name and location; both queries order the same
     * name by most recent inspection, so that's the license shown.
     */
    static List<EstablishmentView> onePerPlace(List<EstablishmentView> ordered, int limit) {
        Set<String> seen = new HashSet<>();
        return ordered.stream()
                .filter(e -> seen.add(NameNormalizer.normalize(e.name()) + "|" + Addresses.locationKey(e.address())))
                .limit(limit)
                .toList();
    }

    @Override
    public long count() {
        return repository.count();
    }
}
