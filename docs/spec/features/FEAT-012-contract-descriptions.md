# FEAT-012: Describe the contract

**ID:** FEAT-012
**Status:** specced
**Depends on:** PRIN-002, ADR-0009, FEAT-005, FEAT-009
**Mockup:** `docs/design/mockups/launcher-hybrid-v10.html` — state 3·7 (quiet descriptions);
View 3 (capability description)

> **Type:** Deterministic — behavior is fixed and verifiable. Every requirement maps to a
> pass/fail test.

## User Story
As an API designer, I want to put a plain-words note on any part of a capability's contract,
so that generated docs and SDK comments carry my intent to readers.

## Context / Notes
Descriptions are already editable where their element is edited: fields (FEAT-006), the API's
`info` (FEAT-003/007), query parameters and headers (part of FEAT-011's model). This feature
adds the remaining contract elements: the **capability**, its **answers**, and the
**resource** after birth (FEAT-005 sets it only at creation).

A vocabulary distinction this feature owns: a capability's *label* ("Browse all products") is
the operation's `summary` — one short line, the capability's name, set by derivation. Its
*description* is the operation's `description` — free prose. This feature edits descriptions
only; renaming (the `summary`) is a different act and out of scope. Likewise, OpenAPI's
`example` is a separate concept with its own semantics — not a description.

## Interaction Model
- **Entry point:** the described element, in place — a description is read and edited where
  its element is shown (the capability's contract view, the resource), never in a separate
  documentation surface.
- **Vocabulary:** "a note for readers" → the element's OpenAPI `description`. Plain words;
  the designer is told the note becomes docs and SDK comments.
- **Ordering invariant:** a description belongs to exactly one element; editing it never
  changes the element itself (a capability description edit never touches its `summary`,
  answers, or parameters).
- **Projection direction:** each edit is one atomic document mutation through the engine seam
  (ADR-0009); displayed text is projected from the document.
- **Escape hatch:** none yet (source peek is future). The guarantee it inherits: a
  description edit writes only that element's `description` member.

## Use Cases

### UC1: Describe a capability (happy path)
- **Precondition:** designer has a capability open.
- **Flow:** writes or edits its description ("Anyone can browse the catalog…"); confirms.
- **Outcome:** the operation carries the description; the label is untouched. Clearing the
  description removes it — a capability without prose is valid.

### UC2: Describe an answer (alternate)
- **Precondition:** a capability with answers; derivation seeded each answer's description
  with noun-phrased default text (FEAT-005).
- **Flow:** replaces an answer's default description with their own words; confirms.
- **Outcome:** the answer carries the designer's text. Clearing it restores the derived
  default — OpenAPI requires every answer to carry a description, so there is no empty
  state.

### UC3: Describe the resource after birth (alternate)
- **Precondition:** an existing resource, with or without a creation-time description.
- **Flow:** writes, edits, or clears the resource's description.
- **Outcome:** the resource's schema carries the description; clearing removes the member
  entirely (omitted when blank, same stance as the API's `info.description`).

## Acceptance Criteria
- **AC1 (UC1):** Given a capability, when the designer confirms a description, then the
  operation's `description` holds exactly that text and nothing else in the document changes
  — in particular the `summary` is byte-identical.
- **AC2 (UC1):** Given a capability description is cleared, then the operation carries no
  `description` member.
- **AC3 (UC2):** Given an answer, when the designer confirms a description, then that
  response's `description` holds the text and nothing else changes; when cleared, the
  response's `description` is the exact derived default from FEAT-005's wording.
- **AC4 (UC3):** Given a resource, when the designer confirms a description, then the
  schema's `description` holds the text; when cleared, the member is absent — with no other
  change either way.
- **AC5 (UC1–UC3):** Given any description edit, then it is one atomic mutation, the document
  contains no Apicius-specific content, ADR-0008 counts are unchanged, and the designer's
  last-edited location moves to this API in the same transaction.

## Data / Domain

**Edited domain (OpenAPI) — link, don't reproduce.**
- [Operation Object](spec-3.1#operation-object) `description` (vs. `summary` — the label).
- [Response Object](spec-3.1#response-object) `description` (required by the spec).
- [Schema Object](spec-3.1#schema-object) `description` on the resource's schema.

**Application domain (Apicius) — describe fully.**
- Entities / fields touched: the `spec` row — `body` JSONB via the engine seam (ADR-0009);
  `last_edited_location` at the same chokepoint; ADR-0008 counts unaffected.
- Validation rules: descriptions are free prose — no length or content validation; an
  answer's description is never absent (cleared → derived default).
- States / transitions: none beyond saved-document; every edit is atomic.

## Non-Goals
- The docs-completeness signal ("2 of 5 described") — belongs to the future house-rules
  feature.
- Inheritance/override mechanics for descriptions — no such mechanics exist; each element's
  description is its own.
- Examples (`example`/`examples`) — a separate concept, edited separately (future pass).
- Renaming a capability (`summary`) or any other element.
- Describing derived furniture — the paging envelope's fields and the standard error shape
  carry fixed derived text (FEAT-010/FEAT-009).
- Per-value descriptions on "one of" parameter kinds (FEAT-011 non-goal, restated).
