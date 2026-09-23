---
name: feature
description: Orchestrate issue-driven feature implementation in this repository using interactive planning, bounded Herdr scouts, Matt Pocock's domain-modeling/TDD disciplines, GPT-6 Sol implementation, and independent Standards + Spec review. Use when implementing a GitHub issue, feature request, or other bounded change that should be planned with the user before autonomous implementation.
---

# Feature workflow

Run this workflow from the main interactive session.

The parent owns decisions and orchestration.
Subagents gather evidence, implement approved work, and review it.
Never delegate product or domain decisions to a subagent.

## 1. Resolve the task

Obtain the complete task before planning.

If the input is a GitHub issue URL or issue number, fetch it from the main
session using `gh issue view`, including comments when they contain decisions.

Do not pass an unresolved GitHub URL to a child and assume it can fetch it.

If the task cannot be retrieved, ask the user for its text.

Record:

- task source
- requirements
- acceptance criteria
- constraints
- relevant comments or prior decisions

## 2. Establish the baseline

Before any implementation:

- inspect `git status`
- record the current HEAD as `BASE_SHA`
- identify unrelated existing changes

Do not overwrite, revert, or absorb unrelated user changes.
If existing changes make the implementation boundary ambiguous, ask before proceeding.

## 3. Investigate before asking

Load and apply the installed `grilling`, `grill-with-docs`, and
`domain-modeling` disciplines.

For questions answerable from the codebase, do not ask the user first.
Spawn bounded `scout` agents instead.

Use at most three scouts concurrently.
Give every scout one concrete question.

Good examples:

- "Map the current Settlement creation flow and identify its public seam."
- "Find event-store/versioning conventions already used in this repository."
- "Find tests closest to the behaviour requested by this issue."

Do not ask a scout "understand the whole repository".

## 4. Plan interactively

The main session owns planning.

Use the grilling discipline:

- one unresolved decision at a time
- give a recommended answer with the question
- use scout evidence where available
- use domain-modeling when terminology or aggregate boundaries are changing
- preserve relevant existing ADR decisions
- avoid implementation detail until it affects an important design decision

Do not implement while material decisions remain unresolved.

When the decision frontier is exhausted, present a compact implementation plan
and explicitly ask the user to approve or amend it.

Do not continue to implementation without approval.

## 5. Implement

After approval, spawn one `implementer` agent.

Give it a self-contained task containing:

- complete issue/spec text
- BASE_SHA
- approved decisions
- relevant scout findings
- acceptance criteria
- explicit scope exclusions

Do not make the implementer rediscover decisions already made.

The implementer owns coding, TDD, verification, and its implementation commit.
It must not push or merge.

The subagent is asynchronous. Do not busy-wait.
Continue normally and process its result when it returns.

## 6. Review independently

After implementation, load `code-review` as the review rubric.

Adapt its two-axis review to Herdr:

- do not use its generic Agent-tool orchestration
- spawn `standards-reviewer`
- spawn `spec-reviewer`
- launch both concurrently

Give both reviewers:

- BASE_SHA
- complete issue/spec
- approved decisions

The Standards reviewer owns repository conventions and code quality.
The Spec reviewer owns fidelity to the requested behaviour.

Neither reviewer may modify files.

## 7. Resolve findings

Combine duplicate findings but keep disagreements visible.

If there are blocking or important findings:

1. resume the original implementer session with the actionable findings
2. ask it to fix only those findings and rerun relevant verification
3. rerun both independent reviewers

Do not let reviewers fix their own findings.

Use at most two automatic fix/re-review rounds.
After that, return unresolved findings to the user for a decision.

Minor optional improvements do not block completion unless the user requests them.

## 8. Finish

Report:

- what changed
- important design decisions
- verification/tests run
- Standards review result
- Spec review result
- remaining risks or follow-ups

Do not push or merge unless explicitly requested.

## Operational rules

- Prefer the configured named Herdr agents over anonymous subagents.
- Keep the main session interactive throughout.
- Use `subagents_list` when execution status is needed.
- Do not invoke the bundled `/plan` workflow.
- Do not create managed worktrees unless explicitly requested.
- Do not run infrastructure mutation commands.
