package com.softwarearchetypes.product;

/** A product type or package offered by the product domain. */
public interface Product {
    ProductIdentifier id();

    ProductName name();

    ProductDescription description();

    ProductMetadata metadata();

    ApplicabilityConstraint applicabilityConstraint();

    default boolean isApplicableFor(ApplicabilityContext context) {
        return applicabilityConstraint().isSatisfiedBy(context);
    }

    static ProductBuilder builder(ProductIdentifier id, ProductName name, ProductDescription description) {
        return new ProductBuilder(id, name, description);
    }
}
