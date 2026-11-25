# Domain Docs

How engineering skills should consume this repo's domain documentation.

## Before exploring, read these

- `GLOSSARY.md` at repo root, or `GLOSSARY-MAP.md` if it exists; read relevant context glossaries.
- `docs/adr/`: read ADRs touching the area. If any files are absent, proceed silently. `/domain-modeling` creates them
  when terms or decisions are resolved.

## File structure

Single-context repo:

```text
/
├── GLOSSARY.md
├── docs/adr/
└── src/
```

## Use the glossary's vocabulary

Use defined domain terms in issue titles, proposals, hypotheses, and test names. If a concept is missing, reconsider
terminology or note the gap for `/domain-modeling`.

## Flag ADR conflicts

If output contradicts an ADR, surface the conflict explicitly rather than silently overriding it.
