package com.github.monaboiste.fairshare.product;

public interface ProductRelationshipDefiningPolicy {

    boolean canDefineFor(ProductIdentifier from, ProductIdentifier to, ProductRelationshipType type);
}

final class AlwaysAllowProductRelationshipDefiningPolicy implements ProductRelationshipDefiningPolicy {

    @Override
    public boolean canDefineFor(ProductIdentifier from, ProductIdentifier to, ProductRelationshipType type) {
        return true;
    }
}
