package com.softwarearchetypes.product;

/** Defines how product instances are identified and grouped for tracking. */
public enum ProductTrackingStrategy {

    /** A one-of-a-kind product. */
    UNIQUE,

    /** Each instance has an individual identifier. */
    INDIVIDUALLY_TRACKED,

    /** Instances are tracked by production batch. */
    BATCH_TRACKED,

    /** Instances have individual identifiers and belong to batches. */
    INDIVIDUALLY_AND_BATCH_TRACKED,

    /** Instances are interchangeable and require no tracking identifier. */
    IDENTICAL;

    /** Returns whether each instance requires an individual identifier. */
    public boolean isTrackedIndividually() {
        return this == UNIQUE || this == INDIVIDUALLY_TRACKED || this == INDIVIDUALLY_AND_BATCH_TRACKED;
    }

    /** Returns whether instances are tracked by batch. */
    public boolean isTrackedByBatch() {
        return this == BATCH_TRACKED || this == INDIVIDUALLY_AND_BATCH_TRACKED;
    }

    /** Returns whether both individual and batch tracking are required. */
    public boolean requiresBothTrackingMethods() {
        return this == INDIVIDUALLY_AND_BATCH_TRACKED;
    }

    /** Returns whether instances are interchangeable. */
    public boolean isInterchangeable() {
        return this == IDENTICAL;
    }
}
