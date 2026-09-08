package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money

public class ComponentBreakdownAssert {
    private final ComponentBreakdown actual
    private ComponentBreakdownAssert(ComponentBreakdown actual) { this.actual = actual }
    static ComponentBreakdownAssert assertThat(ComponentBreakdown actual) { assert actual != null; return new ComponentBreakdownAssert(actual) }
    ComponentBreakdownAssert hasName(String expected) { assert actual.name() == expected; return this }
    ComponentBreakdownAssert hasTotal(Money expected) { assert actual.total() == expected; return this }
    ComponentBreakdownAssert hasChildrenCount(int expected) { assert actual.children().size() == expected; return this }
    ComponentBreakdownAssert hasNoChildren() { return hasChildrenCount(0) }
    ComponentBreakdownAssert child(int index) { assert index < actual.children().size(); return new ComponentBreakdownAssert(actual.children().get(index)) }
    ComponentBreakdownAssert child(String name) {
        ComponentBreakdown child = actual.children().find { it.name() == name }
        assert child != null
        return new ComponentBreakdownAssert(child)
    }
    ComponentBreakdown get() { return actual }
}
