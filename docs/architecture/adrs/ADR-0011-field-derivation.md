# ADR-0011: Field derivation — plain-language types and property names

**Date:** 2026-07-11
**Status:** Accepted
**Depends on:** PRIN-002, PRIN-006, ADR-0009, ADR-0010, FEAT-006

## Context

FEAT-006 lets a designer author a resource's fields. As with capabilities (ADR-0010), the
designer expresses intent in plain language and the schema syntax is derived — and the
document must stay free of anything Apicius-specific, so every mapping here must be
readable straight back out of standard constructs.

Two derivations are needed: the field's **kind** (plain-language type → `type`/`format`
constructs) and the field's **property name** (freeform input → a JSON property name).

## Decision

### Type model: core type + optional refinement + "list of"

A field's kind is one of a fixed set of **core types**, optionally narrowed by a
**refinement**, and either of those optionally wrapped as a **list**. This three-slot model
is the durable contract; vocabulary grows by adding rows, never by reshaping the model.

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

- **Refinements are secondary intent** (PRIN-006): the recommended default is the
  unrefined core type; picking a refinement is a deliberate act, never required to proceed.
- **Dates are formatted strings** — the house rule the table encodes: the designer says
  "Date", the document says ISO 8601, and the tool explains that when asked.
- Every row serializes identically under OpenAPI 3.0 and 3.1 — the table deliberately
  contains no dialect-divergent construct (see the exclusions below and
  `docs/architecture/patterns.md`, schema-dialect handling).

### Field attributes

- **required** → membership in the schema's `required` array.
- **auto** ("the server sets it; you never send it") → `readOnly: true` — the same
  mechanism ADR-0010's `id` uses.
- **write-only** ("you send it; the server never returns it") → `writeOnly: true`.
- auto and write-only are **mutually exclusive** — both at once describes a field nobody
  can ever see; rejected with that explanation.
- **House rule: Text as password defaults write-only on.** `format: password` alone is only
  a display hint; `writeOnly: true` is what states the server never echoes the value. The
  default is applied, explained, and overridable (PRIN-006).
- A field description → the property's `description`.

### Property-name derivation

Field-name input is freeform; the property name is derived and shown live. Derivation is
an assist, not a gate — deliberate identifier conventions are respected, never "corrected":

- Spaces are word separators and trigger camelCase joining: `First name` → `firstName`,
  `API key` → `apiKey` (an all-caps leading word is lowercased whole).
- Valid identifier characters (letters, digits, `_`) pass through: `first_name` and
  `firstName` stay exactly as typed.
- Everything else is stripped, visibly, in the live derivation.

**The derived property name is the field's identity and display.** Unlike resource names
(PascalCase is lossy; recognition must invert it), fields carry no separate display name —
nothing to reconstruct on import. Exact edge semantics (mixed space/underscore input,
casing corners) are pinned by shared cross-language test vectors, the same mechanism that
keeps ADR-0010's derivation mirror honest.

Property names are unique **case-insensitively** within a shape (including against `id`):
`firstName` next to `firstname` is legal JSON and a design smell — rejected.

### Exclusions (each with its why)

- **No byte / binary field types.** Doubly rejected: their serialization is
  dialect-divergent (3.0 `format: byte/binary` vs 3.1 `contentEncoding` /
  `contentMediaType`), and the house rule is *files are operations, not fields* — binary
  content belongs in a multipart request/response body (a future upload/download
  capability), never inline base64 in a JSON field.
- **No inline enums.** The no-anonymous-shapes house style extends to values: a set of
  fixed choices is a named datatype (the glossary's *refined simple value* — CountryCode,
  Status), arriving with datatype creation. Import still recognizes or preserves inline
  enums in foreign documents; authoring them is what's excluded.
- **No validation constraints** (`pattern`, length/bounds, `minProperties`,
  `additionalProperties`): a future validation-rules feature with house-rule treatment per
  constraint — also where the next dialect trap lives (`exclusiveMinimum` is a boolean
  modifier in 3.0, a standalone number in 3.1).

## Consequences

- The editor's type vocabulary can grow additively (a new refinement is a new table row);
  the three-slot model is what the UI is designed around, so later additions — including
  complex types, which occupy the same "kind" slot — reshape nothing.
- Field-level `readOnly`/`writeOnly` compose with ADR-0010's single-schema stance: one
  schema keeps serving request and response bodies, with per-field visibility carrying the
  request/response asymmetry.
- On import, a `(type, format)` pair outside the table is preserved and displayed as-is —
  refusal over mangling (PRIN-003), same stance as capability recognition.
- Number precision refinements (32/64-bit, float/double) admit implementation detail into
  the vocabulary; demoting them to refinements (defaulted away) is the containment.

## Alternatives Considered

- **Raw type + format dropdowns** (the Apicurio pattern: "String as Byte"). Rejected:
  that's the spec tree with combo boxes — jargon-first, exactly what PRIN-002 exists to
  avoid. The table's left column is designer language; the right column is derived.
- **Omitting precision formats entirely.** Rejected after leaning toward it: they express
  real interoperability intent (a 64-bit id crossing a JavaScript boundary is a classic
  bug), they're dialect-stable, and the tiered vocabulary keeps them out of the default
  path.
- **Inline enums now, extraction to named datatypes later.** Rejected: two representations
  of the same concept carried forever, plus a migration feature, to save one slice of
  waiting for datatype creation.
- **Forcing natural-language field names** (rejecting `_`, deriving everything). Rejected:
  property names are developer-facing identifiers and teams hold conventions (snake_case
  APIs exist); the editor derives when given prose and steps aside when given an
  identifier.
