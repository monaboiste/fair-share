package com.softwarearchetypes.product;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Commands accepted by {@link ProductFacade} and {@link ProductCatalog}. */
public final class ProductCommands {

    private ProductCommands() {}

    /** Configuration for a feature value constraint. */
    public sealed interface FeatureConstraintConfig
            permits AllowedValuesConfig,
                    NumericRangeConfig,
                    DecimalRangeConfig,
                    RegexConfig,
                    DateRangeConfig,
                    UnconstrainedConfig {}

    public record AllowedValuesConfig(Set<String> allowedValues) implements FeatureConstraintConfig {
        public AllowedValuesConfig {
            if (allowedValues == null || allowedValues.isEmpty()) {
                throw new IllegalArgumentException("Allowed values must not be empty");
            }
        }
    }

    public record NumericRangeConfig(int min, int max) implements FeatureConstraintConfig {
        public NumericRangeConfig {
            if (min > max) {
                throw new IllegalArgumentException("Min must be less than or equal to max");
            }
        }
    }

    public record DecimalRangeConfig(String min, String max) implements FeatureConstraintConfig {
        public DecimalRangeConfig {
            if (min == null) {
                throw new IllegalArgumentException("Min must be defined");
            }
            if (max == null) {
                throw new IllegalArgumentException("Max must be defined");
            }
        }
    }

    public record RegexConfig(String pattern) implements FeatureConstraintConfig {
        public RegexConfig {
            if (pattern == null || pattern.isBlank()) {
                throw new IllegalArgumentException("Pattern must be defined");
            }
        }
    }

    public record DateRangeConfig(String from, String to) implements FeatureConstraintConfig {
        public DateRangeConfig {
            if (from == null) {
                throw new IllegalArgumentException("From date must be defined");
            }
            if (to == null) {
                throw new IllegalArgumentException("To date must be defined");
            }
        }
    }

    public record UnconstrainedConfig(String valueType) implements FeatureConstraintConfig {
        public UnconstrainedConfig {
            if (valueType == null || valueType.isBlank()) {
                throw new IllegalArgumentException("Value type must be defined");
            }
        }
    }

