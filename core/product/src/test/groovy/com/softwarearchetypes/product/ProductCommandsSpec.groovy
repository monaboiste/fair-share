package com.softwarearchetypes.product

import java.time.LocalDate
import spock.lang.Specification

class ProductCommandsSpec extends Specification {

    def "rejects invalid feature constraint configurations"() {
        when:
        constructor.call(*arguments)

        then:
        thrown(IllegalArgumentException)

        where:
        constructor                              | arguments
        ProductCommands.AllowedValuesConfig.&new | [Set.of()]
        ProductCommands.NumericRangeConfig.&new  | [2, 1]
        ProductCommands.RegexConfig.&new         | [""]
        ProductCommands.RegexConfig.&new         | ["   "]
        ProductCommands.UnconstrainedConfig.&new | [""]
        ProductCommands.UnconstrainedConfig.&new | ["   "]
    }

    def "creates valid feature constraint configuration"() {
        expect:
        config != null

        where:
        config << [
                new ProductCommands.AllowedValuesConfig(Set.of("red")),
                new ProductCommands.NumericRangeConfig(1, 2),
                new ProductCommands.DecimalRangeConfig("1", "2"),
                new ProductCommands.RegexConfig("red.*"),
                new ProductCommands.DateRangeConfig("2025-01-01", "2025-12-31"),
                new ProductCommands.UnconstrainedConfig("STRING")
        ]
    }

    def "rejects invalid product feature definitions"() {
        when:
        constructor.call(*arguments)

        then:
        thrown(IllegalArgumentException)

        where:
        constructor                            | arguments
        ProductCommands.DefineProductType.&new | ["", "id", "name", "desc", "pcs", "IDENTICAL", null, null, null]
        ProductCommands.DefineProductType.&new | ["UUID", "", "name", "desc", "pcs", "IDENTICAL", null, null, null]
        ProductCommands.DefineProductType.&new | ["UUID", "id", "", "desc", "pcs", "IDENTICAL", null, null, null]
        ProductCommands.DefineProductType.&new | ["UUID", "id", "name", "", "pcs", "IDENTICAL", null, null, null]
        ProductCommands.DefineProductType.&new | ["UUID", "id", "name", "desc", "", "IDENTICAL", null, null, null]
        ProductCommands.DefineProductType.&new | ["UUID", "id", "name", "desc", "pcs", "", null, null, null]
        ProductCommands.MandatoryFeature.&new  | ["", new ProductCommands.UnconstrainedConfig("STRING")]
        ProductCommands.OptionalFeature.&new   | ["", new ProductCommands.UnconstrainedConfig("STRING")]
    }

    def "creates product definition with optional null values"() {
        when:
        def command = new ProductCommands.DefineProductType("UUID", "id", "name", "desc", "pcs", "IDENTICAL", null, null, null)

        then:
        command.mandatoryFeatures() == null
        command.optionalFeatures() == null
        command.metadata() == null
    }

    def "rejects invalid offer and lifecycle commands"() {
        when:
        constructor.call(*arguments)

        then:
        thrown(IllegalArgumentException)

        where:
        constructor                             | arguments
        ProductCommands.AddToOffer.&new         | ["", "display", "desc", Set.of(), null, null, [:]]
        ProductCommands.AddToOffer.&new         | ["type", "", "desc", Set.of(), null, null, [:]]
        ProductCommands.AddToOffer.&new         | ["type", "display", "", Set.of(), null, null, [:]]
        ProductCommands.DiscontinueProduct.&new | ["", LocalDate.now()]
        ProductCommands.UpdateMetadata.&new     | ["", [:]]
    }

    def "creates offer with optional dates"() {
        expect:
        new ProductCommands.AddToOffer("type", "display", "desc", Set.of(), null, null, [:])
    }

    def "creates package definition with optional null metadata"() {
        when:
        def command = new ProductCommands.DefinePackageType("UUID", "id", "name", "desc", "pcs", "IDENTICAL", Set.of(new ProductCommands.SingleConfig(Set.of("p"))), null)

        then:
        command.metadata() == null
    }

