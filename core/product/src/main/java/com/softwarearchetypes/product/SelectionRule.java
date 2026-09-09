package com.softwarearchetypes.product;

import java.util.Arrays;
import java.util.List;

/**
 * Defines constraints on how products from a package may be selected.
 *
 * <p>Rules can restrict selection quantities and be composed using AND, OR, NOT, and conditional logic.
 */
public interface SelectionRule {

    /**
     * Returns whether the given selection satisfies this rule.
     *
     * @param selection products selected from the package
     * @return {@code true} if the selection satisfies this rule
     */
    boolean isSatisfiedBy(List<SelectedProduct> selection);

    /** Requires selecting between {@code min} and {@code max} products from the given set. */
    static SelectionRule isSubsetOf(ProductSet sourceSet, int min, int max) {
        return new IsSubsetOf(sourceSet, min, max);
    }

    /** Requires selecting exactly one product from the given set. */
    static SelectionRule single(ProductSet sourceSet) {
        return new IsSubsetOf(sourceSet, 1, 1);
    }

    /** Allows selecting at most one product from the given set. */
    static SelectionRule optional(ProductSet sourceSet) {
        return new IsSubsetOf(sourceSet, 0, 1);
    }

    /** Requires selecting at least one product from the given set. */
    static SelectionRule required(ProductSet sourceSet) {
        return new IsSubsetOf(sourceSet, 1, Integer.MAX_VALUE);
    }

    /** Creates a rule requiring all given rules to be satisfied. */
    static SelectionRule and(SelectionRule... rules) {
        return new AndRule(Arrays.asList(rules));
    }

    /** Creates a rule requiring at least one of the given rules to be satisfied. */
    static SelectionRule or(SelectionRule... rules) {
        return new OrRule(Arrays.asList(rules));
    }

    /** Creates a conditional rule requiring all {@code thenRules} when the condition is satisfied. */
    static SelectionRule ifThen(SelectionRule condition, SelectionRule... thenRules) {
        return new ConditionalRule(condition, Arrays.asList(thenRules));
    }

    /** Creates a rule satisfied when the given rule is not satisfied. */
    static SelectionRule not(SelectionRule rule) {
        return new NotRule(rule);
    }

    private static List<SelectedProduct> relevantSelection(SelectionRule rule, List<SelectedProduct> selection) {
        if (rule instanceof IsSubsetOf subset) {
            return selection.stream()
                    .filter(s -> subset.sourceSet().contains(s.productId()))
                    .toList();
        }
        return selection;
    }

    /** Restricts the total selected quantity to a range within a product set. */
    record IsSubsetOf(ProductSet sourceSet, int min, int max) implements SelectionRule {

        public IsSubsetOf {
            if (min < 0) {
                throw new IllegalArgumentException("Min must be >= 0");
            }
            if (max < min) {
                throw new IllegalArgumentException("Max must be >= min");
            }
        }

        @Override
        public boolean isSatisfiedBy(List<SelectedProduct> selection) {
            if (selection.stream().anyMatch(s -> !sourceSet.contains(s.productId()))) {
                return false;
            }

            long count = selection.stream().mapToInt(SelectedProduct::quantity).sum();
            return count >= min && count <= max;
        }

        @Override
        public String toString() {
            return "IsSubsetOf{set='%s', min=%d, max=%d}".formatted(sourceSet.name(), min, max);
        }
    }

    /** Requires all contained rules to be satisfied. */
    record AndRule(List<SelectionRule> rules) implements SelectionRule {

        public AndRule {
            if (rules.isEmpty()) {
                throw new IllegalArgumentException("Rules cannot be empty");
            }
        }

        @Override
        public boolean isSatisfiedBy(List<SelectedProduct> selection) {
            return rules.stream().allMatch(r -> r.isSatisfiedBy(relevantSelection(r, selection)));
        }

        @Override
        public String toString() {
            return "AND(%d rules)".formatted(rules.size());
        }
    }

    /** Requires at least one contained rule to be satisfied. */
    record OrRule(List<SelectionRule> rules) implements SelectionRule {

        public OrRule {
            if (rules.isEmpty()) {
                throw new IllegalArgumentException("Rules cannot be empty");
            }
        }

        @Override
        public boolean isSatisfiedBy(List<SelectedProduct> selection) {
            return rules.stream().anyMatch(r -> r.isSatisfiedBy(relevantSelection(r, selection)));
        }

        @Override
        public String toString() {
            return "OR(%d rules)".formatted(rules.size());
        }
    }

    /** Is satisfied when the contained rule is not satisfied. */
    record NotRule(SelectionRule rule) implements SelectionRule {

        @Override
        public boolean isSatisfiedBy(List<SelectedProduct> selection) {
            return !rule.isSatisfiedBy(relevantSelection(rule, selection));
        }

        @Override
        public String toString() {
            return "NOT(%s)".formatted(rule);
        }
    }

    /**
     * Requires all consequent rules when the condition is satisfied.
     *
     * <p>If the condition is not satisfied, this rule is satisfied.
     */
    record ConditionalRule(SelectionRule condition, List<SelectionRule> thenRules) implements SelectionRule {

        public ConditionalRule {
            if (thenRules.isEmpty()) {
                throw new IllegalArgumentException("Then rules cannot be empty");
            }
        }

        @Override
        public boolean isSatisfiedBy(List<SelectedProduct> selection) {
            if (condition.isSatisfiedBy(relevantSelection(condition, selection))) {
                return thenRules.stream().allMatch(r -> r.isSatisfiedBy(relevantSelection(r, selection)));
            }
            return true;
        }

        @Override
        public String toString() {
            return "IF(%s) THEN(%d rules)".formatted(condition, thenRules.size());
        }
    }
}
