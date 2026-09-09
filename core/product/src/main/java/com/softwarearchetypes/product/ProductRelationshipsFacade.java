package com.softwarearchetypes.product;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.product.ProductRelationshipCommands.DefineRelationship;
import com.softwarearchetypes.product.ProductRelationshipCommands.RemoveRelationship;

public class ProductRelationshipsFacade {

    private final ProductRelationshipFactory factory;
    private final ProductRelationshipRepository repository;
    private final ProductTypeRepository productTypeRepository;

    ProductRelationshipsFacade(
            ProductRelationshipFactory factory,
            ProductRelationshipRepository repository,
            ProductTypeRepository productTypeRepository) {
        this.factory = factory;
        this.repository = repository;
        this.productTypeRepository = productTypeRepository;
    }

    /** Creates a facade backed by an in-memory relationship repository. */
    public static ProductRelationshipsFacade create(ProductTypeRepository productTypeRepository) {
        return new ProductRelationshipsFacade(
                new ProductRelationshipFactory(ProductRelationshipId::newOne),
                new InMemoryProductRelationshipRepository(),
                productTypeRepository);
    }

    /** Defines a new relationship between two products. */
    public Result<String, ProductRelationshipId> handle(DefineRelationship command) {
        try {
            var from = command.fromProductId();
            var to = command.toProductId();
            var type = command.relationshipType();

            if (productTypeRepository.findById(from).isEmpty()) {
                return Result.failure("PRODUCT_NOT_FOUND: " + from);
            }
            if (productTypeRepository.findById(to).isEmpty()) {
                return Result.failure("PRODUCT_NOT_FOUND: " + to);
            }

            return factory.defineFor(from, to, type)
                    .peekSuccess(repository::save)
                    .map(ProductRelationship::id);

        } catch (Exception e) {
            return Result.failure(e.getMessage());
        }
    }

    /** Removes an existing relationship. */
    public Result<String, ProductRelationshipId> handle(RemoveRelationship command) {
        try {
            var relationshipId = command.relationshipId();
            repository.delete(relationshipId);
            return Result.success(relationshipId);

        } catch (Exception e) {
            return Result.failure(e.getMessage());
        }
    }
}
