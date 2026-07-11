# FEAT-006: Edit a resource's shape (simple-typed fields)

**ID:** FEAT-006
**Status:** building
**Depends on:** PRIN-001, PRIN-002, PRIN-006, ADR-0004, ADR-0008, ADR-0009, ADR-0011
**Mockup:** `docs/design/mockups/launcher-hybrid-v7.html` — views 2c (add a field),
2d·1–2d·6 (field-editor states)

> **Type:** Deterministic — behavior is fixed and verifiable. Every requirement maps to a
> pass/fail test.

## User Story
As an API designer, I want to give a resource its fields by naming each in plain language
and saying what kind of data it holds, so that my API's data model takes real shape without
me authoring schema syntax.

## Context / Notes
After FEAT-005 a resource is born with only its identity field; this slice adds the rest of
its shape. Scope is **simple-typed fields with full field editing** — add, change (name,
kind, attributes, description), and remove. Complex kinds (object fields, links to named
datatypes, enums) are deliberately deferred (see Non-Goals), but the field model — a kind
slot holding a core type, an optional refinement, and an optional list wrapper (ADR-0011) —
is designed so they later occupy the same slot without reshaping anything.

Field mutations follow FEAT-005's pattern: one atomic document mutation through the engine
seam (ADR-0009), display projected back from the document. Fields never touch paths, verbs,
or operations — a shape edit changes the schema and nothing else.

Datatypes without capabilities don't exist yet, so in this slice every shape is a
resource's shape.

## Interaction Model
- **Entry point:** an open resource — the designer works on the noun's shape, never on a
  schema node.
- **Vocabulary:** a field's *name* is freeform; the JSON property name is derived live and
  is the field's identity and display (ADR-0011). A field's *kind* is a plain-language core
  type (Text, Whole number, Decimal number, Yes/no, Date, Date & time), optionally refined
  (as email, UUID, URL, password; 32/64-bit; float/double), optionally a *list of* any of
  these — each serializing per ADR-0011's table. Attributes: *required*, and a
  *visibility* — normal, *auto* ("the server sets it" → `readOnly`) or *write-only*
  ("never returned" → `writeOnly`). The designer never types the serialized column.
- **Tiered vocabulary invariant:** the core types are the working vocabulary; refinements
  are secondary intent — optional, never required to proceed (PRIN-006).
- **Ordering invariant:** name first, then kind, then attributes; serialization is derived,
  never authored.
- **Projection direction:** each field edit is one atomic document mutation; what the
  editor shows is projected from the document, never echoed from the form.
- **Escape hatch:** none yet (source peek is future). The guarantee it inherits: a field
  edit writes only that property's ADR-0011 constructs and its `required` membership,
  touching nothing else in the document.

## Use Cases

### UC1: Add a field (happy path)
- **Precondition:** designer has a resource open.
- **Flow:** adds a field; names it in plain language (property name derived live, e.g.
  "First name" → `firstName`); picks a core type; keeps defaults; confirms.
- **Outcome:** the resource's shape shows the field with its plain-language kind; the
  document's schema carries the property.

### UC2: Refine a field's kind (alternate)
- **Precondition:** adding or changing a field.
- **Flow:** narrows the kind with a refinement (e.g. Text as email) or wraps it as a list
  (e.g. list of UUIDs). Choosing *Text as password* applies the write-only house rule —
  visibly, with its explanation — which the designer may override.
- **Outcome:** the refined kind serializes per ADR-0011; a password field is write-only
  unless deliberately overridden.

### UC3: Change a field (alternate)
- **Precondition:** the resource has a field beyond `id`.
- **Flow:** renames it, changes its kind, its required or visibility, or edits its
  description; confirms.
- **Outcome:** the property is rewritten in place — its `required` membership follows it —
  and nothing else in the document changes. The identity field is exempt: `id` is shown but
  cannot be renamed, retyped, or removed.

### UC4: Remove a field (alternate)
- **Precondition:** the resource has a field beyond `id`.
- **Flow:** removes it; confirms.
- **Outcome:** the property and its `required` entry are gone without other trace; removing
  the last such field is fine — the shape falls back to `id` alone.

