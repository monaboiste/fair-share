package com.softwarearchetypes.graphs.fixture.userjourney

record Product(ProductType type, Map<String, Object> attributes) {

    enum ProductType {
        NEW_LOAN,
        PENALTY,
        DISCOUNT
    }

    static Product of(ProductType type) {
        return new Product(type, new HashMap<>())
    }

    static Product of(ProductType type, Map<String, Object> attributes) {
        return new Product(type, new HashMap<>(attributes))
    }

    static Product newLoan() {
        return new Product(ProductType.NEW_LOAN, new HashMap<>())
    }

    static Product penalty() {
        return new Product(ProductType.PENALTY, new HashMap<>())
    }

    static Product discount(int percentage) {
        Map<String, Object> attributes = new HashMap<>()
        attributes.put("percentage", percentage)
        return new Product(ProductType.DISCOUNT, attributes)
    }
}
