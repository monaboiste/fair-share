package com.softwarearchetypes.product;

record TextProductIdentifier(String value) implements ProductIdentifier {

    TextProductIdentifier {
        if (value.isBlank()) {
            throw new IllegalArgumentException("TextProductIdentifier value cannot be null or blank");
        }
    }

    @Override
    public String type() {
        return "TEXT";
    }

    @Override
    public String toString() {
        return value;
    }
}
