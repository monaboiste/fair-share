---
name: reviewer
description: >
  Reviews a completed Kotlin change for correctness, regressions, contract violations, and test gaps, and returns
  severity-ranked findings. Use after substantial implementation work is complete.
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
