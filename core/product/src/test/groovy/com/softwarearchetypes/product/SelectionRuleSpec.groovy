package com.softwarearchetypes.product

import spock.lang.Specification

class SelectionRuleSpec extends Specification {

    private ProductIdentifier laptop
    private ProductIdentifier mouse
    private ProductIdentifier keyboard
    private ProductIdentifier monitor
    private ProductIdentifier warranty
    private ProductIdentifier insurance

    def setup() {
        laptop = UuidProductIdentifier.random()
        mouse = UuidProductIdentifier.random()
        keyboard = UuidProductIdentifier.random()
        monitor = UuidProductIdentifier.random()
        warranty = UuidProductIdentifier.random()
        insurance = UuidProductIdentifier.random()
    }

    def "is subset of should accept selection within range"() {
        given:
        ProductSet accessories = ProductSet.of("accessories", mouse, keyboard, monitor)
        SelectionRule rule = SelectionRule.isSubsetOf(accessories, 1, 2)

        when:
        boolean result = rule.isSatisfiedBy(select([mouse, keyboard, monitor], quantities))

        then:
        result == satisfied

        where:
        quantities || satisfied
        [1, 0, 0]  || true
        [1, 1, 0]  || true
    }

    def "is subset of should reject selection outside range"() {
        given:
        ProductSet accessories = ProductSet.of("accessories", mouse, keyboard, monitor)
        SelectionRule rule = SelectionRule.isSubsetOf(accessories, 1, 2)

        when:
        boolean result = rule.isSatisfiedBy(select([mouse, keyboard, monitor], quantities))

        then:
        result == satisfied

        where:
        quantities || satisfied
        [0, 0, 0]  || false
        [1, 1, 1]  || false
    }

    def "is subset of should reject products not in set"() {
        given:
        ProductSet accessories = ProductSet.of("accessories", mouse, keyboard)
        SelectionRule rule = SelectionRule.isSubsetOf(accessories, 1, 2)
        List<SelectedProduct> selection = List.of(new SelectedProduct(monitor, 1))

        when:
        boolean satisfied = rule.isSatisfiedBy(selection)

        then:
        !satisfied
    }

    def "is subset of should reject mixed selection"() {
        given:
        ProductSet accessories = ProductSet.of("accessories", mouse, keyboard)
        SelectionRule rule = SelectionRule.isSubsetOf(accessories, 1, 2)
        List<SelectedProduct> selection = List.of(
                new SelectedProduct(mouse, 1),
                new SelectedProduct(monitor, 1)
        )

        when:
        boolean satisfied = rule.isSatisfiedBy(selection)

        then:
        !satisfied
    }

    def "is subset of should count quantities"() {
        given:
        ProductSet accessories = ProductSet.of("accessories", mouse, keyboard)
        SelectionRule rule = SelectionRule.isSubsetOf(accessories, 2, 3)

        when:
        boolean result = rule.isSatisfiedBy(select([mouse, keyboard], quantities))

        then:
        result == satisfied

        where:
        quantities || satisfied
        [1, 1]     || true
        [2, 0]     || true
        [1, 0]     || false
    }

    def "required should accept selection with product"() {
        given:
        ProductSet laptopSet = ProductSet.of("laptopSet", laptop)
        SelectionRule rule = SelectionRule.required(laptopSet)
        List<SelectedProduct> selection = List.of(new SelectedProduct(laptop, 1))

        when:
        boolean satisfied = rule.isSatisfiedBy(selection)

        then:
        satisfied
    }

    def "required should reject empty selection"() {
        given:
        ProductSet laptopSet = ProductSet.of("laptopSet", laptop)
        SelectionRule rule = SelectionRule.required(laptopSet)

        when:
        boolean satisfied = rule.isSatisfiedBy(List.of())

        then:
        !satisfied
    }

    def "optional should accept both with and without product"() {
        given:
        ProductSet warrantySet = ProductSet.of("warranty", warranty)
        SelectionRule rule = SelectionRule.optional(warrantySet)

        when:
        boolean result = rule.isSatisfiedBy(select([warranty], quantities))

        then:
        result == satisfied

        where:
        quantities || satisfied
        [1]        || true
        [0]        || true
    }

    def "single should accept exactly one"() {
        given:
        ProductSet laptopSet = ProductSet.of("laptop", laptop)
        SelectionRule rule = SelectionRule.single(laptopSet)
        List<SelectedProduct> selection = List.of(new SelectedProduct(laptop, 1))

        when:
        boolean satisfied = rule.isSatisfiedBy(selection)

        then:
        satisfied
    }

