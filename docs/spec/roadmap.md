# Roadmap

> Sequence & priority over time. Status lives on each feature (see `features.md`).
> Indicative, not a commitment — reorder freely.

## Next
- Create a resource — noun + chosen standard capabilities, derived per ADR-0010 (FEAT-005).
- Shape editing (fields, links, required/auto, ISO formats; house rule: no anonymous shapes —
  every object shape is named, array fields are 'list of <named datatype | simple type>').
- Capability add/remove on existing resources — includes datatype↔resource promotion/demotion.
- Datatype creation ("shared data" in the editor).
- Example on the noun + inheritance/override (PRIN-005).
- House rules — best practices applied and explained, overridable (PRIN-006).
- Source peek (CodeMirror) + lossless round-trip import/export (PRIN-003/004); recognition of
  capabilities in imported specs (heuristics ADR; matching is extension-blind and resolves
  `$ref`s before matching).

## Later
- Canvas map (React Flow).
- "Try it" / mock server.
- Relationships: nesting + shared-by-reference.
- User-defined vendor extensions (authoring) — source peek covers the interim;
  Apicius-originated extensions stay forbidden.