    /**
     * Command to add a product type and its identification, unit, tracking strategy, features, and static metadata.
     *
     * @param productIdType identifier type, e.g. {@code UUID}, {@code ISBN}, or {@code GTIN}
     * @param productId identifier value parsed according to {@code productIdType}
     * @param name product type name
     * @param description product type description
     * @param unit unit of measurement, e.g. {@code pcs}, {@code kg}, or {@code m²}
     * @param trackingStrategy tracking strategy, e.g. {@code IDENTICAL}, {@code INDIVIDUALLY_TRACKED}, or
     *     {@code BATCH_TRACKED}
     * @param mandatoryFeatures required features, or {@code null} if none
     * @param optionalFeatures optional features, or {@code null} if none
     * @param metadata static product properties, or {@code null} if none
     */
    public record DefineProductType(
            String productIdType,
            String productId,
            String name,
            String description,
            String unit,
            String trackingStrategy,
            @Nullable Set<MandatoryFeature> mandatoryFeatures,
            @Nullable Set<OptionalFeature> optionalFeatures,
            @Nullable Map<String, String> metadata) {
        public DefineProductType {
            if (productIdType == null || productIdType.isBlank()) {
                throw new IllegalArgumentException("Product ID type must be defined");
            }
            if (productId == null || productId.isBlank()) {
                throw new IllegalArgumentException("Product ID must be defined");
            }
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Name must be defined");
            }
            if (description == null || description.isBlank()) {
                throw new IllegalArgumentException("Description must be defined");
            }
            if (unit == null || unit.isBlank()) {
                throw new IllegalArgumentException("Unit must be defined");
            }
            if (trackingStrategy == null || trackingStrategy.isBlank()) {
                throw new IllegalArgumentException("Tracking strategy must be defined");
            }
        }
    }

    /** Mandatory feature definition. */
    public record MandatoryFeature(String name, FeatureConstraintConfig constraint) {
        public MandatoryFeature {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Feature name must be defined");
            }
            if (constraint == null) {
                throw new IllegalArgumentException("Constraint must be defined");
            }
        }
    }

    /** Optional feature definition. */
    public record OptionalFeature(String name, FeatureConstraintConfig constraint) {
        public OptionalFeature {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Feature name must be defined");
            }
            if (constraint == null) {
                throw new IllegalArgumentException("Constraint must be defined");
            }
        }
    }

    /**
     * Adds a product type to the catalog.
     *
     * @param productTypeId product type identifier
     * @param displayName catalog display name
     * @param description catalog description
     * @param categories catalog categories
     * @param availableFrom first availability date, or {@code null} if unbounded
     * @param availableUntil last availability date, or {@code null} if unbounded
     * @param metadata catalog metadata
     */
    public record AddToOffer(
            String productTypeId,
            String displayName,
            String description,
            Set<String> categories,
            @Nullable LocalDate availableFrom,
            @Nullable LocalDate availableUntil,
            Map<String, String> metadata) {
        public AddToOffer {
            if (productTypeId == null || productTypeId.isBlank()) {
                throw new IllegalArgumentException("Product type ID must be defined");
            }
            if (displayName == null || displayName.isBlank()) {
                throw new IllegalArgumentException("Display name must be defined");
            }
            if (description == null || description.isBlank()) {
                throw new IllegalArgumentException("Description must be defined");
            }
        }
    }

    /** Discontinues a catalog entry on the given date. */
    public record DiscontinueProduct(String catalogEntryId, LocalDate discontinuationDate) {
        public DiscontinueProduct {
            if (catalogEntryId == null || catalogEntryId.isBlank()) {
                throw new IllegalArgumentException("Catalog entry ID must be defined");
            }
            if (discontinuationDate == null) {
                throw new IllegalArgumentException("Discontinuation date must be defined");
            }
        }
    }

    /** Replaces a catalog entry's metadata. */
    public record UpdateMetadata(String catalogEntryId, Map<String, String> metadata) {
        public UpdateMetadata {
            if (catalogEntryId == null || catalogEntryId.isBlank()) {
                throw new IllegalArgumentException("Catalog entry ID must be defined");
            }
            if (metadata == null) {
                throw new IllegalArgumentException("Metadata must be defined");
            }
        }
    }

    /**
     * Command to define a new package type and its identification, unit, tracking strategy, selection rules, and static
     * metadata.
     *
     * @param productIdType identifier type, e.g. {@code UUID}, {@code ISBN}, or {@code GTIN}
     * @param productId identifier value parsed according to {@code productIdType}
     * @param name package type name
     * @param description package type description
     * @param unit unit of measurement, e.g. {@code pcs} or {@code kg}
     * @param trackingStrategy tracking strategy, e.g. {@code IDENTICAL}, {@code INDIVIDUALLY_TRACKED}, or
     *     {@code BATCH_TRACKED}
     * @param selectionRules rules used to select this package type
     * @param metadata static package properties, or {@code null} if none
     */
    public record DefinePackageType(
            String productIdType,
            String productId,
            String name,
            String description,
            String unit,
            String trackingStrategy,
            Set<SelectionRuleConfig> selectionRules,
            @Nullable Map<String, String> metadata) {
        public DefinePackageType {
            if (productIdType == null || productIdType.isBlank()) {
                throw new IllegalArgumentException("Product ID type must be defined");
            }
            if (productId == null || productId.isBlank()) {
                throw new IllegalArgumentException("Product ID must be defined");
            }
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Name must be defined");
            }
            if (description == null || description.isBlank()) {
                throw new IllegalArgumentException("Description must be defined");
            }
            if (unit == null || unit.isBlank()) {
                throw new IllegalArgumentException("Unit must be defined");
            }
            if (trackingStrategy == null || trackingStrategy.isBlank()) {
                throw new IllegalArgumentException("Tracking strategy must be defined");
            }
            if (selectionRules == null || selectionRules.isEmpty()) {
                throw new IllegalArgumentException("Selection rules must be defined");
            }
        }
    }

    /** Configuration for a selection rule. */
    public sealed interface SelectionRuleConfig
            permits IsSubsetOfConfig,
                    SingleConfig,
                    OptionalConfig,
                    RequiredConfig,
                    AndRuleConfig,
                    OrRuleConfig,
                    NotRuleConfig,
                    IfThenRuleConfig {}

    /**
     * Selects between {@code min} and {@code max} products from the given set.
     *
     * @param productIds identifiers of products eligible for selection
     * @param min minimum number of products to select
     * @param max maximum number of products to select
     */
    public record IsSubsetOfConfig(Set<String> productIds, int min, int max) implements SelectionRuleConfig {
        public IsSubsetOfConfig {
            if (productIds == null || productIds.isEmpty()) {
                throw new IllegalArgumentException("Product IDs must not be empty");
            }
            if (min < 0) {
                throw new IllegalArgumentException("Min must be >= 0");
            }
            if (max < min) {
                throw new IllegalArgumentException("Max must be >= min");
            }
        }
    }

    /**
     * Selects exactly one product from the given set.
     *
     * @param productIds identifiers of products eligible for selection
     */
    public record SingleConfig(Set<String> productIds) implements SelectionRuleConfig {
        public SingleConfig {
            if (productIds == null || productIds.isEmpty()) {
                throw new IllegalArgumentException("Product IDs must not be empty");
            }
        }
    }

    /**
     * Allows at most one product from the given set.
     *
     * @param productIds identifiers of products eligible for selection
     */
    public record OptionalConfig(Set<String> productIds) implements SelectionRuleConfig {
        public OptionalConfig {
            if (productIds == null || productIds.isEmpty()) {
                throw new IllegalArgumentException("Product IDs must not be empty");
            }
        }
    }

    /**
     * Requires at least one product from the given set.
     *
     * @param productIds identifiers of products eligible for selection
     */
    public record RequiredConfig(Set<String> productIds) implements SelectionRuleConfig {
        public RequiredConfig {
            if (productIds == null || productIds.isEmpty()) {
                throw new IllegalArgumentException("Product IDs must not be empty");
            }
        }
    }

    /** Requires all nested rules to be satisfied. */
    public record AndRuleConfig(Set<SelectionRuleConfig> rules) implements SelectionRuleConfig {
        public AndRuleConfig {
            if (rules == null || rules.isEmpty()) {
                throw new IllegalArgumentException("Rules must not be empty");
            }
        }
    }

    /** Requires at least one nested rule to be satisfied. */
    public record OrRuleConfig(Set<SelectionRuleConfig> rules) implements SelectionRuleConfig {
        public OrRuleConfig {
            if (rules == null || rules.isEmpty()) {
                throw new IllegalArgumentException("Rules must not be empty");
            }
        }
    }

    /** Inverts the nested rule. */
    public record NotRuleConfig(SelectionRuleConfig rule) implements SelectionRuleConfig {
        public NotRuleConfig {
            if (rule == null) {
                throw new IllegalArgumentException("Rule must be defined");
            }
        }
    }

    /** Requires all consequent rules when the condition is satisfied. */
    public record IfThenRuleConfig(SelectionRuleConfig condition, Set<SelectionRuleConfig> thenRules)
            implements SelectionRuleConfig {
        public IfThenRuleConfig {
            if (condition == null) {
                throw new IllegalArgumentException("Condition must be defined");
            }
            if (thenRules == null || thenRules.isEmpty()) {
                throw new IllegalArgumentException("Then rules must not be empty");
            }
        }
    }

    /**
     * Command to create a product instance.
     *
     * @param productTypeId identifier of the product type this instance belongs to
     * @param serialNumber optional serial number
     * @param batchId optional batch identifier
     * @param quantity quantity value, e.g. {@code 1}, {@code 5.5}, or {@code 3.2}
     * @param unit unit of measurement, e.g. {@code pcs}, {@code kg}, or {@code l}
     * @param features features assigned to this product instance
     */
    public record CreateProductInstance(
            String productTypeId,
            @Nullable String serialNumber,
            @Nullable String batchId,
            String quantity,
            String unit,
            Set<FeatureInstanceConfig> features) {
        public CreateProductInstance {
            if (productTypeId == null || productTypeId.isBlank()) {
                throw new IllegalArgumentException("Product type ID must be defined");
            }
            if (quantity == null || quantity.isBlank()) {
                throw new IllegalArgumentException("Quantity must be defined");
            }
            if (unit == null || unit.isBlank()) {
                throw new IllegalArgumentException("Unit must be defined");
            }
        }
    }

    /** Configures a product feature value. */
    public record FeatureInstanceConfig(String featureName, String value) {
        public FeatureInstanceConfig {
            if (featureName == null || featureName.isBlank()) {
                throw new IllegalArgumentException("Feature name must be defined");
            }
            if (value == null) {
                throw new IllegalArgumentException("Feature value must be defined");
            }
        }
    }

    /**
     * Command to create a package instance.
     *
     * @param packageTypeId identifier of the package type this instance belongs to
     * @param serialNumber optional serial number
     * @param batchId optional batch identifier
     * @param selection selected product or package instances
     */
    public record CreatePackageInstance(
            String packageTypeId,
            @Nullable String serialNumber,
            @Nullable String batchId,
            Set<SelectedInstanceConfig> selection) {
        public CreatePackageInstance {
            if (packageTypeId == null || packageTypeId.isBlank()) {
                throw new IllegalArgumentException("Package type ID must be defined");
            }
            if (selection == null || selection.isEmpty()) {
                throw new IllegalArgumentException("Selection must not be empty");
            }
        }
    }

    /**
     * Defines an instance selected for inclusion in a package.
     *
     * @param instanceId identifier of the selected product or package instance
     * @param quantity number of selected instances
     */
    public record SelectedInstanceConfig(String instanceId, int quantity) {
        public SelectedInstanceConfig {
            if (instanceId == null || instanceId.isBlank()) {
                throw new IllegalArgumentException("Instance ID must be defined");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be > 0");
            }
        }
    }
}
