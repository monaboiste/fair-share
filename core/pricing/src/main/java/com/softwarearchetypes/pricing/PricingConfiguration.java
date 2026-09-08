package com.softwarearchetypes.pricing;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PricingConfiguration {

    private final PricingFacade facade;

    PricingConfiguration(PricingFacade facade) {
        this.facade = facade;
    }

    public static PricingConfiguration inMemory(Clock clock) {
        InMemoryCalculatorRepository repository = new InMemoryCalculatorRepository();
        PricingFacade facade = new PricingFacade(repository, new InMemoryComponentRepository(), clock);
        facade.addCalculator(
                "simple-fixed-20",
                CalculatorType.SIMPLE_FIXED,
                new Parameters(Map.of("amount", Money.of(BigDecimal.valueOf(20), "PLN"))));
        facade.addCalculator(
                "simple-interest-6",
                CalculatorType.SIMPLE_INTEREST,
                new Parameters(Map.of("annualRate", BigDecimal.valueOf(6))));
        return new PricingConfiguration(facade);
    }

    public PricingFacade pricingFacade() {
        return facade;
    }
}

interface CalculatorRepository {
    void save(Calculator calculator);

    Optional<Calculator> findByName(String name);

    Optional<Calculator> findById(CalculatorId id);

    Collection<Calculator> findAll();

    Collection<Calculator> findByIds(Collection<CalculatorId> ids);
}

class InMemoryCalculatorRepository implements CalculatorRepository {

    private final Set<Calculator> calculators = new HashSet<>();

    @Override
    public void save(Calculator calculator) {
        calculators.add(calculator);
    }

    @Override
    public Optional<Calculator> findByName(String name) {
        return calculators.stream().filter(c -> c.name().equals(name)).findFirst();
    }

    @Override
    public Optional<Calculator> findById(CalculatorId id) {
        return calculators.stream().filter(c -> c.getId().equals(id)).findFirst();
    }

    @Override
    public Collection<Calculator> findAll() {
        return new HashSet<>(calculators);
    }

    @Override
    public Collection<Calculator> findByIds(Collection<CalculatorId> ids) {
        Set<CalculatorId> idSet = new HashSet<>(ids);
        return calculators.stream().filter(c -> idSet.contains(c.getId())).toList();
    }
}

interface ComponentRepository {
    void save(Component component);

    Optional<Component> findByName(String name);

    Optional<Component> findById(ComponentId id);

    Collection<Component> findAll();

    Collection<Component> findByNames(Collection<String> names);
}

class InMemoryComponentRepository implements ComponentRepository {
    private final Map<ComponentId, Component> components = new HashMap<>();

    @Override
    public void save(Component component) {
        components.put(component.id(), component);
    }

    @Override
    public Optional<Component> findByName(String name) {
        return components.values().stream()
                .filter(c -> c.name().equals(name))
                .findFirst()
                .map(this::refreshComponent);
    }

    @Override
    public Optional<Component> findById(ComponentId id) {
        return Optional.ofNullable(components.get(id)).map(this::refreshComponent);
    }

    @Override
    public Collection<Component> findAll() {
        return components.values().stream().map(this::refreshComponent).collect(Collectors.toSet());
    }

    @Override
    public Collection<Component> findByNames(Collection<String> names) {
        Set<String> nameSet = new HashSet<>(names);
        return components.values().stream()
                .filter(c -> nameSet.contains(c.name()))
                .map(this::refreshComponent)
                .toList();
    }

    /** Refreshes a composite component with the latest child references from the repository. */
    private Component refreshComponent(Component component) {
        if (!(component instanceof CompositeComponent(var id, var name, var versions))) {
            return component;
        }

        var refreshedVersions = versions.stream().map(this::refreshVersion).toList();

        return new CompositeComponent(id, name, refreshedVersions);
    }

    /** Refreshes a composite version with the latest child references by component ID. */
    private CompositeComponentVersion refreshVersion(CompositeComponentVersion version) {
        List<Component> freshChildren = version.children().stream()
                .map(child -> components.get(child.id()))
                .filter(Objects::nonNull)
                .toList();

        return new CompositeComponentVersion(
                freshChildren,
                version.dependencies(),
                version.applicabilityConstraint(),
                version.validity(),
                version.definedAt());
    }
}
