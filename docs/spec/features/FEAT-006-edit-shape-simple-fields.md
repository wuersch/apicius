# FEAT-006: Edit a resource's shape (simple-typed fields)

**ID:** FEAT-006
**Status:** shipped
**Depends on:** PRIN-001, PRIN-002, PRIN-006, ADR-0004, ADR-0008, ADR-0009, FEAT-005
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
slot holding a core type, an optional refinement, and an optional list wrapper (§ Field
model) — is designed so they later occupy the same slot without reshaping anything.

Field mutations follow FEAT-005's pattern: one atomic document mutation through the engine
seam (ADR-0009), display projected back from the document. Fields never touch paths, verbs,
or operations — a shape edit changes the schema and nothing else.

Datatypes without capabilities don't exist yet, so in this slice every shape is a
resource's shape.

## Interaction Model
- **Entry point:** an open resource — the designer works on the noun's shape, never on a
  schema node.
- **Vocabulary:** a field's *name* is freeform; the JSON property name is derived live and
  is the field's identity and display (§ Field model). A field's *kind* is a plain-language
  core type (Text, Whole number, Decimal number, Yes/no, Date, Date & time), optionally
  refined (as email, UUID, URL, password; 32/64-bit; float/double), optionally a *list of*
  any of these — each serializing per § Field model's table. Attributes: *required*, and a
  *visibility* — normal, *auto* ("the server sets it" → `readOnly`) or *write-only*
  ("never returned" → `writeOnly`). The designer never types the serialized column.
- **Tiered vocabulary invariant:** the core types are the working vocabulary; refinements
  are secondary intent — optional, never required to proceed (PRIN-006).
- **Ordering invariant:** name first, then kind, then attributes; serialization is derived,
  never authored.
- **Projection direction:** each field edit is one atomic document mutation; what the
  editor shows is projected from the document, never echoed from the form.
- **Escape hatch:** none yet (source peek is future). The guarantee it inherits: a field
  edit writes only that property's § Field model constructs and its `required` membership,
  touching nothing else in the document.

## Field model — kinds, attributes, and names (canonical)
The derivation this feature owns (formerly ADR-0011; merged 2026-07-16 — a requirements
decision, not an architecture decision). As with capabilities (FEAT-005 § Derivation), the
designer expresses intent in plain language and the schema syntax is derived — and the
document stays free of anything Apicius-specific, so every mapping here reads straight back
out of standard constructs.

### Kind: core type + optional refinement + "list of"
A field's kind is one of a fixed set of **core types**, optionally narrowed by a
**refinement**, and either of those optionally wrapped as a **list**. This three-slot model
is the durable contract; vocabulary grows by adding rows, never by reshaping the model —
later additions, including complex types, occupy the same slot.

| Intent | Serializes to |
|---|---|
| Text | `type: string` |
| Text as email / UUID / URL / password | `type: string` + `format: email / uuid / uri / password` |
| Whole number | `type: integer` |
| Whole number as 32-bit / 64-bit | `type: integer` + `format: int32 / int64` |
| Decimal number | `type: number` |
| Decimal number as float / double | `type: number` + `format: float / double` |
| Yes / no | `type: boolean` |
| Date | `type: string` + `format: date` (ISO 8601) |
| Date & time | `type: string` + `format: date-time` (ISO 8601) |
| List of \<any row above\> | `type: array` + `items:` that row's serialization |

- **Refinements are secondary intent** (PRIN-006): the recommended default is the unrefined
  core type. Precision refinements (32/64-bit, float/double) admit implementation detail
  into the vocabulary because they express real interoperability intent (a 64-bit id
  crossing a JavaScript boundary is a classic bug); demoting them to refinements is the
  containment.
- **Dates are formatted strings** — the designer says "Date", the document says ISO 8601,
  and the tool explains that when asked.
- Every row serializes identically under OpenAPI 3.0 and 3.1 — the table deliberately
  contains no dialect-divergent construct (see `docs/architecture/patterns.md`,
  schema-dialect handling).
- On import, a `(type, format)` pair outside the table is preserved and displayed as-is —
  refusal over mangling (PRIN-003), the same stance as capability recognition.

### Attributes
- **required** → membership in the schema's `required` array.
- **visibility** — a single value, one of three: **normal** (default, serializes nothing);
  **auto** ("the server sets it; you never send it") → `readOnly: true` — the same mechanism
  FEAT-005's `id` uses; **write-only** ("you send it; the server never returns it") →
  `writeOnly: true`. Auto and write-only at once would describe a field nobody can ever see;
  carrying visibility as one value makes that state unrepresentable rather than validated
  away. Field-level `readOnly`/`writeOnly` compose with FEAT-005's single-schema stance: one
  schema serves request and response bodies, per-field visibility carrying the asymmetry.