### UC5: Invalid or conflicting name (failure)
- **Precondition:** adding or renaming a field.
- **Flow:** attempts to confirm with a name that derives to nothing (empty after
  stripping), or one that collides case-insensitively with an existing field of this shape
  (including `id`).
- **Outcome:** the edit cannot be completed; a message names the problem; nothing is
  persisted.

## Acceptance Criteria
- **AC1 (UC1):** Given a resource and a valid field name and core type, when the designer
  confirms, then the document's schema gains exactly that property serialized per
  ADR-0011 (with `required` membership per the choice), and no path, operation, or other
  document content changes.
- **AC2 (UC1):** Given any successful field edit, then the ADR-0008 projection's counts are
  unchanged and the designer's last-edited location moves to this API, in the same
  transaction.
- **AC3 (UC1):** Given any successful field edit, then the document contains no
  Apicius-specific content — no `x-` extensions, no constructs beyond ADR-0011's table.
- **AC4 (UC2):** Given a refinement or list choice, when the designer confirms, then it
  serializes exactly per ADR-0011 (`format` on the type; `array` + `items` for lists).
- **AC5 (UC2):** Given the kind *Text as password*, then visibility defaults to
  write-only, shown as an applied, explained rule; overriding it yields
  `format: password` without `writeOnly`.
- **AC6 (UC3):** Given a rename, retype, attribute, or description change, when the
  designer confirms, then the property is rewritten in place, its `required` membership
  follows the field, and no other property or document content changes.
- **AC7 (UC3, UC4):** Given the identity field `id`, then no rename, retype, attribute
  change, or removal can be performed on it.
- **AC8 (UC4):** Given a field's removal, when the designer confirms, then its property and
  `required` entry are absent with no other change; a shape reduced to `id` alone remains
  valid.
- **AC9 (UC5):** Given a name empty after derivation or case-insensitively equal to an
  existing field's property name (including `id`), then the edit cannot be completed, a
  message names the problem, and nothing is persisted.
- **AC10 (UC1, UC3):** Given any field edit Apicius accepts, then the persisted property
  carries at most one of `readOnly` / `writeOnly` — a field is never both auto and
  write-only, which would make it visible to no one.
- **AC11 (UC1):** Given a resource's shape is displayed, then each field shows its
  plain-language kind and attributes, derived detail de-emphasized, and `id` is visibly
  present as the read-only identity field.

## Data / Domain

**Edited domain (OpenAPI) — link, don't reproduce.**
- [Schema Object](spec-3.1#schema-object) — `properties`, `required`, `readOnly`,
  `writeOnly`, `description` on the resource's schema.
- [Data Types](spec-3.1#data-types) — `type`/`format` pairs (ADR-0011 owns the mapping).

**Application domain (Apicius) — describe fully.**
- Entities / fields touched: the `spec` row — `body` JSONB mutated through the engine seam
  (ADR-0009), with the designer's `last_edited_location` written at the same chokepoint;
  ADR-0008 counts are unaffected by field edits.
- Validation rules: property name non-empty after derivation and case-insensitively unique
  within the shape (including `id`); kind from ADR-0011's vocabulary; visibility one of
  normal / auto / write-only (ADR-0011); `id` immutable.
- States / transitions: none beyond saved-document; each field edit is atomic — the
  document is never persisted mid-edit.

## Non-Goals
Named explicitly for the design handoff: the mockup should leave visual room for these —
a field row and kind slot they can later occupy — without designing them.

- **Complex kinds** — object fields, links to named datatypes, "list of \<named
  datatype\>"; they need datatype creation first. House rule already fixed for then: no
  anonymous shapes — every object shape is named.
- **Enums** — always *named* datatypes (refined simple values: Status, CountryCode),
  arriving with datatype creation; inline enum authoring is permanently excluded
  (ADR-0011).
- **File / binary fields** — files are operations, not fields (multipart bodies via a
  future upload/download capability); byte/binary never appear as field kinds (ADR-0011).
- **Validation constraints** — pattern, length/bounds, min/max properties,
  allow-only-defined-properties: a future validation-rules feature, one house-rule
  treatment per constraint.
- **Field reordering** — new fields append; document order is preserved as-is.
- **Examples and per-field documentation beyond description** — examples attach to the
  noun and flow down (PRIN-005), a future feature.
- **Editing shapes of non-resource datatypes** — datatypes don't exist yet.
