package com.softwarearchetypes.pricing;

import java.util.Collection;
import java.util.Optional;

interface CalculatorRepository {
    void save(Calculator calculator);

    Optional<Calculator> findByName(String name);

    Optional<Calculator> findById(CalculatorId id);

    Collection<Calculator> findAll();

    Collection<Calculator> findByIds(Collection<CalculatorId> ids);
}
