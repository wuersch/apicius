# FEAT-011: Query parameters & headers

**ID:** FEAT-011
**Status:** specced
**Depends on:** PRIN-001, PRIN-002, PRIN-006, ADR-0009, FEAT-006, FEAT-009, FEAT-010
**Mockup:** `docs/design/mockups/launcher-hybrid-v9.html` — View 3 (Filters card; the card's
UI label is design's business — this spec says *query parameter*)

> **Type:** Deterministic — behavior is fixed and verifiable. Every requirement maps to a
> pass/fail test.

## User Story
As an API designer, I want to declare what a capability accepts — query parameters and
request headers — and what extra headers it sends back, each named in plain language with a
kind, so that clients know exactly how to call it without me authoring parameter syntax.

## Context / Notes
One model, three locations. Filtering ("show products under a budget") is one *use* of a
query parameter — so are sorting, search, and field selection; the model is named after the
mechanism, not one use case. The mockup's palette concerns (Sorting · Search · Pick fields ·
Bulk changes) are future recipes over this model, not part of it.

Headers are deliberately all designer-authored: no built-in header set is derived (no
RateLimit-*, ETag, Cache-Control — there is no real common ground to standardize on). The
only header line a capability always shows is the derived, read-only content-negotiation
line (FEAT-009), which is display, not document content.

## Interaction Model
- **Entry point:** a capability's contract view (FEAT-009) — inputs and response headers are
  declared on the capability they belong to, never on a path or operation node.
- **Vocabulary:** each declaration is a freeform plain-language **name** + a **kind** +
  optional **description** + **optionality** (inputs only, default optional), in one of
  three locations: *query parameter* (travels as `?name=`), *request header*, *response
  header* (sent back with the success answer). Kinds are FEAT-006's plain-language
  vocabulary (Text and its refinements, Whole number, Decimal number, Yes/no, Date, Date &
  time) plus **"one of …"** — a fixed set of distinct text values. The serialized form is
  derived, never typed.
- **Ordering invariant:** name first, then kind, then attributes; serialization is derived.
  A declaration belongs to exactly one capability and one location.
- **Projection direction:** each add/change/remove is one atomic document mutation through
  the engine seam (ADR-0009); what is shown is projected back from the document.
- **Escape hatch:** none yet (source peek is future). The guarantee it inherits: an edit
  writes only that declaration's constructs and touches nothing else.

## Derived names
- **Query parameters** reuse FEAT-006's property-name derivation: prose derives to camelCase
  ("Price max" → `priceMax`); deliberate identifiers pass through untouched.
- **Headers** derive to hyphenated capitalized words ("request id" → `Request-Id`); a typed
  identifier convention (`X-Request-ID`) is respected, never corrected.
- The derived name is shown live and is the declaration's identity, same stance as fields.

## Why "one of" is allowed here
A fixed value set on a parameter (`status` — one of `available · pending · sold`) serializes
as an inline `enum`. FEAT-006 excludes inline enums *on datatypes* — that rule keeps shapes
named; a parameter's value set is not a shape and threatens nothing. Named enum datatypes,
when they arrive, will be usable here too.

## Use Cases

### UC1: Add a query parameter (happy path)
- **Precondition:** designer has a capability open.
- **Flow:** declares an input that travels in the query: names it in plain language, picks a
  kind (e.g. "one of available · pending · sold", or Decimal number for a `priceMax` budget
  cap), optionally describes it; confirms.
- **Outcome:** the capability's operation carries the query parameter, serialized per the
  kind; the contract view shows it in plain language with the derived name.

### UC2: Add a request header (alternate)
- **Precondition:** designer has a capability open.
- **Flow:** declares an input that travels as a header (e.g. "Request id" for tracing);
  confirms.
- **Outcome:** the operation carries the header parameter with its derived
  hyphenated-capitalized name.

