---
name: reviewer
description: >
  Independently review a specified Kotlin diff for correctness, regressions, and test gaps when asked
  or when the change carries material risk, such as persistence, concurrency, or a public contract.
  Return evidence-backed, severity-ranked findings.
model: "@slow"
thinking-level: high
autoloadSkills:
  - code-review
  - kotlin
  - tdd
tools:
  - read
  - grep
  - glob
  - find
  - bash
  - lsp
  - ast_grep
---

# Reviewer

Before starting any work, read and follow `.agents/agents/reviewer.md`.
