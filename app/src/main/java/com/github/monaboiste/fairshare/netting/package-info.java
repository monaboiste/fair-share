/**
 * Nets obligations between participants into a minimal, deterministic set of proposed repayments.
 *
 * <p>Participants are vertices; obligations are directed, {@code Money}-weighted edges.
 * {@link com.github.monaboiste.fairshare.netting.Netting} folds the obligations valid at an as-of time into
 * per-participant balances, then greedily settles the largest debtor against the largest creditor to produce a
 * {@link com.github.monaboiste.fairshare.netting.ProposedRepaymentGraph} with no loops, zero edges, parallel edges, or
 * cycles, and at most {@code max(0, unbalanced - 1)} edges.
 *
 * <p>Worked example - four participants in one settlement currency:
 *
 * <pre>
 * Obligation Graph   (edge  A ──n──▶ B  means  A owes B  n)
 *
 *           30
 *   Ada ─────────▶ Bob
 *    ▲              │
 *  5 │              │ 10
 *    │              ▼
 *   Dan ◀───────── Cid
 *           5
 *
 * Net balances (in - out):   Ada -25 (debtor)   Bob +20   Cid +5   Dan 0 (settled)
 *
 * Proposed Repayments   (greedy: largest debtor ▶ largest creditor)
 *
 *           20
 *   Ada ─────────▶ Bob
 *    │
 *    └─────5─────▶ Cid          Dan: nothing
 *
 * 2 edges ≤ max(0, unbalanced - 1) = 2  ·  no loops, cycles, or parallel edges.
 * </pre>
 *
 * <p>The four-obligation cycle collapses to two acyclic repayments and every participant balance is preserved.
 */
@NullMarked
package com.github.monaboiste.fairshare.netting;

import org.jspecify.annotations.NullMarked;
