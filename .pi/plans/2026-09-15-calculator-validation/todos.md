# Calculator validation todos

Authoritative plan: `.pi/plans/2026-09-15-calculator-validation/plan.md`
Scout context: `.pi/plans/2026-09-15-calculator-validation/scout-context.md`
Tag: `calculator-validation`
Execution: sequential only
Worker runtime: `openai-codex/gpt-5.6-luna`, thinking `medium`

Workers must change their todo status from `open` to `claimed`, then `closed`, and create one polished conventional
commit per todo. Follow the plan, repository Spock conventions, and test-first sequence. Do not introduce a registry,
persistence, a custom runtime kind, creation-schema migration, enum-value removal, new exception hierarchy, or Fair
Share domain semantics in core.

## TODO-CV-01: Add typed calculator inputs

- Status: closed
- Depends on: none
- Acceptance: ISC-3, ISC-4, ISC-5, ISC-7, ISC-9

Create `CalculatorInputSpec` first, then add the immutable final `CalculatorInput<T>` core abstraction. Cover
missing/null, convertible `Money` and `BigDecimal`, wrong type/format, strict `instanceOf`, and stable equality/hash
semantics. Keep reader/converter private. Identity must use name plus fixed descriptor type, never lambda identity.
Reuse existing `Parameters` conversions and retain conversion exceptions as causes.

Required shape:

```java
public final class CalculatorInput<T> {
    private final String name;
    private final String descriptorType;
    private final String expectedType;
    private final Reader<T> reader;

    public static CalculatorInput<Money> money(String name) {
        return new CalculatorInput<>(name, "convert:money", "Money", p -> p.getMoney(name));
    }

    public static CalculatorInput<BigDecimal> bigDecimal(String name) {
        return new CalculatorInput<>(name, "convert:big-decimal", "BigDecimal", p -> p.getBigDecimal(name));
    }

    public static <T> CalculatorInput<T> instanceOf(String name, Class<T> type) {
        return new CalculatorInput<>(
                name,
                "instance:" + type.getName(),
                type.getSimpleName(),
                p -> type.cast(p.require(name)));
    }
}
```

Anti-patterns: public record exposing `Function`; public converter; lambda-based equality; duplicated conversion logic;
use for calculator creation fields; app-specific factories.

## TODO-CV-02: Centralize Calculator validation

- Status: closed
- Depends on: TODO-CV-01
- Acceptance: ISC-1, ISC-2, ISC-3, ISC-4, ISC-5, ISC-6, ISC-8, ISC-9, ISC-10

Create `CalculatorValidationSpec` first. Change `Calculator` so `inputs()` is mandatory, `calculate()` validates
contract and values deterministically, and implementations provide `calculateWithValidInputs()`. Cover invalid
custom-style input before logic, explicit parameterless calculators, simulation, compatible duplicates, and incompatible
duplicate-name contracts. Migrate every core calculator in the same compiling change. Remove repeated presence guards,
retain typed reads and business validation, and read through declared descriptors where practical.

Required interface shape:

```java
public interface Calculator {
    Set<CalculatorInput<?>> inputs();

    default Money calculate(Parameters parameters) {
        validateInputContract();
        inputs().stream()
                .sorted(comparing(CalculatorInput::name))
                .forEach(input -> input.read(parameters));
        return calculateWithValidInputs(parameters);
    }

    Money calculateWithValidInputs(Parameters parameters);
}
```

Required implementation shape:

```java
private static final CalculatorInput<BigDecimal> QUANTITY =
        CalculatorInput.bigDecimal("quantity");

@Override
public Set<CalculatorInput<?>> inputs() {
    return Set.of(QUANTITY);
}

@Override
public Money calculateWithValidInputs(Parameters parameters) {
    BigDecimal quantity = QUANTITY.read(parameters);
    // Existing calculation unchanged.
}
```

Document that interface defaults cannot be final and the implementation hook is public. Anti-patterns: abstract-class
migration; executor wrapper; optional/default-empty `inputs()`; `kind()`; removing or renaming `getType()`; catching
calculator business exceptions; semantic formula changes.

## TODO-CV-03: Preserve adapter input contracts

- Status: open
- Depends on: TODO-CV-02
- Acceptance: ISC-9, ISC-10, ISC-13

Extend `AdaptersSpec` first. A wrapped calculator must require `quantity` plus an unrelated typed value and receive both
on every generated evaluation. Cover compatible duplicate `quantity` descriptors and incompatible descriptor failure.
Union each adapter's `quantity` descriptor with wrapped-calculator inputs. Derived evaluations must replace only
`quantity` and retain timestamps, mapped values, and custom inputs.

