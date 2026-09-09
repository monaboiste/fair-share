package com.softwarearchetypes.product;

/** A descriptive batch name. */
record BatchName(String value) {

    BatchName {
        if (value.isBlank()) {
            throw new IllegalArgumentException("BatchName cannot be null or blank");
        }
    }

    static BatchName of(String value) {
        return new BatchName(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
