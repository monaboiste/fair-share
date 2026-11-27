---
name: implement
description: Implement a ready issue from specification to verified change.
disable-model-invocation: true
---

# Implement

Deliver one issue as a verified vertical slice.

## 1. Ground

- Fetch the issue, its acceptance criteria, dependencies, parent specification, and relevant decision comments.
- Read the applicable repository instructions and follow their context pointers.
- Inspect the existing implementation and test seams.
- Check readiness, including unresolved blockers and conflicts between requirements and recorded decisions.
- Escalate unresolved decisions that affect observable behavior, architecture, or acceptance criteria.

**Done:** Every acceptance criterion is understood, its applicable decisions are identified, and no unresolved blocker
prevents implementation.

## 2. Plan

Map every acceptance criterion to an existing or new verification seam. Identify the smallest implementation increments
that deliver the requested behavior. Reuse established boundaries and terminology. Keep the change within the issue's
scope.

**Done:** Every acceptance criterion has a verification strategy and every planned change serves the issue.

## 3. Implement

Use the `tdd` skill for production code changes. Deliver one behavioral increment at a time. Resolve local
implementation details within established architectural decisions. If a material specification gap emerges, surface it
before implementing the affected behavior. **Done:** Every acceptance criterion is implemented or explicitly reported as
blocked.

## 4. Verify

- Run the verification required by repository instructions.
- Review the complete diff against the acceptance criteria, applicable ADRs, and repository conventions.
- Resolve findings and rerun affected checks.

**Done:** Every acceptance criterion has supporting evidence, every required check has passed, and every review finding
is resolved or explicitly accounted for.

## 5. Handoff

Report:

- Implemented behavior.
- Acceptance criteria and supporting evidence.
- Verification results.
- Remaining blockers, deviations, and risks.

When requested, commit the verified change and open a pull request linked to the issue. Leave issue closure to
acceptance.
