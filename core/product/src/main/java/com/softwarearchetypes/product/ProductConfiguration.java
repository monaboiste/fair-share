package com.softwarearchetypes.product;

record ProductConfiguration(
        ProductFacade productFacade,
        ProductRelationshipsFacade productRelationshipsFacade,
        ProductTypeRepository productTypeRepository,
        ProductCatalog productCatalog,
        CatalogEntryRepository catalogEntryRepository) {

    public static ProductConfiguration inMemory() {
        InMemoryProductTypeRepository productTypeRepository = new InMemoryProductTypeRepository();
        ProductFacade facade = new ProductFacade(productTypeRepository);

        InMemoryProductRelationshipRepository productRelationshipRepository =
                new InMemoryProductRelationshipRepository();
        ProductRelationshipFactory productRelationshipFactory =
                new ProductRelationshipFactory(ProductRelationshipId::newOne);
        ProductRelationshipsFacade productRelationshipsFacade = new ProductRelationshipsFacade(
                productRelationshipFactory, productRelationshipRepository, productTypeRepository);

        InMemoryCatalogEntryRepository catalogEntryRepository = new InMemoryCatalogEntryRepository();
        ProductCatalog productCatalog = new ProductCatalog(catalogEntryRepository, productTypeRepository);

        return new ProductConfiguration(
                facade, productRelationshipsFacade, productTypeRepository, productCatalog, catalogEntryRepository);
    }
}
