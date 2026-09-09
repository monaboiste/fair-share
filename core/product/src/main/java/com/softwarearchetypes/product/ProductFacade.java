package com.softwarearchetypes.product;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.product.ProductCommands.DefineProductType;
import com.softwarearchetypes.product.ProductCommands.MandatoryFeature;
import com.softwarearchetypes.product.ProductCommands.OptionalFeature;
import com.softwarearchetypes.product.ProductQueries.FindByTrackingStrategyCriteria;
import com.softwarearchetypes.product.ProductQueries.FindProductTypeCriteria;
import com.softwarearchetypes.product.ProductViews.FeatureTypeView;
import com.softwarearchetypes.product.ProductViews.ProductTypeView;
import com.softwarearchetypes.quantity.Unit;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Manages product types through commands and queries. */
public class ProductFacade {

    private final ProductTypeRepository repository;

    public ProductFacade(ProductTypeRepository repository) {
        this.repository = repository;
    }

    public static ProductFacade create() {
        return new ProductFacade(ProductTypeRepository.inMemory());
    }

    /** Defines a product type. */
    public Result<String, ProductIdentifier> handle(DefineProductType command) {
        try {
            var productId = parseProductIdentifier(command.productIdType(), command.productId());
            var name = ProductName.of(command.name());
            var description = ProductDescription.of(command.description());
            var unit = parseUnit(command.unit());
            var trackingStrategy = parseTrackingStrategy(command.trackingStrategy());

            var productBuilder = new ProductBuilder(productId, name, description);

            if (command.metadata() != null) {
                productBuilder.withMetadata(ProductMetadata.of(command.metadata()));
            }

            var typeBuilder = productBuilder.asProductType(unit, trackingStrategy);

            if (command.mandatoryFeatures() != null) {
                for (var feature : command.mandatoryFeatures()) {
                    typeBuilder.withMandatoryFeature(toProductFeatureType(feature.name(), feature));
                }
            }

            if (command.optionalFeatures() != null) {
                for (var feature : command.optionalFeatures()) {
                    typeBuilder.withOptionalFeature(toProductFeatureType(feature.name(), feature));
                }
            }

            var productType = typeBuilder.build();
            repository.save(productType);

            return Result.success(productId);

        } catch (Exception e) {
            return Result.failure(e.getMessage());
        }
    }

    /** Finds a product type by identifier. */
    public Optional<ProductTypeView> findBy(FindProductTypeCriteria criteria) {
        return repository.findByIdValue(criteria.productId()).map(this::toProductTypeView);
    }

    /** Finds product types by tracking strategy. */
    public Set<ProductTypeView> findBy(FindByTrackingStrategyCriteria criteria) {
        var strategy = parseTrackingStrategy(criteria.trackingStrategy());
        return repository.findByTrackingStrategy(strategy).stream()
                .map(this::toProductTypeView)
                .collect(Collectors.toSet());
    }

    private ProductIdentifier parseProductIdentifier(String type, String value) {
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "UUID" -> UuidProductIdentifier.of(value);
            case "ISBN" -> Isbn10ProductIdentifier.of(value);
            case "GTIN" -> GtinProductIdentifier.of(value);
            default ->
                throw new IllegalArgumentException(
                        "Unknown product identifier type: " + type + ". Supported types: UUID, ISBN, GTIN");
        };
    }

    private ProductTrackingStrategy parseTrackingStrategy(String value) {
        return ProductTrackingStrategy.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private Unit parseUnit(String symbol) {
        return switch (symbol.toLowerCase(Locale.ROOT)) {
            case "pcs", "pieces" -> Unit.pieces();
            case "kg", "kilograms" -> Unit.kilograms();
            case "l", "liters" -> Unit.liters();
            case "m", "meters" -> Unit.meters();
            case "m²", "m2", "square meters" -> Unit.squareMeters();
            case "m³", "m3", "cubic meters" -> Unit.cubicMeters();
            case "h", "hours" -> Unit.hours();
            case "min", "minutes" -> Unit.minutes();
            default -> Unit.of(symbol, symbol);
        };
    }

    private ProductFeatureType toProductFeatureType(String name, MandatoryFeature feature) {
        var constraint = toConstraint(feature.constraint());
        return ProductFeatureType.of(name, constraint);
    }

    private ProductFeatureType toProductFeatureType(String name, OptionalFeature feature) {
        var constraint = toConstraint(feature.constraint());
        return ProductFeatureType.of(name, constraint);
    }

    private FeatureValueConstraint toConstraint(ProductCommands.FeatureConstraintConfig config) {
        return switch (config) {
            case ProductCommands.AllowedValuesConfig(var allowedValues) ->
                AllowedValuesConstraint.of(allowedValues.toArray(String[]::new));

            case ProductCommands.NumericRangeConfig(var min, var max) -> NumericRangeConstraint.between(min, max);

            case ProductCommands.DecimalRangeConfig(var min, var max) -> DecimalRangeConstraint.of(min, max);

            case ProductCommands.RegexConfig(var pattern) -> RegexConstraint.of(pattern);

            case ProductCommands.DateRangeConfig(var from, var to) -> DateRangeConstraint.between(from, to);

            case ProductCommands.UnconstrainedConfig(var valueType) ->
                new Unconstrained(FeatureValueType.valueOf(valueType.toUpperCase(Locale.ROOT)));
        };
    }

    private ProductTypeView toProductTypeView(ProductType productType) {
        return new ProductTypeView(
                productType.id().toString(),
                productType.name().value(),
                productType.description().value(),
                productType.preferredUnit().symbol(),
                productType.trackingStrategy().name(),
                toFeatureTypeViews(productType.featureTypes().mandatoryFeatures()),
                toFeatureTypeViews(productType.featureTypes().optionalFeatures()));
    }

    private Set<FeatureTypeView> toFeatureTypeViews(Set<ProductFeatureType> features) {
        return features.stream().map(this::toFeatureTypeView).collect(Collectors.toSet());
    }

    private FeatureTypeView toFeatureTypeView(ProductFeatureType featureType) {
        var constraint = featureType.constraint();
        return new FeatureTypeView(
                featureType.name(),
                constraint.valueType().name(),
                constraint.type(),
                constraintConfigToMap(constraint),
                constraint.desc());
    }

    private Map<String, Object> constraintConfigToMap(FeatureValueConstraint constraint) {
        return switch (constraint) {
            case AllowedValuesConstraint(var allowedValues) -> Map.of("allowedValues", allowedValues);

            case NumericRangeConstraint(var min, var max) -> Map.of("min", min, "max", max);

            case DecimalRangeConstraint(var min, var max) -> Map.of("min", min, "max", max);

            case RegexConstraint(var pattern) -> Map.of("pattern", pattern);

            case DateRangeConstraint(var from, var to) -> Map.of("from", from.toString(), "to", to.toString());

            default -> Map.of();
        };
    }
}