Required union shape:

```java
@Override
public Set<CalculatorInput<?>> inputs() {
    return Stream.concat(Stream.of(QUANTITY), sourceCalculator.inputs().stream())
            .collect(Collectors.toUnmodifiableSet());
}
```

Required derivation shapes:

```java
Parameters previous = parameters.with("quantity", quantity.subtract(BigDecimal.ONE));
Money previousTotal = sourceCalculator.calculate(previous);
```

```java
for (int i = 1; i <= quantity.intValue(); i++) {
    Parameters atQuantity = parameters.with("quantity", BigDecimal.valueOf(i));
    total = total.add(sourceCalculator.calculate(atQuantity));
}
```

Anti-patterns: `Parameters.of("quantity", ...)` in derived calls; silent first-descriptor wins; unioning all composite
repository calculators; bypassing wrapped `calculate()`; suppressing nested validation.

## TODO-CV-04: Validate selected component version

- Status: open
- Depends on: TODO-CV-03
- Acceptance: ISC-11, ISC-12

Add a test first in `SimpleComponentVersioningSpec` or `ComponentSpec` with two versions having different typed inputs
and mappings. At explicit timestamps, only the selected version's mapped contract must be validated. Assert
applicability is checked before calculator validation. Delete `SimpleComponent.requiredParameters()` without
replacement.

Preserve this order:

```java
PricingContext context = PricingContext.from(parameters);
SimpleComponentVersion version = versionAt(context.timestamp());
if (!version.isApplicableFor(context)) {
    return Money.zero("PLN");
}
Parameters transformed = transformParameters(parameters, version.parameterMappings());
Calculator adapted = InterpretationAdapters.adapt(version.calculator(), targetInterpretation);
return adapted.calculate(transformed);
```

Anti-patterns: requirements from first version; union across versions; validation before mappings/applicability;
context-free replacement query; changed version tie-breaking or clock fallback.

## TODO-CV-05: Migrate currency conversion calculator

- Status: open
- Depends on: TODO-CV-04
- Acceptance: ISC-7, ISC-14, ISC-17, ISC-18, ISC-19, ISC-20, ISC-21

Add direct app coverage first for declared `Money source`, missing/null/wrong source, and convertible Money text if
supported. Migrate `CurrencyConversionCalculator` to its public core descriptor and validated body. Keep
`CalculatorType.CUSTOM`. Run or strengthen Valuation regressions for implicit same-currency version, selected
`ComponentVersionId`, latest `validFrom`, later-list-item equal-start tie, explicit calculation time, direction
validation, and multiply-then-single-round behavior.

Required shape:

```java
private static final CalculatorInput<Money> SOURCE =
        CalculatorInput.money("source");

@Override
public Set<CalculatorInput<?>> inputs() {
    return Set.of(SOURCE);
}

@Override
public Money calculateWithValidInputs(Parameters parameters) {
    return exchangeRate.convert(SOURCE.read(parameters));
}
```

Anti-patterns: Exchange Rate or Valuation logic in core; routing Valuation through `SimpleComponent.versionAt`; changing
list-order ties; persisting `CalculatorId`; changing conversion order/rounding; generic calculator registration.

## TODO-CV-06: Remove enum runtime metadata and verify

- Status: open
- Depends on: TODO-CV-05
- Acceptance: ISC-15, ISC-16, ISC-A-1, ISC-A-2, ISC-A-3, ISC-A-4, ISC-A-5

Remove only `requiredCalculationFields` and its accessor from `CalculatorType`. Keep `requiredCreationFields`, all enum
constants, `getType()`, `CalculatorView`, grouping, and `PricingFacade` creation behavior. Update tests that referenced
runtime enum requirements to assert calculator-owned descriptors.

Required enum shape:

```java
public enum CalculatorType {
    SIMPLE_FIXED("simple-fixed", "Fixed amount calculator - returns %s regardless", Set.of("amount")),
    // Existing values, including COMPOSITE, CUSTOM, and adapters.

    private final String typeName;
    private final String descriptionTemplate;
    private final Set<String> requiredCreationFields;
}
```

Configure Java and run final checks:

```sh
export JAVA_HOME="$HOME/.local/bin/sdkman/candidates/java/25-librca"
./gradlew format check
./gradlew scan
```

Anti-patterns: changing facade creation validation; typed creation schema; deleting `CUSTOM` or adapter constants;
replacing `getType()` with `kind()`; unrelated `Parameters` redesign; new dependencies or architecture documents.
