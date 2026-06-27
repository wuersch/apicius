# Conventions

The wiring that makes progressive disclosure work. Small, stable, rarely changes.

## Identifiers
Format `PREFIX-NNN`, zero-padded to 3 (ADRs use 4: `ADR-0007`).
- `PRIN-` design/UX principle — `docs/product/constitution.md`
- `FEAT-` feature spec — `docs/spec/features/`
- `ADR-`  architecture decision — `docs/architecture/adrs/`

IDs are permanent. Never renumber. Retire by marking `superseded`, not by deleting.

## Citations — the citation graph
Every doc declares what it depends on, by ID, in a `Depends on:` header line.
- A FEAT cites the PRIN it honors and the ADR that constrains it.
- An ADR cites the FEAT/decision that triggered it.
- A mock-up frame's label cites its FEAT (`data-screen-label="FEAT-NNN · short title"`).
- Code references IDs in comments where non-obvious (`// PRIN-003`).

`grep -r FEAT-NNN .` returns the full context for a feature — spec, decisions, mock-up,
code. That citation graph replaces pixel-level mock↔code sync.

## Feature status
`proposed → specced → building → shipped` (plus `superseded`).
Source of truth = the feature file header. `docs/spec/features.md` mirrors only
**ID · title · status** as the load-first index.

## Files
- One feature per file: `FEAT-NNN-kebab-title.md`.
- One ADR per file: `ADR-NNNN-kebab-title.md`.
- Diagrams: Mermaid `.mmd` under `docs/architecture/diagrams/` (text, diffable, draw.io-importable).

## Mock-ups
Design references, not production code. Fork a new mock-up per feature change; never sync
mock-ups to code. The running app is the source of truth once a feature ships.
