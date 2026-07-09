# FEAT-005: Create a resource

**ID:** FEAT-005
**Status:** building
**Depends on:** PRIN-001, PRIN-002, PRIN-006, ADR-0004, ADR-0008, ADR-0009, ADR-0010
**Mockup:** `docs/design/mockups/launcher-hybrid-v6.html` — View 2 (editor, resource cards);
the creation-flow view is pointed to by the user before/during build.

> **Type:** Deterministic — behavior is fixed and verifiable. Every requirement maps to a
> pass/fail test.

## User Story
As an API designer, I want to add a resource to my API by naming the noun and choosing what
people can do with it, so that my API gains real structure without me authoring paths, verbs,
or schema syntax.

## Context / Notes
The first content-creating feature: every API is empty after FEAT-003, and this is the first
mutation of a document body beyond seeding it (extends the ADR-0009 engine seam past creation).
A **resource** is a datatype with identity and at least one capability (see glossary); creating
one therefore always creates capabilities with it, derived per ADR-0010. Creating a plain
datatype (no capabilities — the UI's "shared data") is a separate future feature, as is editing
a resource after creation. With creation comes the minimal honest display: an empty API states
that it has no resources yet, and created resources are visible with their capabilities — full
editor-view fidelity grows across later features.

## Interaction Model
- **Entry point:** an open API's resource context. Creating a resource is a distinct intent
  from creating a datatype; the designer starts from the noun, never from a path, verb, or
  schema node.
- **Vocabulary:** "Name" (the noun, e.g. *Product*) → the schema name and, pluralized, the
  collection path; "Description" → the schema's `description`; each offered capability is a
  plain-language label ("Browse all products", "Look up one product", "Add a product", "Update
  a product", "Remove a product") → its ADR-0010 operation. The identity field ("identifier")
  → `id: string, readOnly`. The user never types the right-hand column.
- **Ordering invariant:** noun first, then capabilities, then serialization — derivation is
  automatic and deterministic (ADR-0010). No operation exists before its resource.
- **Projection direction:** confirming creation derives schema + paths into the document in one
  atomic mutation; what the editor then shows is projected from the document, not echoed from
  the input. The derived document contains nothing Apicius-specific.
- **Escape hatch:** none yet — source peek is a future feature. The guarantee it will inherit:
  creation writes only the ADR-0010 constructs and touches nothing else in the document.

## Use Cases

### UC1: Create a resource with the standard capabilities (happy path)
- **Precondition:** designer has an API open; it may have zero or more resources.
- **Flow:** starts resource creation; names the noun (optionally describes it); keeps all five
  pre-selected standard capabilities; confirms.
- **Outcome:** the API contains the resource with its identity field and all five capabilities;
  the designer sees it listed with what people can do with it.

### UC2: Create a resource with a subset of capabilities (alternate)
- **Precondition:** as UC1; the designer's API shouldn't allow one or more of the standard
  actions (e.g. nothing may ever be removed).
- **Flow:** starts resource creation; names the noun; deselects the unwanted capabilities
  (deselecting is the deliberate override of the recommended default, PRIN-006); confirms.
- **Outcome:** only the chosen capabilities exist; no trace of the deselected ones — and no
  empty leftovers — appears in the document.

### UC3: Invalid name (failure)
- **Precondition:** on resource creation.
- **Flow:** attempts to confirm with an empty name, or a name already used by a resource or
  datatype in this API.
- **Outcome:** creation is blocked with a message naming the problem; nothing is persisted.

### UC4: No capability selected (failure)
- **Precondition:** on resource creation.
- **Flow:** deselects all five capabilities and attempts to confirm.
- **Outcome:** creation is blocked; the message explains that a resource is something people
  can act on — at least one capability is required. Nothing is persisted.

## Acceptance Criteria
- **AC1 (UC1):** Given a valid name and all five capabilities selected, when the designer
  confirms, then the document gains the schema (with required, read-only `id: string`) and the
  collection + item paths carrying all five operations exactly per ADR-0010, each operation's
  `summary` being the capability's label.
- **AC2 (UC1):** Given a creation succeeds, then the ADR-0008 projection reflects it in the
  same transaction (`resource_count` +1, `operation_count` + number of chosen capabilities) and
  the designer's last-edited location moves to this API.
- **AC3 (UC1):** Given a creation succeeds, then the document contains no Apicius-specific
  content — no `x-` extensions, no constructs beyond ADR-0010's table.
- **AC4 (UC2):** Given only a subset of capabilities is selected, when the designer confirms,
  then exactly the chosen operations are derived, and a path item with no chosen operation does
  not exist at all.
- **AC5 (UC3):** Given an empty name, when the designer attempts to confirm, then creation is
  rejected with a "name required" message and nothing is persisted.
- **AC6 (UC3):** Given the name is already used by a resource or datatype in this API, when the
  designer attempts to confirm, then creation is rejected with a message naming the conflict
  and nothing is persisted.
- **AC7 (UC4):** Given no capability is selected, when the designer attempts to confirm, then
  creation is rejected with a message explaining the at-least-one-capability rule and nothing
  is persisted.
- **AC8 (UC1):** Given an API with no resources, when the designer opens it, then the editor
  states that plainly and offers resource creation; after AC1 the resource is shown with its
  capabilities in plain language, derived detail de-emphasized.

## Data / Domain

**Edited domain (OpenAPI) — link, don't reproduce.**
- [Schema Object](spec-3.1#schema-object) — the resource's shape under `components/schemas`.
- [Paths Object](spec-3.1#paths-object) / [Path Item Object](spec-3.1#path-item-object) /
  [Operation Object](spec-3.1#operation-object) — the derived operations (ADR-0010 owns the
  mapping).

**Application domain (Apicius) — describe fully.**
- Entities / fields touched: the `spec` row — `body` JSONB mutated through the engine seam
  (ADR-0009; first mutation method beyond document creation), with the ADR-0008 projection
  columns and the designer's `last_edited_location` (API-level in v1) written at the same
  chokepoint.
- Validation rules: name non-empty, unique among this API's schemas, and cleanly derivable to a
  schema name and path (ADR-0010 naming rules); at least one capability selected.
- States / transitions: none beyond saved-document; creation is atomic — the document is never
  persisted with a partially derived resource.

## Non-Goals
- Field / shape editing (types, links, required, formats) — next slice; the resource is born
  with its identity field only.
- Datatype creation ("shared data") and datatype↔resource promotion/demotion — emergent from
  capability add/remove, both future features.
- Adding/removing capabilities after creation; rename/delete resource.
- Capabilities beyond the standard five (Replace, Search, custom actions) — future taxonomy
  extensions per ADR-0010.
- Custom paths, pagination parameters, error-body modeling — deferred house rules (ADR-0010).
- Recognition of resources in imported documents — the import-derivation feature.
- Full View 2 display fidelity (nav sections, guidelines rail, counts footer) — the display
  grows with each content feature; this one ships only the minimal honest display (AC8).
