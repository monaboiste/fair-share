package com.softwarearchetypes.product;

import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/** A named set of products available for selection in a package. */
public class ProductSet {
    private final String name;
    private final Set<ProductIdentifier> products;

    ProductSet(String name, Set<ProductIdentifier> products) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("ProductSet name must be defined");
        }
        if (products == null || products.isEmpty()) {
            throw new IllegalArgumentException("ProductSet must contain at least one product");
        }
        this.name = name;
        this.products = Set.copyOf(products);
    }

    public static ProductSet singleOf(String name, ProductIdentifier id) {
        return new ProductSet(name, Set.of(id));
    }

    public static ProductSet of(String name, ProductIdentifier... ids) {
        return new ProductSet(name, Arrays.stream(ids).collect(toSet()));
    }

    public String name() {
        return name;
    }

    public Set<ProductIdentifier> products() {
        return products;
    }

    public boolean contains(ProductIdentifier productId) {
        return products.contains(productId);
    }

    @Override
    @NonNull public String toString() {
        return "ProductSet{name='%s', products=%s}".formatted(name, products);
    }
}
