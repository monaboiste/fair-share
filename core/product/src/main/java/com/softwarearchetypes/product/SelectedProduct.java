package com.softwarearchetypes.product;

/** A product and quantity selected for package validation. */
public record SelectedProduct(ProductIdentifier productId, int quantity) {
    public SelectedProduct {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be > 0");
        }
    }
}
