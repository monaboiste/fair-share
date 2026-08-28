package com.softwarearchetypes.product;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Persist product types. */
public interface ProductTypeRepository {
    void save(ProductType productType);

    Optional<ProductType> findById(ProductIdentifier id);

    Optional<ProductType> findByIdValue(String idValue);

    Set<ProductType> findAll();

    Set<ProductType> findByTrackingStrategy(ProductTrackingStrategy strategy);

    void remove(ProductIdentifier id);

    static ProductTypeRepository inMemory() {
        return new InMemoryProductTypeRepository();
    }
}

/** Stores product types in memory. */
class InMemoryProductTypeRepository implements ProductTypeRepository {

    private final Map<ProductIdentifier, ProductType> storage = new HashMap<>();

    @Override
    public void save(ProductType productType) {
        storage.put(productType.id(), productType);
    }

    @Override
    public Optional<ProductType> findById(ProductIdentifier id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<ProductType> findByIdValue(String idValue) {
        return storage.values().stream()
                .filter(pt -> pt.id().toString().equals(idValue))
                .findFirst();
    }

    @Override
    public Set<ProductType> findAll() {
        return new HashSet<>(storage.values());
    }

    @Override
    public Set<ProductType> findByTrackingStrategy(ProductTrackingStrategy strategy) {
        return storage.values().stream()
                .filter(pt -> pt.trackingStrategy().equals(strategy))
                .collect(Collectors.toSet());
    }

    @Override
    public void remove(ProductIdentifier id) {
        storage.remove(id);
    }
}
