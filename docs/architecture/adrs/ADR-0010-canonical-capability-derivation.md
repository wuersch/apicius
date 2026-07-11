# ADR-0010: Canonical capability derivation

**Date:** 2026-07-08
**Status:** Accepted
**Depends on:** PRIN-001, PRIN-002, PRIN-006, ADR-0009, FEAT-005

## Context

FEAT-005 creates a resource together with its chosen standard capabilities, so the deterministic
mapping from capability to operation (the glossary's *Derivation*) needs a canonical definition.
The same table is the foundation for round-trip recognition: Apicius adds nothing
Apicius-specific to the document (no `x-` vendor extensions — exported specs must be
indistinguishable from well-crafted hand-written ones), so recognizing a capability in a stored
spec means running this derivation backwards. Structure itself is the marker.

An ontology decision frames the table: a **datatype** is a named shape (a schema); a
**resource** is a datatype with identity and at least one capability. Resource-ness is therefore
always structurally visible — no marker is ever needed for it.

## Decision

For a resource named `X` (e.g. `Product`), the five standard capabilities derive as follows.
The schema name is the PascalCase noun; the collection path is the kebab-case plural
(`Product` → `/products`, `Order item` → `OrderItem`, `/order-items`), produced by a small
built-in English pluralizer — an uncovered irregular yields a wrong-but-consistent plural until
a per-resource path override arrives (deliberately deferred).

| Capability (label)    | Operation              | Success                    | Failure | `operationId`    |
|-----------------------|------------------------|----------------------------|---------|------------------|
| Browse all products   | `GET /products`        | 200 — paged wrapper of `X` | —       | `listProducts`   |
| Look up one product   | `GET /products/{id}`   | 200 — `X`                  | 404     | `getProduct`     |
| Add a product         | `POST /products`       | 201 — `X`                  | —       | `createProduct`  |
| Update a product      | `PATCH /products/{id}` | 200 — `X`                  | 404     | `updateProduct`  |
| Remove a product      | `DELETE /products/{id}`| 204 — empty                | 404     | `deleteProduct`  |

- **The capability label is the operation's `summary`** — this is the round-trip carrier of the
  plain-language name; recognition reads it back, and a missing/edited summary degrades only the
  label, never the structural match.
- **Identity house rule:** every resource's schema carries `id` — `type: string`, `readOnly:
  true`, required. Opaque string, *not* `format: uuid`: string is the non-breaking superset
  (every UUID is a valid string; tightening later is compatible, loosening is not), and the
  floor being enforced is "never sequential integers". `readOnly` lets one schema serve request
  and response bodies — the server assigns identity, so `id` is absent from what clients send.
- **Bodies:** all request/response bodies are `application/json` referencing
  `#/components/schemas/X` — except Update, whose request media type is
  `application/merge-patch+json` (RFC 7386) referencing `X`. That the referenced schema still
  lists required fields is an accepted v1 imprecision; a derived partial shape can replace it
  later without changing the operation's structure.
- **Paged wrapper:** Browse's 200 body is an **inline** object `{ items: [X] }`, never a bare
  array. The wrapper's v1 job is evolvability, not pagination — page fields and parameters can
  be added to it later without a breaking change, which a bare array cannot offer. It is inline
  (not a named schema) so `components/schemas` contains only user-meaningful datatypes — the
  shared-data view projects straight from it.
- **Failure responses:** 404 on the `{id}` paths, declared with a plain-language description
  and no body schema in v1 — the error-body model (e.g. RFC 9457) is a separate future house
  rule.
- **Spec-required completions** (a valid, well-crafted document forces three constructs the
  table's cells don't show): the `{id}` path parameter, declared once at the item path-item
  level (`name: id, in: path, required: true, schema: {type: string}`); `required: true` on the
  Add/Update request bodies; and a plain-language `description` on every response, phrased from
  the noun ("The list of products.", "No product with this id exists.") and owned by the
  canonical derivation so recognition and tests stay stable.
- Derivation writes exactly these constructs and nothing else — no tags, no extensions, no
  scaffolding elsewhere in the document.

## Consequences

- **Recognition is derivation inverted.** An Apicius-authored spec is recognized by structure
  alone (method + path shape + schema linkage); heuristic recognition is only ever needed for
  imported specs, and it *refuses rather than mangles* — an operation that doesn't match the
  table stays a preserved, unrecognized operation (PRIN-003), never a force-fitted capability.
- **RPC is second-class, not foreclosed.** The table is additive: custom actions ("Cancel an
  order" → `POST /orders/{id}/cancel`) are a planned taxonomy extension, and noun-less
  capabilities remain representable. No invariant "every operation belongs to a resource" may
  be baked into the model or projections — an imported spec's `operation_count` (ADR-0008)
  counts *all* operations, not just recognized ones.
- PATCH-only update is a deliberate nudge away from the PUT-vs-PATCH confusion; adding a
  Replace capability later is an additive table row.
- Path derivation couples to the pluralizer; renaming that coupling away (path overrides) is a
  known future feature, and the deferral is visible, not silent.
- `{id}` as the parameter name keeps canonical paths minimal; when nested resources arrive
  (roadmap "Later"), outer parameters will need resource-qualified names (`{orderId}`) — an
  additive naming rule for that ADR.

## Alternatives Considered

- **`x-apicius-*` markers as the capability/resource source of truth.** Rejected: taints
  exports, undercuts the thesis that standardized structure is itself the identifier. If lens
  state ever genuinely can't be inferred structurally, a DB wrapper entity pointing into the
  document is the fallback (own ADR needed — it deviates from ADR-0004 and pointers risk
  dangling on renames); extensions remain the last resort.
- **`format: uuid` as the default identity type.** Rejected: the spec is a contract, and uuid
  commits every future implementation to UUIDs (foreclosing prefixed ids or slugs). Notable
  style guides caution against UUID-by-default; uuid stays a one-step tightening.
- **PUT (Replace) in the standard set.** Rejected for v1 — five capabilities match the design
  reference, and offering only PATCH nudges toward its more efficient use.
- **Bare arrays for list responses.** Rejected: not evolvable; the design reference's
  guidelines rail treats them as a violation.
- **Named wrapper schemas (`ProductList`) in `components/schemas`.** Rejected: they would
  appear as datatypes in the shared-data projection; wrappers are derived plumbing, not
  user-declared shapes.
