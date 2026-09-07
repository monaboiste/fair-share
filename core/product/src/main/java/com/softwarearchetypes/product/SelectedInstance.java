package com.softwarearchetypes.product;

/** A concrete product or package instance selected for a package. */
record SelectedInstance(Instance instance, int quantity) {

    public SelectedInstance {
        if (instance == null) {
            throw new IllegalArgumentException("Instance must be defined");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be > 0");
        }
    }

    /** Returns the selected instance's product. */
    Product product() {
        return instance.product();
    }

    /** Returns the selected product's identifier. */
    ProductIdentifier productId() {
        return instance.product().id();
    }

    /** Returns the selected instance's identifier. */
    InstanceId instanceId() {
        return instance.id();
    }

    /** Returns the product and quantity used for package validation. */
    SelectedProduct toSelectedProduct() {
        return new SelectedProduct(instance.product().id(), quantity);
    }
}
