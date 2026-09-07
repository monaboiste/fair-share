package com.softwarearchetypes.product;

public sealed interface ProductIdentifier
        permits TextProductIdentifier, GtinProductIdentifier, Isbn10ProductIdentifier, UuidProductIdentifier {

    String type();

    @Override
    String toString();

    static ProductIdentifier gtin(String value) {
        return GtinProductIdentifier.of(value);
    }

    static ProductIdentifier isbn10(String value) {
        return Isbn10ProductIdentifier.of(value);
    }

    static ProductIdentifier uuid() {
        return UuidProductIdentifier.random();
    }

    static ProductIdentifier of(String value) {
        return new TextProductIdentifier(value);
    }
}