    def "single should reject zero or multiple"() {
        given:
        ProductSet laptopSet = ProductSet.of("laptop", laptop)
        SelectionRule rule = SelectionRule.single(laptopSet)

        when:
        boolean result = rule.isSatisfiedBy(select([laptop], quantities))

        then:
        result == satisfied

        where:
        quantities || satisfied
        [0]        || false
        [2]        || false
    }

    def "and should require all rules to be satisfied"() {
        given:
        ProductSet laptopSet = ProductSet.of("laptop", laptop)
        ProductSet mouseSet = ProductSet.of("mouse", mouse)
        SelectionRule rule = SelectionRule.and(
                SelectionRule.required(laptopSet),
                SelectionRule.required(mouseSet)
        )

        when:
        boolean result = rule.isSatisfiedBy(select([laptop, mouse], quantities))

        then:
        result == satisfied

        where:
        quantities || satisfied
        [1, 1]     || true
        [1, 0]     || false
        [0, 1]     || false
        [0, 0]     || false
    }

    def "and should work with three rules"() {
        given:
        SelectionRule rule = SelectionRule.and(
                SelectionRule.required(ProductSet.of("laptop", laptop)),
                SelectionRule.required(ProductSet.of("mouse", mouse)),
                SelectionRule.required(ProductSet.of("keyboard", keyboard))
        )

        when:
        boolean result = rule.isSatisfiedBy(select([laptop, mouse, keyboard], quantities))

        then:
        result == satisfied

        where:
        quantities || satisfied
        [1, 1, 1]  || true
        [1, 1, 0]  || false
    }

    def "or should require at least one rule to be satisfied"() {
        given:
        ProductSet mouseSet = ProductSet.of("mouse", mouse)
        ProductSet keyboardSet = ProductSet.of("keyboard", keyboard)
        SelectionRule rule = SelectionRule.or(
                SelectionRule.required(mouseSet),
                SelectionRule.required(keyboardSet)
        )

        when:
        boolean result = rule.isSatisfiedBy(select([mouse, keyboard], quantities))

        then:
        result == satisfied

        where:
        quantities || satisfied
        [1, 0]     || true
        [0, 1]     || true
        [1, 1]     || true
        [0, 0]     || false
    }

    def "or should work with three rules"() {
        given:
        SelectionRule rule = SelectionRule.or(
                SelectionRule.required(ProductSet.of("mouse", mouse)),
                SelectionRule.required(ProductSet.of("keyboard", keyboard)),
                SelectionRule.required(ProductSet.of("monitor", monitor))
        )

        when:
        boolean result = rule.isSatisfiedBy(select([mouse, keyboard, monitor], quantities))

        then:
        result == satisfied

        where:
        quantities || satisfied
        [1, 0, 0]  || true
        [0, 1, 0]  || true
        [0, 0, 1]  || true
        [1, 0, 1]  || true
        [0, 0, 0]  || false
    }

    def "not should invert rule result"() {
        given:
        ProductSet insuranceSet = ProductSet.of("insurance", insurance)
        SelectionRule rule = SelectionRule.not(SelectionRule.required(insuranceSet))

        when:
        boolean result = rule.isSatisfiedBy(select([insurance], quantities))

        then:
        result == satisfied

        where:
        quantities || satisfied
        [0]        || true
        [1]        || false
    }

    def "not should work with complex rules"() {
        given:
        SelectionRule rule = SelectionRule.not(
                SelectionRule.and(
                        SelectionRule.required(ProductSet.of("laptop", laptop)),
                        SelectionRule.required(ProductSet.of("mouse", mouse))
                )
        )

        when:
        boolean result = rule.isSatisfiedBy(select([laptop, mouse], quantities))

        then:
        result == satisfied

        where:
        quantities || satisfied
        [0, 0]     || true
        [1, 0]     || true
        [0, 1]     || true
        [1, 1]     || false
    }

    def "if then should enforce then rules when condition is met"() {
        given:
        SelectionRule rule = SelectionRule.ifThen(
                SelectionRule.required(ProductSet.of("laptop", laptop)),
                SelectionRule.required(ProductSet.of("warranty", warranty))
        )

        when:
        boolean result = rule.isSatisfiedBy(select([laptop, warranty], quantities))

        then:
        result == satisfied

        where:
        quantities || satisfied
        [0, 0]     || true
        [1, 1]     || true
        [1, 0]     || false
    }

