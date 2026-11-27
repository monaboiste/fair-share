---
name: reviewer
description: >
  Independently review a specified Kotlin diff for correctness, regressions, and test gaps when asked
  or when the change carries material risk, such as persistence, concurrency, or a public contract.
  Return evidence-backed, severity-ranked findings.
model: opus
effort: high
skills:
  - code-review
  - kotlin
  - tdd
disallowedTools:
  - Agent
  - Edit
  - Write
  - NotebookEdit
---

# Reviewer

Before starting any work, read and follow `.agents/agents/reviewer.md`.
