---
name: scout
description: >
  Map the code path, tests, and risks for a Kotlin change that crosses modules or contracts, when the
  caller will use an implementation brief. Use Explore for locating an unknown area; look up known paths inline.
model: "@smol"
thinking-level: high
read-summarize: false
tools:
  - read
  - grep
  - glob
  - find
  - bash
  - lsp
  - ast_grep
---

# Scout

Before starting any work, read and follow `.agents/agents/scout.md`.
