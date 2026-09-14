package com.tedredington.bourdain.establishment.internal;

import org.springframework.data.repository.Repository;

interface EstablishmentRevisionRepository extends Repository<EstablishmentRevision, Long> {

    EstablishmentRevision save(EstablishmentRevision revision);
}
