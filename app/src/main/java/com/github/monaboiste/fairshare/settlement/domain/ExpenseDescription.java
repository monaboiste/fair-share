package com.github.monaboiste.fairshare.settlement.domain;

public record ExpenseDescription(String value) {
    public ExpenseDescription {
        if (value.isBlank()) {
            throw new IllegalArgumentException("Expense description must not be blank");
        }
    }
}
