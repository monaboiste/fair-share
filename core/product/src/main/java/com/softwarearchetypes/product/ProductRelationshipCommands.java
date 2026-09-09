package com.softwarearchetypes.product;

/** Commands accepted by {@link ProductRelationshipsFacade}. */
public final class ProductRelationshipCommands {

    private ProductRelationshipCommands() {}

    /**
     * Defines a directed relationship between two products.
     *
     * @param fromProductId source product identifier
     * @param toProductId target product identifier
     * @param relationshipType type of the relationship
     */
    public record DefineRelationship(
            ProductIdentifier fromProductId, ProductIdentifier toProductId, ProductRelationshipType relationshipType) {}

    /** Removes a product relationship. */
    public record RemoveRelationship(ProductRelationshipId relationshipId) {}
}
