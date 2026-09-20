/**
 * Nets obligations between participants into a deterministic set of proposed repayments.
 *
 * <p>Participants are vertices and obligations are directed, {@code Money}-weighted edges. {@link Netting} folds
 * obligations valid at an as-of time into per-participant balances and produces {@link ProposedRepayments} preserving
 * those balances while eliminating loops, zero edges, parallel edges, and cycles.
 */
@NullMarked
package com.github.monaboiste.fairshare.netting;

import org.jspecify.annotations.NullMarked;
