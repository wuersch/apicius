# FEAT-008: Export an API document

**ID:** FEAT-008
**Status:** building
**Depends on:** PRIN-003, ADR-0004, ADR-0009
**Mockup:** `docs/design/mockups/launcher-hybrid-v8.html` — frame `launcher 1g·1 — card overflow menu` (Download items), with design notes in `launcher-hybrid-v8-notes.md`.

> **Type:** Deterministic — behavior is fixed and verifiable. Every requirement maps to a
> pass/fail test.

## User Story
As an API designer, I want to download an API as a standard OpenAPI document in YAML or JSON,
so that I can use it outside Apicius — in other tooling, to share, or as a backup.

## Context / Notes
The outbound half of PRIN-003: the document leaves Apicius whole. The promise is **functional
equivalence** — any OpenAPI consumer reads identical data from the export as from the stored
document: every node, including unmodeled / preservation-bag content (FEAT-004), in authored
property order (which ADR-0004's storage deliberately retains). It is *not* textual identity:
YAML comments, anchors, and formatting are consumed when the ADR-0009 engine parses an import,
so no later export can reproduce them. Future round-trip and source-peek work (roadmap) extends
this feature rather than re-owning export.

## Interaction Model
- **Entry point:** an API as an object — from the "My APIs" home (FEAT-002); never a node or
  sub-tree.
- **Vocabulary:** "Download as YAML" / "Download as JSON" — the whole document, one step; the
  format choice is the only decision.
- **Projection direction:** export serializes the source-of-truth document itself — not a
  projection, so nothing modeled-but-uncurated is missing from it.
- **Escape hatch:** export *is* the escape hatch — the guarantee that Apicius never locks a
  design in.

## Use Cases

### UC1: Download as YAML (happy path)
- **Precondition:** designer has an API.
- **Flow:** requests the API as YAML.
- **Outcome:** receives a `.yaml` file, named from the API's title, functionally equivalent to
  the stored document.

### UC2: Download as JSON (alternate)
- **Precondition:** as UC1.
- **Flow:** requests the API as JSON.
- **Outcome:** as UC1, with a `.json` file.

## Acceptance Criteria
- **AC1 (UC1/UC2):** Given any API, when it is exported in either format, then the output is
  functionally equivalent to the stored document — every node reproduced, including unmodeled /
  preservation-bag content, in authored property order — and parses as valid YAML/JSON.
- **AC2 (UC1/UC2):** Given any API — created empty, imported from YAML, or imported from
  JSON — then both formats are offered; the source format does not constrain the export format.
- **AC3 (UC1/UC2):** Given an export, then its `openapi` version is the stored one — no
  conversion — and the file is named from the API's title with the format's extension.

## Data / Domain

**Edited domain (OpenAPI) — link, don't reproduce.**
- The whole [OpenAPI Object](spec-3.1#openapi-object) — serialized, never restructured.

**Application domain (Apicius) — describe fully.**
- Entities / fields touched: none written — export reads the `spec` row's body (ADR-0004) and
  serializes it via the ADR-0009 engine.
- Validation rules: none; every persisted document is exportable.
- States / transitions: none.

## Non-Goals
- **Textual fidelity** — YAML comments, anchors/aliases, quoting and whitespace style are not
  reproduced (lost at import parse; see Context).
- **Spec-version conversion on export** (3.0 ↔ 3.1 ↔ 3.2) — the future conversion feature.
- **Export options** (resolved `$ref`s, bundling, filtering) — if such options ever appear, the
  v8 notes already anticipate the two direct download actions graduating to a dialog.
- **Publishing** — pushing to a registry, URL, or share link is not exporting a file.
