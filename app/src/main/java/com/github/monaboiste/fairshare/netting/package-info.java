/**
 * Nets obligations between participants into a deterministic set of proposed repayments.
 *
 * <p>Participants are vertices and obligations are directed, {@code Money}-weighted edges.
 * {@link com.github.monaboiste.fairshare.netting.Netting} folds obligations into per-participant balances and produces
 * {@link com.github.monaboiste.fairshare.netting.ProposedRepayments} preserving those balances while eliminating loops,
 * zero edges, parallel edges, and cycles.
 */
package com.github.monaboiste.fairshare.netting;
