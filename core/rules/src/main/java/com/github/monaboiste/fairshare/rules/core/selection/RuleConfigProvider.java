package com.github.monaboiste.fairshare.rules.core.selection;

import java.util.List;

public interface RuleConfigProvider<C, T> {

    List<CandidateRule<C, T>> load();
}
