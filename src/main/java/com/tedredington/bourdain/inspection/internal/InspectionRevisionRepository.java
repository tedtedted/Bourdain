package com.tedredington.bourdain.inspection.internal;

import org.springframework.data.repository.Repository;

interface InspectionRevisionRepository extends Repository<InspectionRevision, Long> {

    InspectionRevision save(InspectionRevision revision);
}
