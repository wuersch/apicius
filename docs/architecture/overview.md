# Architecture Overview

> C4 levels 1–2 (context + containers) and the quality drivers that shape everything — the
> arc42 essence, no ceremony. Diagrams: `./diagrams/*.mmd` (Mermaid; text, diffable,
> draw.io-importable). Decisions live in `adrs/` (ADR-*).

## Quality drivers / constraints
1. **Lossless round-trip** — import → edit → export must preserve 100% of the spec (PRIN-003).
   Drives a superset domain model with a preservation bag for unmodeled nodes.
2. **Server-authoritative model** — the superset OpenAPI model, parse/serialize, and the rules
   engine live in the Quarkus backend (the apitomy engine behind the `DocumentEngine` seam,
   ADR-0009); user specs persist in PostgreSQL as key-order-preserving JSON documents plus
   denormalized summary columns (ADR-0004, ADR-0008); the browser is a typed client over the
   REST API, holding only view state (ADR-0002).
3. **OpenAPI 3.0–3.2 span** — one in-memory superset model spanning the versions.
4. **Approachable to non-engineers** — the intent layer; correctness supplied via house rules.

## C4 L1 — System context
See `diagrams/context.mmd`. API designers, backend engineers, and frontend consumers edit an API
in Apicius (a React frontend over a Quarkus backend); they authenticate via an OIDC identity
provider (Keycloak). Apicius imports/exports OpenAPI documents (3.0–3.2); downstream codegen/SDK
tools consume the exported output — where `operationId` stability matters.

## C4 L2 — Containers
See `diagrams/containers.mmd`.
- **Frontend SPA** (React + Vite + shadcn) — the intent UI (resources, capabilities, shapes).
  Talks to the backend via an **orval-generated TanStack Query client**; **CodeMirror 6** renders
  the source view (PRIN-004). Holds view state only.
- **Backend** (Quarkus) — the **REST API** (JAX-RS; spec auto-published via SmallRye OpenAPI at
  `/q/openapi`, ADR-0002) over the **document engine**: `apitomy-data-models` as the superset
  OpenAPI model, edit protocol, validation engine, and parse/serialize (3.0 / 3.1 / 3.2,
  lossless preservation) — consumed behind the Apicius-owned `DocumentEngine` seam; only the
  `document.apitomy` adapter imports `io.apitomy.*` (ADR-0009). **Derivation** (canonical
  capabilities ADR-0010, fields ADR-0011) turns plain-language intent into spec constructs at
  the service chokepoint.
- **Datastore** (PostgreSQL via Hibernate ORM Panache) — specs as key-order-preserving `json`
  documents plus the ADR-0008 summary projection columns (list views never deserialize the
  body); relational tables for users/sharing/audit (ADR-0004).
- **Identity** (Keycloak / OIDC) — authentication; authorization is enforced server-side in the
  service layer (ADR-0005).
- *(Later)* **Canvas** (React Flow), **Mock server** ("Try it").

## C4 L3 — Components
Add per-container zoom-ins only when a container gets complex.

## Conventions (the short version — full rationale in the ADRs)
- **Tier split & contract:** decoupled frontend/backend; code-first API, typed client via orval
  (ADR-0002). Apicius's own API dogfoods the house rules (`page`/`limit`, `problem+json`).
- **Backend layering:** Resource → Service → Repository over a pure domain layer; cross-cutting
  concerns (audit, authorization) enforced in the service layer (ADR-0003). **No Lombok, no
  MapStruct** — explicit static mappers.
- **Database:** PostgreSQL; specs stored as `json` documents (not normalized into per-node
  tables — key order is load-bearing, ADR-0004) with denormalized summary columns kept in sync
  at the write chokepoint (ADR-0008); relational tables for users/sharing/audit; `snake_case`
  singular tables, UUID PKs, `created_at`/`updated_at`/`version`; enums as `VARCHAR`; Hibernate
  drop-and-create → Flyway later (ADR-0004).
- **Auth:** OIDC/Keycloak; server-side enforcement; token in memory (ADR-0005).
- **Real-time:** TanStack Query; polling behind the hooks, upgradeable to WebSocket (ADR-0006).
- **Document edits:** every mutation is one atomic `String → String` transformation through the
  `DocumentEngine` seam inside a transactional service chokepoint — derivation and validation
  resolve before the document is touched (ADR-0009, ADR-0010, ADR-0011).
- **Frontend structure:** surface-based `src/components/{home,editor,capability}` + `src/pages`;
  shadcn/Tailwind UI; everything rendered is the backend's projection, nothing echoed locally.
- **Deployment:** two pods (nginx + Quarkus), UBI9 images, Helm, JVM mode (ADR-0007).
