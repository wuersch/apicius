# FEAT-002: My APIs home

**ID:** FEAT-002
**Status:** shipped
**Depends on:** PRIN-001, PRIN-002, ADR-0004, ADR-0005, ADR-0008
**Mockup:** `docs/design/mockups/launcher-hybrid-v3.html` — frame `apicius launcher v3` (+ `— dark`), with design notes in `launcher-hybrid-v3-notes.md`.

> **Type:** Deterministic — behavior is fixed and verifiable. Every requirement maps to a
> pass/fail test.

## User Story
As an API designer, I want to browse all the APIs I have access to, so that I can find and open the
one I want to work on.

## Context / Notes
This is the surface the designer lands on after login and returns to between APIs — it owns the
**Create** (FEAT-003) and **Import** (FEAT-004) entry points and the first-run **empty state**. It
reads the summary projection (ADR-0008), not the heavy spec body. The per-user greeting and
jump-back-in resume need identity (FEAT-001), which create/import do not.

## Interaction Model
- **Entry point:** the application home — the unit is an **API** (a noun), never a path or an endpoint
  list. Create and Import are launched from here.
- **Vocabulary:** each card speaks in domain terms — "N resources · N operations" (the API's nouns and
  what it lets people do), API version, last-edited — not spec-tree language.
- **Ordering invariant:** APIs are listed **alphabetically by title**; no user-facing sort in v1.
- **Projection direction:** the list is a **read-only projection** (the ADR-0008 summary), never an
  editing surface; opening a card enters the editor, where the document is authoritative.
- **Escape hatch:** none here — the home is navigation, not editing; source/advanced live in the editor.

## Use Cases

### UC1: Resume the last-edited work (happy path)
- **Precondition:** designer is authenticated and has edited at least one API before.
- **Flow:** lands on the home; sees the greeting and a "jump back in" card naming the API and, when
  available, the last-edited capability; opens it.
- **Outcome:** the editor opens on that API at (or near) where they left off.

### UC2: Find and open a specific API (browse)
- **Precondition:** authenticated; several APIs accessible.
- **Flow:** scans the card list (alphabetical by title), reading title, description,
  resource/operation counts, version, and last-edited; opens the target.
- **Outcome:** the editor opens on the chosen API.

### UC3: First run, no APIs (empty state)
- **Precondition:** authenticated; no accessible APIs yet.
- **Flow:** lands on the home; sees an empty state offering Create and Import.
- **Outcome:** the designer starts a new API (FEAT-003) or imports one (FEAT-004).

## Acceptance Criteria
- **AC1 (UC1):** Given the designer has previously edited an API, when they land on the home, then a
  jump-back-in card shows that API and — when a last-edited capability is recorded — names it; opening
  it enters the editor on that API. When no capability is recorded, the card resolves to API-level.
- **AC2 (UC2):** Given ≥1 accessible API, when the home renders, then a card per API is shown in
  alphabetical order by title, each with monogram, title, truncated description, "N resources · N
  operations", API version (`info.version`, e.g. `v1.0`), and a relative last-edited (e.g. "2d ago").
- **AC3 (UC2):** Given the designer opens a card, when it is selected, then the editor opens on that API.
- **AC4 (UC3):** Given no accessible APIs, when the home renders, then an empty state is shown with
  Create and Import entry points.
- **AC5 (UC2):** Given the home renders, then only the summary projection (ADR-0008) is read — the spec
  body JSONB is not deserialized.
- **AC6 (UC1/UC2):** Given an authenticated designer, when the home renders, then it greets them by
  name with the time of day and the current date.

## Data / Domain

**Edited domain (OpenAPI) — link, don't reproduce.**
Nothing is edited here. The `v1.0` shown per card is the API version from each spec's
[Info Object](spec-3.1#info-object) (`info.version`).

**Application domain (Apicius) — describe fully.**
- Entities / fields touched: per API, the ADR-0008 **summary projection** — `title`, `description`,
  `api_version`, `resource_count`, `operation_count`, `updated_at`, `owner`, and the per-user
  **last-edited-location** (API + optional capability). `owner` is shown as provenance, not used to
  filter the list. Plus the current user's identity / display name from auth (ADR-0005).
- Validation rules: none — read-only surface.
- States / transitions: none; navigation only.

## Non-Goals
- Draft/Live lifecycle badge on cards — future feature.
- Rules/validation badge and activity roll-ups ("N need a look", "N active today") — depend on the
  rules engine (PRIN-006) and collaboration; future.
- User-facing **sort** and **pagination / "Show all"** — future.
- **Per-API actions** (edit details / duplicate / delete / download) — owned by FEAT-007
  (Manage an API) and FEAT-008 (Export an API document).
- **Table / condensed view** — card view only in v1.
- Deriving `resource_count` for imported-but-unmodeled specs — owned by the resource-extraction feature.
