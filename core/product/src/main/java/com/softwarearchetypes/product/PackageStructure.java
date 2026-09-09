package com.softwarearchetypes.product;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Defines selectable product sets and the rules governing their combination. */
public record PackageStructure(Map<String, ProductSet> productSets, List<SelectionRule> selectionRules) {
    public PackageStructure(Map<String, ProductSet> productSets, List<SelectionRule> selectionRules) {
        if (productSets.isEmpty()) {
            throw new IllegalArgumentException("ProductSets must be defined");
        }
        if (selectionRules.isEmpty()) {
            throw new IllegalArgumentException("Selection rules must be defined");
        }
        this.productSets = Map.copyOf(productSets);
        this.selectionRules = List.copyOf(selectionRules);
    }

    /** Validates whether a product selection satisfies every package rule. */
    public PackageValidationResult validate(List<SelectedProduct> selection) {
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < selectionRules.size(); i++) {
            SelectionRule rule = selectionRules.get(i);
            List<SelectedProduct> ruleSelection = selection;
            if (rule instanceof SelectionRule.IsSubsetOf subset) {
                ruleSelection = selection.stream()
                        .filter(selected -> subset.sourceSet().contains(selected.productId()))
                        .toList();
            }
            if (!rule.isSatisfiedBy(ruleSelection)) {
                errors.add("Rule %d not satisfied: %s".formatted(i + 1, rule));
            }
        }

        return errors.isEmpty() ? PackageValidationResult.success() : PackageValidationResult.failure(errors);
    }

    @Override
    public String toString() {
        return "PackageStructure{sets=%d, rules=%d}".formatted(productSets.size(), selectionRules.size());
    }
}
