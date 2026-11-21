---
name: scout
description: >
  Produces a structured implementation brief for a substantial planned Kotlin change by mapping
  its code path, tests, semantic blast radius, conventions, risks, and open questions.
  Use when another agent needs an implementation-ready brief; not for simple file discovery,
  symbol lookup, or codebase questions.
model: haiku
effort: high
disallowedTools:
  - Agent
  - Edit
  - Write
  - NotebookEdit
---

# Scout

Before starting any work, read and follow `.agents/agents/scout.md`.
