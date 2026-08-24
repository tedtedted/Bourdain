package com.tedredington.bourdain.establishment.internal;

import java.util.List;
import java.util.Optional;

import com.tedredington.bourdain.establishment.EstablishmentView;
import com.tedredington.bourdain.establishment.Establishments;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class EstablishmentsImpl implements Establishments {

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
        List<Establishment> matches = trimmed.matches("\\d{5}")
                ? repository.findByZipOrderByLastInspectedOnDesc(trimmed, Limit.of(limit))
                : repository.search(NameNormalizer.normalize(trimmed), limit);
        return matches.stream().map(Establishment::toView).toList();
    }

    @Override
    public long count() {
        return repository.count();
    }
}
