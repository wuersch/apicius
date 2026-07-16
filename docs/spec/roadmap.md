# Roadmap

> Sequence & priority over time. Status lives on each feature (see `features.md`).
> Indicative, not a commitment — reorder freely.

## Next
- Create a resource — noun + chosen standard capabilities, canonically derived (FEAT-005).
- Edit a resource's shape — simple-typed fields (FEAT-006): full field editing,
  plain-language types with optional refinements, required/auto/write-only, ISO date
  formats, 'list of <simple type>'.
- Manage an API — edit details (`info`), duplicate (fork semantics), delete (FEAT-007).
- Export an API document — download as YAML/JSON, functionally equivalent output (FEAT-008).
- View a capability — its full derived contract in one plain-language place, plus the
  standard error answers, RFC 9457 (FEAT-009).
- Paging on list capabilities — built-in `page`/`limit` + pagination envelope,
  per-capability opt-out (FEAT-010).
- Query parameters & headers — freeform inputs and response headers with plain-language
  kinds (FEAT-011).
- Describe the contract — descriptions on capability, answers, and resource (FEAT-012).
- Capability add/remove on existing resources — includes datatype↔resource promotion/demotion.
- Datatype creation ("shared data" in the editor) — shapes, refined simple values, and named
  enums (inline enums on shapes stay excluded, FEAT-006).
- Complex fields and links — object fields and 'list of <named datatype>'; house rule: no
  anonymous shapes — every object shape is named.
- Example on the noun + inheritance/override (PRIN-005).
- House rules — best practices applied and explained, overridable (PRIN-006).
- Source peek (CodeMirror) + functionally-equivalent round-trip import/export (PRIN-003/004,
  extends FEAT-008); recognition of
  capabilities in imported specs (heuristics ADR; matching is extension-blind and resolves
  `$ref`s before matching).

## Later
- Validation rules on fields and shapes — pattern, length/bounds, allow-only-defined
  properties; house-rule treatment per constraint (FEAT-006 defers these).
- File upload/download capabilities — multipart bodies; binary is never an inline field
  (FEAT-006).
- Canvas map (React Flow).
- "Try it" / mock server.
- Relationships: nesting + shared-by-reference.
- User-defined vendor extensions (authoring) — source peek covers the interim;
  Apicius-originated extensions stay forbidden.
