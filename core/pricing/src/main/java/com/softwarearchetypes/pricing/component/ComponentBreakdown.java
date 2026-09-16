package com.softwarearchetypes.pricing.component;

import com.softwarearchetypes.pricing.calculation.PricingResult;
import com.softwarearchetypes.quantity.money.Money;
import java.util.List;

/**
 * Breakdown showing individual component contributions to total price. Forms a tree structure mirroring the component
 * hierarchy.
 */
public record ComponentBreakdown(String name, PricingResult result, List<ComponentBreakdown> children) {

    public ComponentBreakdown {
        children = List.copyOf(children);
    }

    public ComponentBreakdown(String name, PricingResult result) {
        this(name, result, List.of());
    }

    /**
     * Returns the total amount for this breakdown. This is the contribution of this component (which for composite
     * components is already the sum of all children).
     */
    public Money total() {
        return result.money();
    }

    /** Format breakdown as indented text for display. */
    public String format() {
        return formatWithIndent(0);
    }

    private String formatWithIndent(int level) {
        String indent = "  ".repeat(level);
        StringBuilder sb = new StringBuilder();

        sb.append(indent).append(name).append(": ").append(result.money());

        if (!children.isEmpty()) {
            sb.append("%n".formatted());
            for (int i = 0; i < children.size(); i++) {
                sb.append(children.get(i).formatWithIndent(level + 1));
                if (i < children.size() - 1) {
                    sb.append("%n".formatted());
                }
            }
        }

        return sb.toString();
    }
}
