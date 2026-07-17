# FEAT-010: Paging on list capabilities

**ID:** FEAT-010
**Status:** building
**Depends on:** PRIN-002, PRIN-006, ADR-0009, FEAT-005, FEAT-009
**Mockup:** `docs/design/mockups/launcher-hybrid-v9.html` — View 3 (Paging card) and state
3·2 (paging off)

> **Type:** Deterministic — behavior is fixed and verifiable. Every requirement maps to a
> pass/fail test.

## User Story
As an API designer, I want capabilities that return lists to page their results by default,
with a deliberate per-capability opt-out, so that responses stay bounded as the data grows
without me designing paging myself.

## Context / Notes
Applies to list-returning capabilities — today that is exactly **Browse** (the standard
five's only list). The contract follows the modern-petstore reference (petstoreapi.com).
Paging extends what derivation produces (FEAT-005's Derivation section lists this feature as
an extension); it is applied wherever applicable, with no API-level convention setting and no
deviation flag — the opt-out is per capability, deliberate, and reversible.

Shipping this feature rewrites no existing document: pre-existing Browse capabilities show
paging as available-but-absent and adopt it with one action (same stance as FEAT-009's
errors).

## Interaction Model
- **Entry point:** a list capability's contract view (FEAT-009) — paging appears as part of
  what the capability promises, framed as built-in behavior (PRIN-006), never as parameters
  the designer authors.
- **Vocabulary:** "pages" / "results come back N at a time" → the `page`/`limit` query
  parameters and the `pagination` member of the wrapped list; "the whole list in one
  response" → their absence. The designer chooses behavior; the serialization is derived.
- **Ordering invariant:** paging state is a property of the capability, changed only from the
  capability; it never reorders or reshapes the list items themselves (`data` is untouched
  either way).
- **Projection direction:** paging on/off is derived structurally from the document (the
  presence of the paging constructs) — no marker, no extension. Each change is one atomic
  document mutation through the engine seam (ADR-0009).
- **Escape hatch:** the opt-out itself (PRIN-006: built in, explained, overridable). The
  consequence is stated in plain language — the whole list in one response is fine for a
  small, bounded set and risky as it grows.

## Paging — the contract
- Query parameters, both optional, with document-declared defaults:
  - `page` — whole number ≥ 1, default 1;
  - `limit` — whole number 1–100, default 20.
- The list's success answer stays the inline wrapped list (FEAT-005) and gains a
  `pagination` member:

  ```json
  { "data": [ …the items… ],
    "pagination": { "page": 1, "limit": 20, "totalItems": 45, "totalPages": 3 } }
  ```

  `pagination` and its four fields are required while paging is on; all four are whole
  numbers, `totalItems`/`totalPages` ≥ 0.
- Paging **off**: `page`, `limit`, and `pagination` are absent; the answer remains
  `{ "data": [...] }` — never a bare array (the wrapper's evolvability job outlives paging).
- Deliberately no `links` object (`self`/`next`/`prev`/`last`): complexity we can't carry
  yet; link relations are a future concern (HATEOAS).

## Use Cases

### UC1: Create a list capability (happy path)
- **Precondition:** any API.
- **Flow:** the designer creates a resource whose capabilities include Browse (FEAT-005).
- **Outcome:** Browse pages from birth: the paging parameters and `pagination` member exist
  per the contract above, and the capability's contract view states the behavior in plain
  language ("results come back 20 at a time").

### UC2: Opt a capability out of paging (alternate)
- **Precondition:** a paged list capability.
- **Flow:** the designer switches paging off; the consequence is stated in plain language.
- **Outcome:** the document loses exactly the paging constructs — `page`, `limit`, the
  `pagination` member and its required entries; `data` and everything else are untouched.
  The contract view now states the whole list comes in one response.

### UC3: Opt back in (alternate)
- **Precondition:** a list capability opted out of paging.
- **Flow:** the designer switches paging on.
- **Outcome:** the exact contract above is restored. Opting out and back in yields a
  document functionally identical to never having opted out.

### UC4: Adopt paging on an older capability (alternate)
- **Precondition:** a Browse capability derived before this feature; its operation carries
  no paging constructs.
- **Flow:** the contract view shows paging as available-but-absent; the designer adopts it
  with one action.
- **Outcome:** the capability pages per the contract above; nothing else in the document
  changes.

### UC5: Name collision on enabling (failure)
- **Precondition:** a list capability without paging that carries a designer-authored query
  parameter named `page` or `limit` (FEAT-011).
- **Flow:** the designer attempts to switch paging on.
- **Outcome:** the change is blocked; a message names the conflicting parameter; nothing is
  persisted.

## Acceptance Criteria
- **AC1 (UC1):** Given a resource is created with Browse after this feature ships, then the
  derived operation carries `page` and `limit` exactly per the contract (types, bounds,
  defaults, optional) and its success answer's wrapper carries the required `pagination`
  member with its four required whole-number fields.
- **AC2 (UC2):** Given a paged capability, when the designer opts out, then exactly the
  paging constructs are removed — the wrapper keeps `data` and is never a bare array — and
  no other document content changes.
- **AC3 (UC3):** Given an opted-out capability, when the designer opts back in, then the
  restored document is functionally identical to one that never opted out.
- **AC4 (UC2, UC3):** Given any paging change, then it is one atomic document mutation, the
  ADR-0008 counts are unchanged, and the designer's last-edited location moves to this API in
  the same transaction.
- **AC5 (UC4):** Given a pre-existing Browse without paging, then viewing performs no
  mutation; the adopt action writes exactly the paging constructs and nothing else.
- **AC6 (UC5):** Given a designer-authored `page` or `limit` query parameter on the
  capability, when enabling paging, then the change is rejected with a message naming the
  conflict and nothing is persisted.
- **AC7 (UC1–UC4):** Given any state of paging, then the document contains no
  Apicius-specific content — paging state is recognizable from structure alone.

## Data / Domain

**Edited domain (OpenAPI) — link, don't reproduce.**
- [Parameter Object](spec-3.1#parameter-object) — `page`/`limit` in `query` with
  `schema` bounds and `default`.
- [Schema Object](spec-3.1#schema-object) — the `pagination` member on the inline wrapper.

**Application domain (Apicius) — describe fully.**
- Entities / fields touched: the `spec` row — `body` JSONB via the engine seam (ADR-0009);
  `last_edited_location` at the same chokepoint; ADR-0008 counts unaffected.
- Validation rules: paging applies to list capabilities only (Browse today); enabling is
  blocked while a designer-authored query parameter claims `page` or `limit`.
- States / transitions: paged ↔ unpaged per capability, both directions cheap, atomic, and
  derived structurally from the document.

## Non-Goals
- API-level paging convention or "pages differently than the rest" deviation flag —
  considered and dropped; the per-capability opt-out is the whole model.
- `links` / HATEOAS, cursor-based paging, sorting, search — separate concerns.
- Paging on non-list capabilities, custom parameter names, or designer-set defaults/bounds.
- A paging house-rule/guidelines display ("1 overridden") — house rules are deferred.