    def "if then should work with multiple then rules"() {
        given:
        SelectionRule rule = SelectionRule.ifThen(
                SelectionRule.required(ProductSet.of("laptop", laptop)),
                SelectionRule.required(ProductSet.of("warranty", warranty)),
                SelectionRule.required(ProductSet.of("insurance", insurance))
        )

        when:
        boolean result = rule.isSatisfiedBy(select([laptop, warranty, insurance], quantities))

        then:
        result == satisfied

        where:
        quantities || satisfied
        [0, 0, 0]  || true
        [1, 1, 1]  || true
        [1, 1, 0]  || false
        [1, 0, 1]  || false
    }

    def "should combine and or not in complex scenario"() {
        given:
        SelectionRule rule = SelectionRule.and(
                SelectionRule.or(
                        SelectionRule.required(ProductSet.of("laptop", laptop)),
                        SelectionRule.required(ProductSet.of("monitor", monitor))
                ),
                SelectionRule.not(
                        SelectionRule.required(ProductSet.of("insurance", insurance))
                )
        )

        when:
        boolean result = rule.isSatisfiedBy(select([laptop, monitor, insurance], quantities))

        then:
        result == satisfied

        where:
        quantities || satisfied
        [1, 0, 0]  || true
        [0, 1, 0]  || true
        [1, 1, 0]  || true
        [1, 0, 1]  || false
        [0, 0, 0]  || false
    }

    def "should combine if then with and or"() {
        given:
        SelectionRule rule = SelectionRule.ifThen(
                SelectionRule.required(ProductSet.of("laptop", laptop)),
                SelectionRule.or(
                        SelectionRule.required(ProductSet.of("warranty", warranty)),
                        SelectionRule.required(ProductSet.of("insurance", insurance))
                )
        )

        when:
        boolean result = rule.isSatisfiedBy(select([laptop, warranty, insurance], quantities))

        then:
        result == satisfied

        where:
        quantities || satisfied
        [0, 0, 0]  || true
        [1, 1, 0]  || true
        [1, 0, 1]  || true
        [1, 1, 1]  || true
        [1, 0, 0]  || false
    }

    def "should handle nested conditionals"() {
        given:
        SelectionRule innerIfThen = SelectionRule.ifThen(
                SelectionRule.required(ProductSet.of("monitor", monitor)),
                SelectionRule.required(ProductSet.of("warranty", warranty))
        )
        SelectionRule rule = SelectionRule.ifThen(
                SelectionRule.required(ProductSet.of("laptop", laptop)),
                innerIfThen
        )

        when:
        boolean result = rule.isSatisfiedBy(select([laptop, monitor, warranty], quantities))

        then:
        result == satisfied

        where:
        quantities || satisfied
        [0, 0, 0]  || true
        [1, 0, 0]  || true
        [1, 1, 1]  || true
        [1, 1, 0]  || false
    }

    def "should handle real world banking scenario"() {
        given:
        ProductIdentifier basicCard = UuidProductIdentifier.random()
        ProductIdentifier premiumCard = UuidProductIdentifier.random()
        ProductIdentifier basicInsurance = UuidProductIdentifier.random()
        ProductIdentifier extendedInsurance = UuidProductIdentifier.random()
        ProductIdentifier investmentAccount = UuidProductIdentifier.random()
        SelectionRule rule = SelectionRule.and(
                SelectionRule.or(
                        SelectionRule.required(ProductSet.of("basic", basicCard)),
                        SelectionRule.required(ProductSet.of("premium", premiumCard))
                ),
                SelectionRule.ifThen(
                        SelectionRule.required(ProductSet.of("premium", premiumCard)),
                        SelectionRule.required(ProductSet.of("extendedInsurance", extendedInsurance)),
                        SelectionRule.required(ProductSet.of("investmentAccount", investmentAccount)),
                        SelectionRule.not(SelectionRule.required(ProductSet.of("basicInsurance", basicInsurance)))
                )
        )

        when:
        boolean result = rule.isSatisfiedBy(select(
                [basicCard, premiumCard, basicInsurance, extendedInsurance, investmentAccount],
                quantities
        ))

        then:
        result == satisfied

        where:
        quantities      || satisfied
        [1, 0, 0, 0, 0] || true
        [1, 0, 1, 0, 0] || true
        [0, 1, 0, 1, 1] || true
        [0, 1, 0, 0, 1] || false
        [0, 1, 1, 1, 1] || false
    }

    private static List<SelectedProduct> select(
            List<ProductIdentifier> products,
            List<Integer> quantities
    ) {
        [products, quantities].transpose()
                .findAll { product, quantity -> quantity > 0 }
                .collect { product, quantity -> new SelectedProduct(product as ProductIdentifier, quantity as int) }
    }
}
