package com.softwarearchetypes.product;

/** A directed relationship between two products. */
public enum ProductRelationshipType {
    UPGRADABLE_TO,
    SUBSTITUTED_BY,
    REPLACED_BY,
    COMPLEMENTED_BY,
    COMPATIBLE_WITH,
    INCOMPATIBLE_WITH
}
