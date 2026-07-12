# ADR-0008: Derived summary projection for list views

**Date:** 2026-07-02
**Status:** Accepted
**Depends on:** ADR-0004, ADR-0002, FEAT-002

## Context

The "My APIs" home (FEAT-002) renders a card per API, each showing facts *about* the spec: title,
description, API version, resource and operation counts, and how long ago it was last edited. Specs
persist as opaque JSONB documents (ADR-0004), and the list is a hot, frequently-loaded surface that
must stay fast as both the number of APIs per user and the size of each document grow — this is an
open-source product, so we plan for many users × many specs.

Two of the displayed facts are the problem. **`operation_count`** is `sum over paths of the number of
HTTP-method keys` — an aggregation over the document's nested structure, awkward to express as an
indexable SQL/JSON path (a path item also holds `parameters`, `summary`, `$ref`, …, so a naive
`$.paths.*.*` over-counts). **`resource_count`** isn't in the raw spec *at all* — "resource" is an
Apicius-derived concept, so the application must compute it regardless of how it's stored. Reading the
entire multi-KB `body JSONB` on every card render, just to extract a few scalars and recompute counts,
is wasteful on exactly the path where latency is most visible.

## Decision

Maintain a **denormalized summary projection** alongside the authoritative document — the "metadata
columns" ADR-0004 already anticipates on the `spec` table:

- Projection fields: `title`, `description`, `api_version`, `spec_version` (the document's
  `openapi` string — FEAT-007 shows it locked), `resource_count`, `operation_count`,
  `updated_at`, `owner`, and a per-user **last-edited-location** (the API and, when available, the
  capability last edited — powers FEAT-002's jump-back-in).
- The backend **writes the projection atomically on every create / import / save**, in the same
  transaction as the `body JSONB`. The server already owns parse + derive (ADR-0002), so keeping the
  projection in sync is a single service-layer chokepoint, not a distributed concern. Scalar fields
  may be `GENERATED … STORED` columns or expression indexes; the counts are app-derived at write time.
- The home list query reads **only** the projection columns; it never deserializes `body JSONB`.

The projection is **additive**: a derived, rebuildable read-model over the untouched document. It is
not a decomposition of the spec — the JSONB body remains the single, undecomposed source of truth
(ADR-0004), so lossless round-trip (PRIN-003) is unaffected. If the projection is ever lost or its
derivation rules change, it can be recomputed from the documents.

## Consequences

- The list stays fast as documents grow — the query touches narrow columns, never the heavy body.
- Sync is one code path (the write chokepoint), not a background job or a cross-store reconciliation.
- `resource_count` couples to how Apicius derives resources — trivial for authored specs, and the
  rule for *imported* specs is owned by a separate future feature (resource extraction); until that
  lands, an imported-but-unmodeled spec's `resource_count` is whatever that derivation yields.
- A schema change adds columns to `spec` (Phase-1 drop-and-create early; a Flyway migration once the
  schema stabilises, per ADR-0004).

## Alternatives Considered

- **Compute-on-read from JSONB.** No extra columns, but it re-scans nested structure per card, can't
  cheaply index the operation count, and *still* can't produce `resource_count` (not in the spec).
  Rejected — doesn't scale and doesn't even solve the hardest field.
- **Materialized view.** Postgres materialized views need manual/scheduled refresh and don't update
  per-row, so the list would show stale counts right after an edit. Rejected — write-time columns give
  immediate freshness with less machinery.
