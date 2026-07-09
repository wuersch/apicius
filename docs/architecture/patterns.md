# Patterns

> Recurring **implementation** patterns — where a problem has several valid solutions, the
> codebase picks **one** and applies it everywhere. Each entry: the problem, the chosen approach,
> and the decision it follows. Snippets are illustrative.
>
> Entries are added here once a pattern is genuinely settled (e.g. how pagination is implemented),
> not before — an unsettled convention belongs in the ADR or feature that decides it.

## Schema-dialect handling in the document adapter

**Problem.** OpenAPI 3.0 uses its own schema dialect while 3.1+ adopts JSON Schema 2020-12, so
the engine's generated models differ per version — e.g. `type` is a plain string setter on the
3.0 schema but a string-or-array union on 3.1/3.2.

**Chosen approach.** Derivation writes only constructs in the *intersection* of the dialects,
so the serialized output is identical across versions; the per-version model difference is
absorbed by a single private helper in the apitomy adapter (`ApitomyDocumentEngine.setType`),
covered by a 3.0/3.1/3.2 test matrix. No strategy abstraction — its interface would be designed
before its real client (field/shape editing) exists.

**Standing re-evaluation rule.** Every feature that extends schema writing must reconsider
promoting the helper to a version-selected schema-writer strategy. The trigger is the first
construct whose *serialized output* must differ by dialect (nullability, type arrays, formats)
— not merely another setter-shape branch. Extending the helper past that trigger is the
anti-pattern this entry exists to prevent.

**Follows** ADR-0009 (everything stays inside the apitomy adapter, behind the `DocumentEngine`
seam) and FEAT-005 (the first schema writer).
