package com.tedredington.bourdain.inspection.internal;

import java.util.List;

import com.tedredington.bourdain.inspection.InspectionResult;
import com.tedredington.bourdain.inspection.Inspections;

import org.springframework.data.domain.Limit;
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
        return repository.findByResult(InspectionResult.FAIL, Limit.of(limit));
    }

    @Override
    public List<InspectionDetail> history(long licenseNumber) {
        return repository.findHistory(licenseNumber).stream()
                .map(Inspection::toDetail)
                .toList();
    }
}
