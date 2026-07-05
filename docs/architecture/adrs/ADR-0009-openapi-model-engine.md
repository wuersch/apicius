# ADR-0009: OpenAPI document model, edit, and validation engine

**Date:** 2026-07-04
**Status:** Accepted
**Depends on:** ADR-0002, ADR-0004, PRIN-003

## Context

ADR-0002 puts the backend in charge of "the superset OpenAPI domain model, parse/serialize, the
rules engine" — but leaves *what implements them* open. The techstack currently carries two
placeholders: **parse/serialize** as "a Java library (e.g. swagger-parser) + a custom lossless
layer", and the **rules engine** as "custom pure-function checks". This ADR resolves both.

What the backend actually needs is more than a parser:

1. An **object model** for OpenAPI 2.0 / 3.0.x / **3.1 / 3.2** (the specs we target), navigable in
   memory — the domain model ADR-0002 assigns here.
2. **Traversal** to *derive* the concept-first view — resources and plain-language capabilities
   (PRIN-001, PRIN-002) — and the counts behind the summary projection (ADR-0008). "Resource" is
   not a spec node; it's computed by walking the document.
3. An **edit protocol** fit for a collaborative web editor: mutations that are serializable over
   the management API, applied server-side, and **undoable**.
4. A **validation / rules engine** to back house rules (PRIN-006) — deterministic, explainable,
   with per-node problems and tunable severity.
5. **Lossless round-trip** (PRIN-003, ADR-0004): unknown or oddly-placed nodes survive
   import → edit → export untouched.

Apicius is inspired by **Apicurio Studio**, which was built on exactly this library. It has since
migrated `Apicurio → Apitomy` and remains actively maintained (release 3.1.1, Jun 2026; 62
releases; 284 validation rules; a 94-command mutation layer). Rather than reassemble those five
capabilities from a bare parser, adopt the library that already is them.

## Decision

Adopt **`io.apitomy:apitomy-data-models`** (Apache-2.0) as the backend's OpenAPI/AsyncAPI
**document model, edit engine, and validation engine**. Scope it to the **backend only** — it is
the concrete implementation of the model authority ADR-0002 already located server-side. What we
use, mapped to the need above:

- **Model + parse/serialize** — `Library.readDocumentFromJSONString` / `writeDocumentToJSONString`
  over the generated node hierarchy (`Document → Paths → PathItem → Operation → Schema …`). The
  serialized JSON is what persists as the `body JSONB` (ADR-0004); no separate parser.
- **Traversal → the concept projection** — `IVisitor` + `Library.visitTree(node, v, direction)`
  and the built-in finders (`OperationFinder`, `PathItemFinder`, `ConsumesProducesFinder`) build
  the resources/capabilities view and the ADR-0008 counts in one walk. `Library.createNodePath` /
  `NodePath.resolve` give a stable pointer to round-trip a capability card ↔ its spec location.
- **Edit protocol** — the **command system** (`ICommand` / `CommandFactory`, JSON-serializable
  payloads). The React client emits a command; a service applies it to the in-memory model and
  re-serializes (ADR-0003 write chokepoint); we get **undo/redo** and a clean mutation contract
  for free, instead of hand-mutating a tree.
- **Rules engine** — the built-in validation rules + `IValidationSeverityRegistry` are the base of
  PRIN-006 house rules; problems attach per node (`getValidationProblemsFor`). Apicius-specific
  guidelines extend this rather than start from zero.
- **Version transforms** — `transform/` (2.0→3.0→3.1→3.2 visitors) give spec-upgrade capability.

The library is consumed **behind a thin Apicius-owned seam** — the `dev.apicius.document`
package's `DocumentEngine` interface; only the `document.apitomy` adapter may import
`io.apitomy.*`. The seam keeps the engine swappable (the bus-factor trade-off below) and grows
method-by-method as features need it: creation (FEAT-003) first; parse/validate (FEAT-004) and
commands/traversal (editor) when they land.

The library **stays out of the browser.** Its TypeScript build is JSweet-transpiled from Java —
Java-shaped and un-idiomatic. The React app holds only view state (ADR-0002): it consumes derived
projections and emits command JSON over the management API. This reinforces, rather than bends, the
decoupling in ADR-0002.

**Lossless round-trip** (PRIN-003) of unknown / oddly-placed properties against representative
real-world specs remains to be exercised — it is FEAT-004's AC2, the first feature to import
anything. Acceptance does not hinge on it: any preservation gap the check finds is absorbed by the
`body JSONB` **preservation bag** ADR-0004 already provides; the check determines how much the bag
must carry, not whether the engine stays.

## Consequences

- Collapses **two** proposed techstack items (parse/serialize + rules engine) into **one**
  maintained dependency, and throws in a mutation/undo layer we had not scoped.
- Continuity with the Apicurio Studio lineage: the model Apicius is conceptually descended from,
  now maintained under Apitomy.
- The command system makes collaborative editing and undo/redo a library concern, not bespoke code.
- **Trade-off — bus factor:** small community, effectively one maintainer's lineage. Mitigated by
  Apache-2.0 (forkable) and by the boundary above — we depend on a published artifact, not its
  internals.
- **Trade-off — closed, generated model:** node classes are code-generated (the `umg` generator),
  so the model is extended by regenerating, not subclassing. Acceptable: Apicius *projects away*
  from the spec tree (PRIN-001/002) rather than extending it.
- We consume the **published Maven artifact**; no generator/transpile step enters our build.

## Alternatives Considered

- **swagger-parser + custom lossless layer + custom rules + custom mutation/undo** (the prior
  techstack placeholder). Mature *parsing*, but no editing model, no command/undo, no rules engine,
  and a hand-built preservation layer — three subsystems to write and maintain versus one to adopt.
  Rejected as far more bespoke work for a weaker result.
- **Hand-rolled domain model.** Maximum fit, maximum cost; re-invents traversal, validation, and
  round-trip that this library already ships. Rejected.
- **Other Java OpenAPI models (openapi4j, KaiZen-OpenApi-Parser).** Less complete spec coverage
  (little/no 3.1+), no editing/command layer, and thinly maintained. Rejected.
