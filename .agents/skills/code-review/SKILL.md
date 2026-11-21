---
name: code-review
description: >
  Review code changes for correctness, regressions, contract and invariant violations,
  missing tests, concurrency or data-consistency risks, and maintainability. Use when
  reviewing a diff, pull request, commit, patch, or completed implementation.
---

# Code review

Review for defects and behavioral risk, not personal style. Do not modify code unless explicitly asked.

## Workflow

1. Establish the review target. If none is specified, review both `git diff master...HEAD` and `git diff HEAD`.
2. Establish the intended behavior and scope of the change. Read relevant tests, contracts, and nearby abstractions when
   needed.
3. Review the diff, then inspect the **semantic blast radius** beyond it when behavior, types, or contracts change:
   callers, usages, implementations, persistence/events, serialization, and tests.
4. Prioritize correctness, regressions, invariants, API contracts, concurrency/data consistency, error handling, and
   tests before maintainability concerns. Apply the project's relevant language, framework, and testing conventions
   when evaluating the change.
5. Verify every finding against the actual code. Run the project-required verification, plus focused tests or checks
   when they can confirm or refute a finding.
6. Report findings ordered by severity. If there are no material findings, say so explicitly.

## Findings

Report only issues with a plausible execution path and a concrete consequence, such as:

- behavior contradicting the requirement, tests, or an established contract;
- invariants enforced on only some creation or mutation paths;
- API or data-shape changes whose consumers were not updated;
- boundary cases that can produce incorrect or inconsistent state;
- swallowed, mistranslated, or incorrectly recovered failures;
- concurrency, cancellation, resource-lifetime, or ordering bugs;
- tests that do not exercise the changed behavior or would not catch the regression;
- complexity that materially increases defect risk.

Do not report:

- subjective style preferences consistent with nearby code;
- unrelated refactors or cleanup;
- trivial duplication or missing comments unless they hide a concrete defect.

## Output

Use these severities:

- **P0**: catastrophic or unsafe; must not merge.
- **P1**: likely correctness or regression bug; should block merge.
- **P2**: real issue with limited impact or conditions; should be fixed.
- **P3**: worthwhile improvement that does not affect correctness.

Format each finding as:

```text
[P1] Short title - path/to/File.kt:42
```

Follow it with a concise explanation of the triggering condition and consequence. Use the smallest useful code location
and one concrete issue per finding.

Do not inflate severity. Do not praise unaffected code or restate the diff.
