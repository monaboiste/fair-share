---
name: commit
description: Propose and create logical git commits.
disable-model-invocation: true
---

# Commit

1. Inspect recent commits (`git log -5 --oneline`) and infer the repository's commit convention (e.g. Conventional
   Commits, `TICKET-123` prefix, casing, scopes).
2. Inspect changes (`git status --short -uall`, `git diff`) and group related files into logical commits.
3. Draft messages matching the detected convention. If none is clear, use imperative subjects ≤50 chars, 72-char body,
   and prefer lowercase.
4. Show the commit plan for approval, then stage and commit approved groups.
