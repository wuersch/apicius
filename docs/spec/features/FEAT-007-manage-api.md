# FEAT-007: Manage an API

**ID:** FEAT-007
**Status:** building
**Depends on:** PRIN-003, ADR-0004, ADR-0008
**Mockup:** `docs/design/mockups/launcher-hybrid-v8.html` — frames `launcher 1g·1 — card overflow menu`, `1g·2 — edit details dialog`, `1g·3 — delete confirm`, with design notes in `launcher-hybrid-v8-notes.md`.

> **Type:** Deterministic — behavior is fixed and verifiable. Every requirement maps to a
> pass/fail test.

## User Story
As an API designer, I want to manage an API as a whole — update its details, duplicate it,
delete it — so that my workspace holds exactly the APIs I mean to keep, correctly described.

## Context / Notes
The counterpart to creating (FEAT-003) and importing (FEAT-004): caring for the API **as an
object** over its lifetime. Management never reaches inside the document — contents are edited
in the editor. Duplicate has **fork semantics**: the copy is the same design under a new
identity, not a fresh API seeded from an old one. There is no archive or undo; deletion is
permanent.

## Interaction Model
- **Entry point:** an API on the "My APIs" home (FEAT-002) — actions address the API as an
  object, never a path, node, or its contents.
- **Vocabulary:** "Title" → `info.title`; "Description" → `info.description`; "Version" →
  `info.version` (bumping it is the most common reason to edit details). "Duplicate" = fork.
  "Delete" = permanent removal.
- **Projection direction:** editing details rewrites `info` in the document (the source of
  truth); the home re-renders from the ADR-0008 summary projection.
- **Escape hatch:** none — management touches `info` and the API's lifecycle only;
  contents, source, and advanced live in the editor.

## Use Cases

### UC1: Update an API's details (happy path)
- **Precondition:** designer has an API on the home.
- **Flow:** opens the API's details; changes title, description, and/or version; saves.
- **Outcome:** the document's `info` reflects the changes; nothing else in the document
  changes; the API's card shows the new values.

### UC2: Duplicate an API (fork)
- **Precondition:** designer has an API they want to fork — to experiment, or to build on.
- **Flow:** duplicates the API.
- **Outcome:** a new, independent API exists alongside the original — same document (including
  `info.version` and anything Apicius doesn't model first-class), derived title
  "<title> (copy)", owned by the duplicator. Edits to either no longer affect the other.

### UC3: Delete an API
- **Precondition:** designer has an API they no longer need.
- **Flow:** requests deletion; is told the blast radius (resources, capabilities, no undo);
  deliberately confirms.
- **Outcome:** the API is permanently removed — no longer listed, no longer openable.

### UC4: Invalid details (failure path)
- **Precondition:** on an API's details.
- **Flow:** attempts to save with an empty title.
- **Outcome:** the save is rejected with a "title required" message; nothing is persisted.

## Acceptance Criteria
- **AC1 (UC1):** Given new details are saved, then `info.title` / `info.description` /
  `info.version` reflect them (description removable), every other document node is unchanged,
  and the ADR-0008 projection is updated.
- **AC2 (UC1):** Given the details surface, then the `openapi` spec version is visible but not
  editable — immutability is FEAT-003's rule; this feature offers no way around it.
- **AC3 (UC2):** Given an API is duplicated, then a new API is persisted with its own UUID,
  `owner` = the duplicating user, fresh timestamps, `info.title` = "<original title> (copy)",
  and a document otherwise functionally equivalent to the original — including unmodeled /
  preservation-bag content and the original `info.version`.
- **AC4 (UC2):** Given the duplicate exists, then it appears on the home like any API, and
  changes to one API never appear in the other.
- **AC5 (UC3):** Given deletion is requested, then nothing is removed until the designer
  completes a deliberate confirmation that names the API; confirming permanently removes it
  from the list and from access by ID; abandoning the confirmation changes nothing.
- **AC6 (UC4):** Given an empty title, when the designer attempts to save details, then the
  save is rejected with a "title required" message and nothing is persisted.

## Data / Domain

**Edited domain (OpenAPI) — link, don't reproduce.**
- [Info Object](spec-3.1#info-object) — `title`, `description`, `version`. Nothing else is
  touched.

**Application domain (Apicius) — describe fully.**
- Entities / fields touched: the `spec` row (ADR-0004) — details update `title`, `updated_at`,
  and the ADR-0008 projection columns; duplicate inserts a new row (new UUID, `owner`,
  timestamps) with a copy of the body; delete removes the row.
- Validation rules: `title` non-empty on edit; delete requires the completed confirmation.
- States / transitions: delete is terminal — no archive, no undo. A per-user
  last-edited-location (FEAT-002) pointing at a deleted API is cleared; jump-back-in never
  references a deleted API. Last-edited-location is not copied on duplicate.

## Non-Goals
- **Rename as a separate action** — renaming is editing the title, inside details.
- **Archive / trash / undo** — would soften delete to a plain confirmation; future.
- **Changing the `openapi` spec version** — conversion is a real future feature (FEAT-003
  immutability stands until then).
- **Sharing / access management, history** — future; the v8 notes list them as plausible
  menu growth.
- **Bulk operations** — one API at a time.
- **"New API from template"** — duplicate is a fork; seeding a fresh API (reset version, clean
  slate) from an existing one is a create-side feature (see FEAT-003 non-goals).
