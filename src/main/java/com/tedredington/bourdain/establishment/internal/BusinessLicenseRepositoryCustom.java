package com.tedredington.bourdain.establishment.internal;

import java.util.List;

/** Batch upsert, which a {@code @Modifying} query would run one round trip per row. */
interface BusinessLicenseRepositoryCustom {

    /** Upserting a license also lists it again if it had been delisted. */
    void upsertAll(List<BusinessLicense> licenses);
}