### UC3: Add a response header (alternate)
- **Precondition:** designer has a capability open.
- **Flow:** declares a header the capability sends back; confirms.
- **Outcome:** the capability's success answer(s) declare the header. The shared standard
  failure answers (FEAT-009) are untouched — per-capability furniture never grafts onto
  shared responses.

### UC4: Change or remove a declaration (alternate)
- **Precondition:** the capability has a declaration.
- **Flow:** renames it, changes its kind, optionality, or description — or removes it;
  confirms.
- **Outcome:** the declaration is rewritten in place, or gone without other trace; nothing
  else in the document changes.

### UC5: Invalid or conflicting name (failure)
- **Precondition:** adding or renaming a declaration.
- **Flow:** attempts to confirm a name that derives to nothing, collides case-insensitively
  with an existing declaration in the same location on this capability, or claims a reserved
  name — `page`/`limit` as query parameters on a paged capability (FEAT-010 owns them);
  `Accept`, `Content-Type`, or `Authorization` as headers (owned by content negotiation and
  the future security feature).
- **Outcome:** the edit cannot be completed; a message names the problem; nothing is
  persisted.

## Acceptance Criteria
- **AC1 (UC1):** Given a valid name and kind, when the designer confirms a query parameter,
  then the operation gains exactly one query parameter with the derived name, the kind's
  serialization per FEAT-006's table (a "one of" kind serializing as an inline value
  list), `required` per the optionality choice, and the description when given — and no
  other document content changes.
- **AC2 (UC2):** Given a request header declaration, when confirmed, then the operation
  gains exactly one header parameter with the derived hyphenated-capitalized name, same
  guarantees as AC1.
- **AC3 (UC3):** Given a response header declaration, when confirmed, then each success
  answer of the capability declares that header with the kind's serialization; the shared
  failure answers are byte-identical before and after.
- **AC4 (UC1–UC3):** Given any successful edit, then it is one atomic mutation, the document
  contains no Apicius-specific content, ADR-0008 counts are unchanged, and the designer's
  last-edited location moves to this API in the same transaction.
- **AC5 (UC4):** Given a change, when confirmed, then the declaration is rewritten in place
  with nothing else changing; given a removal, the declaration's constructs are absent with
  no other trace.
- **AC6 (UC5):** Given a name empty after derivation, a case-insensitive collision within
  the same location on this capability, or a reserved name, then the edit cannot be
  completed, a message names the problem, and nothing is persisted.
- **AC7 (UC1):** Given a "one of" kind, then its values are non-empty, distinct, and at
  least one; violating that blocks the edit as in AC6.

## Data / Domain

**Edited domain (OpenAPI) — link, don't reproduce.**
- [Parameter Object](spec-3.1#parameter-object) — `in: query` / `in: header`, `required`,
  `schema` (including `enum` for "one of"), `description`.
- [Header Object](spec-3.1#header-object) — response headers under a response's `headers`.

**Application domain (Apicius) — describe fully.**
- Entities / fields touched: the `spec` row — `body` JSONB via the engine seam (ADR-0009);
  `last_edited_location` at the same chokepoint; ADR-0008 counts unaffected.
- Validation rules: derived name non-empty; case-insensitive uniqueness per location per
  capability; reserved names as in UC5; kind from FEAT-006's vocabulary or "one of" with ≥ 1
  distinct values; optionality only on inputs.
- States / transitions: none beyond saved-document; every edit is atomic.

## Non-Goals
- Palette concerns as named recipes (Sorting, Search, Pick fields, Bulk changes) — future
  features layered on this model.
- Path parameters — always derived (FEAT-005), never authored.
- Cookie parameters; list-valued parameters (`style`/`explode` semantics); designer-set
  default values; per-value descriptions on "one of"; deprecation flags.
- Targeting individual answers with a response header — success answers as a set is the
  granularity.
- The "add a field to Product…" escape hatch from the mockup's filter picker — shape editing
  keeps one home (FEAT-006).
