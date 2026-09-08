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
        ProductCommands.AllowedValuesConfig.&new | [null]
        ProductCommands.AllowedValuesConfig.&new | [Set.of()]
        ProductCommands.NumericRangeConfig.&new  | [2, 1]
        ProductCommands.DecimalRangeConfig.&new  | [null, "2"]
        ProductCommands.DecimalRangeConfig.&new  | ["1", null]
        ProductCommands.RegexConfig.&new         | [null]
        ProductCommands.RegexConfig.&new         | [""]
        ProductCommands.RegexConfig.&new         | ["   "]
        ProductCommands.DateRangeConfig.&new     | [null, "2025-01-01"]
        ProductCommands.DateRangeConfig.&new     | ["2025-01-01", null]
        ProductCommands.UnconstrainedConfig.&new | [null]
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
        ProductCommands.DefineProductType.&new | [null, "id", "name", "desc", "pcs", "IDENTICAL", null, null, null]
        ProductCommands.DefineProductType.&new | ["", "id", "name", "desc", "pcs", "IDENTICAL", null, null, null]
        ProductCommands.DefineProductType.&new | ["UUID", null, "name", "desc", "pcs", "IDENTICAL", null, null, null]
        ProductCommands.DefineProductType.&new | ["UUID", "", "name", "desc", "pcs", "IDENTICAL", null, null, null]
        ProductCommands.DefineProductType.&new | ["UUID", "id", null, "desc", "pcs", "IDENTICAL", null, null, null]
        ProductCommands.DefineProductType.&new | ["UUID", "id", "", "desc", "pcs", "IDENTICAL", null, null, null]
        ProductCommands.DefineProductType.&new | ["UUID", "id", "name", null, "pcs", "IDENTICAL", null, null, null]
        ProductCommands.DefineProductType.&new | ["UUID", "id", "name", "", "pcs", "IDENTICAL", null, null, null]
        ProductCommands.DefineProductType.&new | ["UUID", "id", "name", "desc", null, "IDENTICAL", null, null, null]
        ProductCommands.DefineProductType.&new | ["UUID", "id", "name", "desc", "", "IDENTICAL", null, null, null]
        ProductCommands.DefineProductType.&new | ["UUID", "id", "name", "desc", "pcs", null, null, null, null]
        ProductCommands.DefineProductType.&new | ["UUID", "id", "name", "desc", "pcs", "", null, null, null]
        ProductCommands.MandatoryFeature.&new  | [null, new ProductCommands.UnconstrainedConfig("STRING")]
        ProductCommands.MandatoryFeature.&new  | ["", new ProductCommands.UnconstrainedConfig("STRING")]
        ProductCommands.MandatoryFeature.&new  | ["name", null]
        ProductCommands.OptionalFeature.&new   | [null, new ProductCommands.UnconstrainedConfig("STRING")]
        ProductCommands.OptionalFeature.&new   | ["", new ProductCommands.UnconstrainedConfig("STRING")]
        ProductCommands.OptionalFeature.&new   | ["name", null]
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
        ProductCommands.AddToOffer.&new         | [null, "display", "desc", Set.of(), null, null, [:]]
        ProductCommands.AddToOffer.&new         | ["", "display", "desc", Set.of(), null, null, [:]]
        ProductCommands.AddToOffer.&new         | ["type", null, "desc", Set.of(), null, null, [:]]
        ProductCommands.AddToOffer.&new         | ["type", "", "desc", Set.of(), null, null, [:]]
        ProductCommands.AddToOffer.&new         | ["type", "display", null, Set.of(), null, null, [:]]
        ProductCommands.AddToOffer.&new         | ["type", "display", "", Set.of(), null, null, [:]]
        ProductCommands.DiscontinueProduct.&new | [null, LocalDate.now()]
        ProductCommands.DiscontinueProduct.&new | ["", LocalDate.now()]
        ProductCommands.DiscontinueProduct.&new | ["entry", null]
        ProductCommands.UpdateMetadata.&new     | [null, [:]]
        ProductCommands.UpdateMetadata.&new     | ["", [:]]
        ProductCommands.UpdateMetadata.&new     | ["entry", null]
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
        ProductCommands.DefinePackageType.&new | [null, "id", "name", "desc", "pcs", "IDENTICAL", Set.of(new ProductCommands.SingleConfig(Set.of("p"))), null]
        ProductCommands.DefinePackageType.&new | ["", "id", "name", "desc", "pcs", "IDENTICAL", Set.of(new ProductCommands.SingleConfig(Set.of("p"))), null]
        ProductCommands.DefinePackageType.&new | ["UUID", null, "name", "desc", "pcs", "IDENTICAL", Set.of(new ProductCommands.SingleConfig(Set.of("p"))), null]
        ProductCommands.DefinePackageType.&new | ["UUID", "", "name", "desc", "pcs", "IDENTICAL", Set.of(new ProductCommands.SingleConfig(Set.of("p"))), null]
        ProductCommands.DefinePackageType.&new | ["UUID", "id", null, "desc", "pcs", "IDENTICAL", Set.of(new ProductCommands.SingleConfig(Set.of("p"))), null]
        ProductCommands.DefinePackageType.&new | ["UUID", "id", "", "desc", "pcs", "IDENTICAL", Set.of(new ProductCommands.SingleConfig(Set.of("p"))), null]
        ProductCommands.DefinePackageType.&new | ["UUID", "id", "name", null, "pcs", "IDENTICAL", Set.of(new ProductCommands.SingleConfig(Set.of("p"))), null]
        ProductCommands.DefinePackageType.&new | ["UUID", "id", "name", "", "pcs", "IDENTICAL", Set.of(new ProductCommands.SingleConfig(Set.of("p"))), null]
        ProductCommands.DefinePackageType.&new | ["UUID", "id", "name", "desc", null, "IDENTICAL", Set.of(new ProductCommands.SingleConfig(Set.of("p"))), null]
        ProductCommands.DefinePackageType.&new | ["UUID", "id", "name", "desc", "", "IDENTICAL", Set.of(new ProductCommands.SingleConfig(Set.of("p"))), null]
        ProductCommands.DefinePackageType.&new | ["UUID", "id", "name", "desc", "pcs", null, Set.of(new ProductCommands.SingleConfig(Set.of("p"))), null]
        ProductCommands.DefinePackageType.&new | ["UUID", "id", "name", "desc", "pcs", "", Set.of(new ProductCommands.SingleConfig(Set.of("p"))), null]
        ProductCommands.DefinePackageType.&new | ["UUID", "id", "name", "desc", "pcs", "IDENTICAL", null, null]
        ProductCommands.DefinePackageType.&new | ["UUID", "id", "name", "desc", "pcs", "IDENTICAL", Set.of(), null]
    }

    def "rejects invalid selection rule configurations"() {
        when:
        constructor.call(*arguments)

        then:
        thrown(IllegalArgumentException)

        where:
        constructor                           | arguments
        ProductCommands.IsSubsetOfConfig.&new | [null, 0, 1]
        ProductCommands.IsSubsetOfConfig.&new | [Set.of(), 0, 1]
        ProductCommands.IsSubsetOfConfig.&new | [Set.of("p"), -1, 1]
        ProductCommands.IsSubsetOfConfig.&new | [Set.of("p"), 2, 1]
        ProductCommands.SingleConfig.&new     | [null]
        ProductCommands.SingleConfig.&new     | [Set.of()]
        ProductCommands.OptionalConfig.&new   | [null]
        ProductCommands.OptionalConfig.&new   | [Set.of()]
        ProductCommands.RequiredConfig.&new   | [null]
        ProductCommands.RequiredConfig.&new   | [Set.of()]
        ProductCommands.AndRuleConfig.&new    | [null]
        ProductCommands.AndRuleConfig.&new    | [Set.of()]
        ProductCommands.OrRuleConfig.&new     | [null]
        ProductCommands.OrRuleConfig.&new     | [Set.of()]
        ProductCommands.NotRuleConfig.&new    | [null]
        ProductCommands.IfThenRuleConfig.&new | [null, Set.of(new ProductCommands.SingleConfig(Set.of("p")))]
        ProductCommands.IfThenRuleConfig.&new | [new ProductCommands.SingleConfig(Set.of("p")), null]
        ProductCommands.IfThenRuleConfig.&new | [new ProductCommands.SingleConfig(Set.of("p")), Set.of()]
    }

    def "rejects invalid instance commands"() {
        when:
        constructor.call(*arguments)

        then:
        thrown(IllegalArgumentException)

        where:
        constructor                                 | arguments
        ProductCommands.CreateProductInstance.&new  | [null, null, null, "1", "pcs", Set.of()]
        ProductCommands.CreateProductInstance.&new  | ["", null, null, "1", "pcs", Set.of()]
        ProductCommands.CreateProductInstance.&new  | ["type", null, null, null, "pcs", Set.of()]
        ProductCommands.CreateProductInstance.&new  | ["type", null, null, "", "pcs", Set.of()]
        ProductCommands.CreateProductInstance.&new  | ["type", null, null, "1", null, Set.of()]
        ProductCommands.CreateProductInstance.&new  | ["type", null, null, "1", "", Set.of()]
        ProductCommands.FeatureInstanceConfig.&new  | [null, "value"]
        ProductCommands.FeatureInstanceConfig.&new  | ["", "value"]
        ProductCommands.FeatureInstanceConfig.&new  | ["name", null]
        ProductCommands.CreatePackageInstance.&new  | [null, null, null, Set.of(new ProductCommands.SelectedInstanceConfig("i", 1))]
        ProductCommands.CreatePackageInstance.&new  | ["", null, null, Set.of(new ProductCommands.SelectedInstanceConfig("i", 1))]
        ProductCommands.CreatePackageInstance.&new  | ["type", null, null, null]
        ProductCommands.CreatePackageInstance.&new  | ["type", null, null, Set.of()]
        ProductCommands.SelectedInstanceConfig.&new | [null, 1]
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
