package com.softwarearchetypes.rules.core.selection;

import com.softwarearchetypes.rules.core.ChainModifier;
import java.util.List;

public final class RuleSelector {

    private RuleSelector() {}

    public static <C, T> ChainModifier<T> select(C context, List<CandidateRule<C, T>> rules) {
        ChainModifier<T> chain = new ChainModifier<>();
        for (CandidateRule<C, T> rule : rules) {
            if (rule.appliesTo().test(context)) {
                chain.add(rule.modifier());
            }
        }
        return chain;
    }
}