- **House rule: Text as password defaults visibility to write-only.** `format: password`
  alone is only a display hint; `writeOnly: true` is what states the server never echoes the
  value. Applied, explained, overridable (PRIN-006).
- A field description → the property's `description`.

### Property-name derivation
Field-name input is freeform; the property name is derived and shown live. Derivation is an
assist, not a gate — deliberate identifier conventions are respected, never "corrected":

- Spaces are word separators and trigger camelCase joining: `First name` → `firstName`,
  `API key` → `apiKey` (an all-caps leading word is lowercased whole).
- Valid identifier characters (letters, digits, `_`) pass through: `first_name` and
  `firstName` stay exactly as typed.
- Everything else is stripped, visibly, in the live derivation.

**The derived property name is the field's identity and display.** Unlike resource names
(PascalCase is lossy; recognition must invert it), fields carry no separate display name —
nothing to reconstruct on import. Exact edge semantics (mixed space/underscore input, casing
corners) are pinned by shared cross-language test vectors, the same mechanism that keeps
FEAT-005's derivation mirror honest.

Property names are unique **case-insensitively** within a shape (including against `id`):
`firstName` next to `firstname` is legal JSON and a design smell — rejected.

### Exclusions (each with its why)
- **No byte / binary field types.** Doubly rejected: their serialization is
  dialect-divergent (3.0 `format: byte/binary` vs 3.1 `contentEncoding` /
  `contentMediaType`), and *files are operations, not fields* — binary content belongs in a
  multipart request/response body (a future upload/download capability), never inline base64
  in a JSON field.
- **No inline enums on datatypes.** The no-anonymous-shapes house style extends to values: a
  set of fixed choices on a *shape* is a named datatype (the glossary's *refined simple
  value* — CountryCode, Status), arriving with datatype creation. Import still recognizes or
  preserves inline enums in foreign documents; authoring them on shapes is what's excluded.
  (A parameter's fixed value set is a different context — FEAT-011.)
- **No validation constraints** (`pattern`, length/bounds, `minProperties`,
  `additionalProperties`): a future validation-rules feature with house-rule treatment per
  constraint — also where the next dialect trap lives (`exclusiveMinimum` differs between
  3.0 and 3.1).
- Rejected: raw type + format dropdowns (the Apicurio pattern — the spec tree with combo
  boxes, jargon-first); forcing natural-language names (identifier conventions like
  snake_case are respected).

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
- **Outcome:** the refined kind serializes per § Field model; a password field is write-only
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
  § Field model (with `required` membership per the choice), and no path, operation, or other
  document content changes.
- **AC2 (UC1):** Given any successful field edit, then the ADR-0008 projection's counts are
  unchanged and the designer's last-edited location moves to this API, in the same
  transaction.
- **AC3 (UC1):** Given any successful field edit, then the document contains no
  Apicius-specific content — no `x-` extensions, no constructs beyond § Field model's table.
- **AC4 (UC2):** Given a refinement or list choice, when the designer confirms, then it
  serializes exactly per § Field model (`format` on the type; `array` + `items` for lists).
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
- [Data Types](spec-3.1#data-types) — `type`/`format` pairs (§ Field model owns the mapping).

**Application domain (Apicius) — describe fully.**
- Entities / fields touched: the `spec` row — `body` JSONB mutated through the engine seam
  (ADR-0009), with the designer's `last_edited_location` written at the same chokepoint;
  ADR-0008 counts are unaffected by field edits.
- Validation rules: property name non-empty after derivation and case-insensitively unique
  within the shape (including `id`); kind from § Field model's vocabulary; visibility one of
  normal / auto / write-only; `id` immutable.
- States / transitions: none beyond saved-document; each field edit is atomic — the
  document is never persisted mid-edit.

## Non-Goals
Named explicitly for the design handoff: the mockup should leave visual room for these —
a field row and kind slot they can later occupy — without designing them.

- **Complex kinds** — object fields, links to named datatypes, "list of \<named
  datatype\>"; they need datatype creation first. House rule already fixed for then: no
  anonymous shapes — every object shape is named.
- **Enums** — always *named* datatypes (refined simple values: Status, CountryCode),
  arriving with datatype creation; inline enum authoring on shapes is permanently excluded
  (§ Field model).
- **File / binary fields** — files are operations, not fields (multipart bodies via a
  future upload/download capability); byte/binary never appear as field kinds
  (§ Field model).
- **Validation constraints** — pattern, length/bounds, min/max properties,
  allow-only-defined-properties: a future validation-rules feature, one house-rule
  treatment per constraint.
- **Field reordering** — new fields append; document order is preserved as-is.
- **Examples and per-field documentation beyond description** — examples attach to the
  noun and flow down (PRIN-005), a future feature.
- **Editing shapes of non-resource datatypes** — datatypes don't exist yet.
