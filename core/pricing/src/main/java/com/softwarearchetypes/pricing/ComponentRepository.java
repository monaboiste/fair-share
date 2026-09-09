package com.softwarearchetypes.pricing;

import java.util.Collection;
import java.util.Optional;

interface ComponentRepository {
    void save(Component component);

    Optional<Component> findByName(String name);

    Optional<Component> findById(ComponentId id);

    Collection<Component> findAll();

    Collection<Component> findByNames(Collection<String> names);
}
