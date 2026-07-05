# FEAT-003: Create a new empty API

**ID:** FEAT-003
**Status:** building
**Depends on:** PRIN-006, ADR-0004, ADR-0009
**Mockup:** Create-a-design dialog — title, optional description, and an explained OpenAPI-version picker (default 3.1).

> **Type:** Deterministic — behavior is fixed and verifiable. Every requirement maps to a
> pass/fail test.

## User Story
As an API designer, I want to create a new, empty API — giving it a title, an optional description,
and a one-time spec version — so that I have a persistent, clean starting point to model resources
and capabilities.

## Context / Notes
Launched from the "My APIs" home (FEAT-002) and persisted per ADR-0004. Creating the empty
container is the one moment that *precedes* any resource authoring, so it can't violate
resource-first. The spec version is chosen **once at creation and is immutable in v1**.

## Interaction Model
- **Entry point:** the "My APIs" home's **Create** action — never a file path or a spec node.
- **Vocabulary:** "Title" → `info.title`; "Description" → `info.description`; the version picker
  exposes plain-language capability tradeoffs (e.g. 3.1 → "full JSON Schema, webhooks, example
  variations; the safe modern default") — the user reads the tradeoff, not the encoding.
- **Ordering invariant:** creation precedes all resource/capability authoring; it produces the empty
  container the rest of the editor projects over. No path or operation is authored here.
- **Projection direction:** the dialog seeds a new OpenAPI document (`info` + `openapi`), which is the
  source of truth from the first save.
- **Escape hatch:** none needed at creation — there is no raw content yet; the source view becomes
  reachable once editing begins.

## Use Cases

### UC1: Create a blank API with the default version (happy path)
- **Precondition:** designer is authenticated, on the My APIs home.
- **Flow:** opens Create; enters a title (and optional description); leaves the version at its default;
  confirms.
- **Outcome:** a new API is persisted and the editor opens on it, empty, ready for the first resource.
  `info.version` is `1.0.0`; `openapi` is the latest patch of the chosen minor (default 3.1).

### UC2: Choose a non-default spec version (alternate)
- **Precondition:** as UC1; the designer needs 3.0 (widest tooling) or 3.2 (newest).
- **Flow:** opens Create; enters a title; selects a different version from the picker, reading each
  option's plain-language description; confirms.
- **Outcome:** the API is created with the chosen version, fixed for its lifetime.

### UC3: Missing title (failure path)
- **Precondition:** on the Create dialog.
- **Flow:** attempts to confirm with an empty title.
- **Outcome:** creation is blocked with a clear "title required" message; nothing is persisted.

## Acceptance Criteria
- **AC1 (UC1):** Given a valid title, when the designer confirms with the default version, then a new
  API is persisted with `info.title` = the title, `info.version` = `"1.0.0"`, `openapi` = the latest
  patch of 3.1, and the editor opens on the empty API.
- **AC2 (UC1):** Given an optional description is provided, when the API is created, then
  `info.description` is set; when omitted, `info.description` is absent.
- **AC3 (UC2):** Given the designer selects 3.0.x or 3.2.x, when the API is created, then `openapi`
  reflects the latest patch of that minor, and the version cannot be changed afterward in v1.
- **AC4 (UC1):** Given another API with the same title already exists, when the designer creates this
  one, then creation succeeds — titles need not be unique; APIs are keyed by UUID.
- **AC5 (UC3):** Given an empty title, when the designer attempts to confirm, then creation is rejected
  with a "title required" message and nothing is persisted.

## Data / Domain

**Edited domain (OpenAPI) — link, don't reproduce.**
- [Info Object](spec-3.1#info-object) — `title` (required), `description` (optional), `version` (the
  API's own version, required).
- The `openapi` version string (e.g. `3.1.1`).

**Application domain (Apicius) — describe fully.**
- Entities / fields touched: a new `spec` row (ADR-0004) — UUID `id`, `owner`, `title`,
  `created_at`/`updated_at`, `body` JSONB holding the new document. The ADR-0008 projection columns are
  written on create.
- Validation rules: `title` non-empty (required); `description` optional; spec version ∈
  {3.0.x, 3.1.x, 3.2.x}, defaulted to 3.1.
- States / transitions: created → editable; spec version is **immutable** in v1.

## Non-Goals
- Create-from-template (petstore / USPTO) — a separate future feature.
- Changing the spec version after creation — recreate is the v1 recourse.
- Setting `info.version` at creation — auto-seeded `1.0.0`, edited later.
