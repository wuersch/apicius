# ADR-0004: Persistence — storage model, conventions, and migrations

**Date:** 2026-06-26
**Status:** Accepted
**Depends on:** ADR-0003

## Context
The backend persists user specs. The core artifact is a single large, deeply nested,
version-spanning OpenAPI document that must round-trip **losslessly** (PRIN-003). So the primary
decision is the **storage model** — how a spec is laid out at rest — not just naming conventions.

Alongside the documents there is genuinely **relational** data: users, ownership/sharing, and an
audit trail. We need a strategy that fits the document shape, keeps cross-cutting concerns
(audit, authorization) transactional with the data they describe, and supports fast early
iteration with production safety later.

The engine is **PostgreSQL** (via Hibernate ORM Panache) — chosen on the merits below, not
inherited. A single engine holds both the document-shaped specs (JSONB) and the relational data.

## Decision

### Storage model
- A spec persists as a **JSONB document** — the parsed superset model plus the lossless
  preservation bag — **not** shredded into per-node relational tables. `resource`, `operation`,
  `schema`, etc. are **nodes inside that document**, never tables. This keeps round-trip faithful
  by construction: nothing is decomposed, so nothing is lost reassembling it (PRIN-003).
- **Relational tables only** for what is genuinely relational and queried *across* specs:
  `app_user`, `spec` (metadata columns + a `body JSONB` holding the document), ownership/sharing,
  `audit_log`. The JSONB body is opaque to foreign keys — cross-spec queries use the metadata
  columns and selective indexes.
- **Selective indexing:** add GIN / expression indexes on the JSONB body only where a query
  pattern demands it; otherwise the body is opaque storage and queries hit metadata columns.

### Naming
| Element | Convention | Example |
|---|---|---|
| Tables | `snake_case`, singular | `app_user`, `spec`, `audit_log` |
| Columns | `snake_case` | `created_at`, `owner_id` |
| Primary keys | `id`, type `UUID` | `id UUID DEFAULT gen_random_uuid() PRIMARY KEY` |
| Foreign keys | `{referenced_table}_id` | `spec_id`, `owner_id` |
| Join tables | `{table1}_{table2}` (alphabetical) | `spec_user` (sharing) |
| Indexes | `idx_{table}_{columns}` | `idx_audit_log_spec_id` |
| Unique constraints | `uq_{table}_{columns}` | `uq_app_user_oidc_subject` |
| Check constraints | `ck_{table}_{description}` | `ck_spec_status_valid` |

### Standard columns
Every table: `id` (UUID), `created_at`, `updated_at` (`TIMESTAMPTZ NOT NULL DEFAULT now()`).
Mutable entities also: `version INTEGER NOT NULL DEFAULT 0` (optimistic locking via JPA `@Version`).

### Enum storage
Enums as `VARCHAR` + check constraint, **not** PostgreSQL `ENUM` types (which need `ALTER TYPE`
and can't drop values — migration-hostile).

### JSONB
Used for two things: the **spec body** (the document model + preservation bag — see Storage model)
and **flexible snapshots without schema coupling** (e.g. `audit_log.before_state`/`after_state`).
Not for relational data that needs indexing or foreign keys — that gets real columns.

### Migrations — two phases
- **Phase 1 (early):** Hibernate `drop-and-create`; Flyway disabled; change entity, restart;
  seed via `import.sql`.
- **Phase 2 (stabilised):** `generation=none` + Flyway; a baseline captures the Phase-1 schema;
  later changes are `V{n}__{desc}.sql`. The switch is called out in the triggering feature spec.

## Consequences
- **Lossless by construction:** the spec is never decomposed, so import → export stays the
  identity function for the whole document, not just unmodeled nodes.
- **One engine:** document-shaped specs (JSONB) and relational data (users/sharing/audit) live in
  the same PostgreSQL instance — simpler self-hosting, and the audit row commits in the **same
  transaction** as the spec mutation (ADR-0003).
- Self-documenting, readable schema; fast early iteration; versioned production migrations later.
- UUID PKs enable data portability / self-hosting without ID collisions (minor perf trade-off,
  negligible here).
- Trade-off: the JSONB body is opaque to the relational engine — querying *inside* specs needs
  GIN/expression indexes or in-app traversal, not joins. Accepted: cross-spec queries run on
  metadata; deep structural queries are rare and belong to the in-memory model anyway.
- Trade-off: the Phase 1→2 switch needs discipline (baseline migration in the triggering spec).

## Alternatives Considered
- **MongoDB / document DB:** native document storage is a clean fit for whole specs, but it
  either adds a *second* datastore (the relational users/sharing/audit data still wants real
  constraints and transactions) or forces an all-Mongo design that loses relational integrity and
  weakens the cross-cutting audit-in-the-same-transaction guarantee. The extra engine also raises
  self-hosting friction. PostgreSQL JSONB matches the one real edge (documents) without those
  costs. Rejected.
- **Fully normalized relational (shred the spec into tables):** fights lossless round-trip — every
  unmodeled node needs a home, and reassembly must be exact — at a high shred/reassemble cost for
  little query benefit. Rejected.
- **Flyway from day one:** migration churn while the schema is most volatile; no value when the
  DB is ephemeral. Rejected.
- **Sequential integer PKs:** worse for a self-hostable, portable app. Rejected.
- **PostgreSQL ENUM types:** DDL hard to manage in migrations. Rejected for VARCHAR + check.
