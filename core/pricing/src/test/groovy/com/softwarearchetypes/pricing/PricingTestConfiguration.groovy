package com.softwarearchetypes.pricing


import java.time.Clock
import java.util.stream.Collectors

final class PricingTestConfiguration {

    private PricingTestConfiguration() {}

    static PricingFacade inMemory(Clock clock) {
        CalculatorRepository calculatorRepository = new InMemoryCalculatorRepository()
        new PricingFacade(calculatorRepository, new InMemoryComponentRepository(), clock)
    }

    final static class InMemoryComponentRepository implements ComponentRepository {

        private final Map<ComponentId, Component> components = new HashMap<>()

        @Override
        void save(Component component) {
            components.put(component.id(), component)
        }

        @Override
        Optional<Component> findByName(String name) {
            components.values().stream()
                    .filter({ component -> component.name() == name })
                    .findFirst()
                    .map(this::refreshComponent)
        }

        @Override
        Optional<Component> findById(ComponentId id) {
            Optional.ofNullable(components.get(id)).map(this::refreshComponent)
        }

        @Override
        Collection<Component> findAll() {
            components.values().stream().map(this::refreshComponent).collect(Collectors.toSet())
        }

        @Override
        Collection<Component> findByNames(Collection<String> names) {
            Set<String> requestedNames = new HashSet<>(names)
            components.values().stream()
                    .filter({ component -> requestedNames.contains(component.name()) })
                    .map({ Component component -> refreshComponent(component) })
                    .toList()
        }

        private Component refreshComponent(Component component) {
            if (!(component instanceof CompositeComponent)) {
                return component
            }

            List<CompositeComponentVersion> refreshedVersions = component.versions().stream()
                    .map({ CompositeComponentVersion version -> refreshVersion(version) })
                    .toList()
            new CompositeComponent(component.id(), component.name(), refreshedVersions)
        }

        private CompositeComponentVersion refreshVersion(CompositeComponentVersion version) {
            List<Component> children = version.children().stream()
                    .map({ child -> components.get(child.id()) })
                    .filter(Objects::nonNull)
                    .toList()
            new CompositeComponentVersion(
                    children,
                    version.dependencies(),
                    version.applicabilityConstraint(),
                    version.validity(),
                    version.definedAt())
        }
    }

    final static class InMemoryCalculatorRepository implements CalculatorRepository {

        private final Set<Calculator> calculators = new HashSet<>()

        @Override
        void save(Calculator calculator) {
            calculators.add(calculator)
        }

        @Override
        Optional<Calculator> findByName(String name) {
            calculators.stream().filter({ calculator -> calculator.name() == name }).findFirst()
        }

        @Override
        Optional<Calculator> findById(CalculatorId id) {
            calculators.stream().filter({ calculator -> calculator.getId() == id }).findFirst()
        }

        @Override
        Collection<Calculator> findAll() {
            new HashSet<>(calculators)
        }

        @Override
        Collection<Calculator> findByIds(Collection<CalculatorId> ids) {
            Set<CalculatorId> requestedIds = new HashSet<>(ids)
            calculators.stream().filter({ calculator -> requestedIds.contains(calculator.getId()) }).toList()
        }
    }
}