    def "rejects invalid selection configurations"() {
        when:
        constructor.call(*arguments)

        then:
        thrown(IllegalArgumentException)

        where:
        constructor                            | arguments
        ProductCommands.DefinePackageType.&new | ["", "id", "name", "desc", "pcs", "IDENTICAL", Set.of(new ProductCommands.SingleConfig(Set.of("p"))), null]
        ProductCommands.DefinePackageType.&new | ["UUID", "", "name", "desc", "pcs", "IDENTICAL", Set.of(new ProductCommands.SingleConfig(Set.of("p"))), null]
        ProductCommands.DefinePackageType.&new | ["UUID", "id", "", "desc", "pcs", "IDENTICAL", Set.of(new ProductCommands.SingleConfig(Set.of("p"))), null]
        ProductCommands.DefinePackageType.&new | ["UUID", "id", "name", "", "pcs", "IDENTICAL", Set.of(new ProductCommands.SingleConfig(Set.of("p"))), null]
        ProductCommands.DefinePackageType.&new | ["UUID", "id", "name", "desc", "", "IDENTICAL", Set.of(new ProductCommands.SingleConfig(Set.of("p"))), null]
        ProductCommands.DefinePackageType.&new | ["UUID", "id", "name", "desc", "pcs", "", Set.of(new ProductCommands.SingleConfig(Set.of("p"))), null]
        ProductCommands.DefinePackageType.&new | ["UUID", "id", "name", "desc", "pcs", "IDENTICAL", Set.of(), null]
    }

    def "rejects invalid selection rule configurations"() {
        when:
        constructor.call(*arguments)

        then:
        thrown(IllegalArgumentException)

        where:
        constructor                           | arguments
        ProductCommands.IsSubsetOfConfig.&new | [Set.of(), 0, 1]
        ProductCommands.IsSubsetOfConfig.&new | [Set.of("p"), -1, 1]
        ProductCommands.IsSubsetOfConfig.&new | [Set.of("p"), 2, 1]
        ProductCommands.SingleConfig.&new     | [Set.of()]
        ProductCommands.OptionalConfig.&new   | [Set.of()]
        ProductCommands.RequiredConfig.&new   | [Set.of()]
        ProductCommands.AndRuleConfig.&new    | [Set.of()]
        ProductCommands.OrRuleConfig.&new     | [Set.of()]
        ProductCommands.IfThenRuleConfig.&new | [new ProductCommands.SingleConfig(Set.of("p")), Set.of()]
    }

    def "rejects invalid instance commands"() {
        when:
        constructor.call(*arguments)

        then:
        thrown(IllegalArgumentException)

        where:
        constructor                                 | arguments
        ProductCommands.CreateProductInstance.&new  | ["", null, null, "1", "pcs", Set.of()]
        ProductCommands.CreateProductInstance.&new  | ["type", null, null, "", "pcs", Set.of()]
        ProductCommands.CreateProductInstance.&new  | ["type", null, null, "1", "", Set.of()]
        ProductCommands.FeatureInstanceConfig.&new  | ["", "value"]
        ProductCommands.CreatePackageInstance.&new  | ["", null, null, Set.of(new ProductCommands.SelectedInstanceConfig("i", 1))]
        ProductCommands.CreatePackageInstance.&new  | ["type", null, null, Set.of()]
        ProductCommands.SelectedInstanceConfig.&new | ["", 1]
        ProductCommands.SelectedInstanceConfig.&new | ["id", 0]
        ProductCommands.SelectedInstanceConfig.&new | ["id", -1]
    }

    def "creates instance commands with optional null values"() {
        expect:
        new ProductCommands.CreateProductInstance("type", null, null, "1", "pcs", Set.of())
        new ProductCommands.CreatePackageInstance("type", null, null, Set.of(new ProductCommands.SelectedInstanceConfig("id", 1)))
    }
}
