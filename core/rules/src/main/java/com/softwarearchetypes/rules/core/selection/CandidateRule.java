package com.softwarearchetypes.rules.core.selection;

import com.softwarearchetypes.rules.core.Modifier;
import java.util.function.Predicate;

public record CandidateRule<C, T>(Modifier<T> modifier, Predicate<C> appliesTo) {}
