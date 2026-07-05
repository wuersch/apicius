# FEAT-004: Import an existing OpenAPI document

**ID:** FEAT-004
**Status:** specced
**Depends on:** PRIN-003, PRIN-004, ADR-0004, ADR-0009
**Mockup:** `docs/design/mockups/launcher-hybrid-v6.html` — frames `apicius launcher v6 · 1c — New API menu`, `1d — import picker`, `1e — import reading`, `1f — import review` (+ dark variants), with design notes in `launcher-hybrid-v6-notes.md`.

> **Type:** Deterministic — behavior is fixed and verifiable. Every requirement maps to a
> pass/fail test.

## User Story
As an API designer, I want to import an existing OpenAPI document, so that I can continue working on
it in Apicius's intent-first editor.

## Context / Notes
Launched from the "My APIs" home (FEAT-002). **Lossless round-trip is the whole point** (PRIN-003):
the parsed superset model plus a **preservation bag** for anything Apicius doesn't model first-class,
stored in the spec's body JSONB (ADR-0004). The spec version is **detected** from the file, not
chosen; title/description come from the imported `info`. Sources in v1 are **file upload + paste**.

## Interaction Model
- **Entry point:** the "My APIs" home's **Import** action.
- **Vocabulary:** "Import" accepts a **whole OpenAPI document**; title/description/version are read
  from the file's `info` / `openapi`, never typed.
- **Ordering invariant:** the imported document becomes the model; resource/capability projection
  follows from it (as with any API), never authored as paths here.
- **Projection direction:** document-as-source-of-truth from ingestion — the superset model plus a
  preservation bag; `import → export` is faithful.
- **Escape hatch:** anything not modeled first-class stays reachable under **Advanced / source**, never
  dropped (PRIN-003, PRIN-004).

## Use Cases

### UC1: Import a valid document by file (happy path)
- **Precondition:** on the My APIs home; designer has a `.yaml`/`.yml`/`.json` OpenAPI 3.0–3.2 file.
- **Flow:** opens Import; uploads the file; confirms.
- **Outcome:** a new API is persisted losslessly (modeled nodes + preservation bag) and the editor
  opens on it; title/description/version come from the file.

### UC2: Import by pasting raw text (alternate)
- **Precondition:** on the My APIs home; designer has YAML or JSON text.
- **Flow:** opens Import; pastes the document; confirms.
- **Outcome:** same as UC1.

### UC3: Unsupported or unparseable input (failure path)
- **Precondition:** on Import.
- **Flow:** supplies Swagger 2.0, or malformed YAML/JSON, or a structurally questionable 3.x document.
- **Outcome:** 2.0 and unparseable input are **rejected** with a clear message and nothing is
  persisted; a valid-but-questionable document is **imported with surfaced warnings**.

## Scenarios

### Scenarios for UC3
| #  | Situation (concrete input)                     | Expected outcome                                              |
|----|------------------------------------------------|--------------------------------------------------------------|
| S1 | Swagger / OpenAPI **2.0** document             | Rejected with an "OpenAPI 3.0–3.2 only" message; not persisted |
| S2 | Malformed YAML / JSON                          | Rejected with a parse-error message; not persisted           |
| S3 | Valid 3.x with unmodeled / oddly-placed nodes  | Imported; unmodeled nodes preserved; structural warnings surfaced |
| S4 | Valid 3.x with vendor extensions (`x-*`)       | Imported; extensions preserved in the preservation bag       |

## Acceptance Criteria
- **AC1 (UC1/UC2):** Given a valid OpenAPI 3.0–3.2 document via file or paste, when the designer imports
  it, then a new API is persisted and the editor opens on it, with title/description/version read from
  the document.
- **AC2 (UC1 / lossless):** Given an imported document, when it is exported unchanged, then export
  reproduces 100% of the original content — including nodes Apicius doesn't model (preservation bag,
  S3/S4) — so `import → export` is faithful. Verified against representative real-world specs, not
  just fixtures — this test is also ADR-0009's round-trip verification of the document engine.
- **AC3 (UC3 / S1):** Given a Swagger 2.0 document, when import is attempted, then it is rejected with a
  clear "3.0–3.2 only" message and nothing is persisted.
- **AC4 (UC3 / S2):** Given unparseable input, when import is attempted, then it is rejected with a
  parse-error explanation and nothing is persisted.
- **AC5 (UC3 / S3):** Given a valid but structurally questionable document, when imported, then it is
  accepted and structural warnings are surfaced.

## Data / Domain

**Edited domain (OpenAPI) — link, don't reproduce.**
- The whole [OpenAPI Object](spec-3.1#openapi-object); version read from the `openapi` field; metadata
  from the [Info Object](spec-3.1#info-object).

**Application domain (Apicius) — describe fully.**
- Entities / fields touched: a new `spec` row (ADR-0004) — `body` JSONB = parsed superset model +
  **preservation bag**; the ADR-0008 projection is written on import.
- Validation rules: `openapi` ∈ 3.0–3.2 (reject 2.0, no conversion); the document must parse as YAML or
  JSON; structural issues surface as **warnings**, not rejection.
- States / transitions: imported → editable.
- Preservation: unmodeled / unknown nodes retained verbatim (preservation bag) for faithful round-trip.

## Non-Goals
- URL / Git import — deferred.
- Swagger 2.0 (and up-conversion).
- Merging an import *into* an existing API.
- Bulk import.
- Deriving Apicius resources from the imported spec — a separate future feature (defines
  `resource_count` for imported docs).
