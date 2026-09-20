package com.github.monaboiste.fairshare.rules.discounting.stock;

import com.github.monaboiste.fairshare.quantity.Quantity;
import java.util.UUID;

public record ProductStock(UUID productId, Quantity quantity, int daysInStock) {}
