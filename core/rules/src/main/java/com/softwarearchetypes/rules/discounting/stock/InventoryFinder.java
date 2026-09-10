package com.softwarearchetypes.rules.discounting.stock;

import java.util.List;

public interface InventoryFinder {
    List<ProductStock> findOverstockedProducts();
}
