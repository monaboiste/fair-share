package com.github.monaboiste.fairshare.rules.core.selection;

import com.github.monaboiste.fairshare.rules.core.Modifier;
import java.util.function.Predicate;

public record CandidateRule<C, T>(Modifier<T> modifier, Predicate<C> appliesTo) {}
