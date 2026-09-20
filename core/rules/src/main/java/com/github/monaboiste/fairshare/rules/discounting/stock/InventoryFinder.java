package com.github.monaboiste.fairshare.rules.discounting.stock;

import java.util.List;

public interface InventoryFinder {
    List<ProductStock> findOverstockedProducts();
}
