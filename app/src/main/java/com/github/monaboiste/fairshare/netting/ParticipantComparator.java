package com.github.monaboiste.fairshare.netting;

import java.util.Comparator;

/**
 * Orders participants for deterministic netting output.
 *
 * <p>Participant identity is independent of any non-unique display name, so implementations order by identity and must
 * define a total order: {@code compare} returns zero only for the same participant. A partial order lets tie-breaking
 * fall back to iteration order and breaks the determinism guarantee.
 *
 * @param <P> participant identity type
 */
@FunctionalInterface
public interface ParticipantComparator<P> extends Comparator<P> {}
