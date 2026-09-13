package com.tedredington.bourdain.inspection.internal;

import java.util.List;

import com.tedredington.bourdain.inspection.Inspections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class InspectionsImpl implements Inspections {

    private final InspectionRepository repository;

    InspectionsImpl(InspectionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RecentFailure> recentFailures(int limit) {
        return repository.findRecentFailures(limit);
    }

    /** One transaction, so a sync committing mid-read can't pair inspections with the wrong violations. */
    @Override
    public List<InspectionDetail> history(long licenseNumber) {
        return repository.findHistory(licenseNumber);
    }
}
