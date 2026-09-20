package com.github.monaboiste.fairshare.product;

import java.util.Optional;

/** A concrete instance of a {@link ProductType} or {@link PackageType}. */
interface Instance {

    InstanceId id();

    Product product();

    Optional<SerialNumber> serialNumber();

    Optional<BatchId> batchId();

    default InstanceBuilder builder(InstanceId id) {
        return new InstanceBuilder(id);
    }
}
