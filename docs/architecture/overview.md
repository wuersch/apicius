# Architecture Overview

> C4 levels 1–2 (context + containers) and the quality drivers that shape everything — the
> arc42 essence, no ceremony. Diagrams: `./diagrams/*.mmd` (Mermaid; text, diffable,
> draw.io-importable). Decisions live in `adrs/` (ADR-*).

## Quality drivers / constraints
1. **Lossless round-trip** — import → edit → export must preserve 100% of the spec (PRIN-003).
   Drives a superset domain model with a preservation bag for unmodeled nodes.
2. **Server-authoritative model** — the superset OpenAPI model, parse/serialize, and the rules
   engine live in the Quarkus backend; user specs persist in PostgreSQL as JSONB documents; the
   browser is a typed client over the REST API, holding only view state (ADR-0002).
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
  `/q/openapi`, ADR-0002), the **superset OpenAPI domain model** (single source of truth), the
  **rules engine** (deterministic, pure Java functions → findings/nudges, PRIN-006), and
  **import/export** (parse & serialize 3.0 / 3.1 / 3.2, lossless preservation).
- **Datastore** (PostgreSQL via Hibernate ORM Panache) — specs as JSONB documents; relational
  tables for users/sharing/audit (ADR-0004).
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
- **Database:** PostgreSQL; specs stored as JSONB documents (not normalized into per-node tables),
  relational tables for users/sharing/audit; `snake_case` singular tables, UUID PKs,
  `created_at`/`updated_at`/`version`; enums as `VARCHAR`; Hibernate drop-and-create → Flyway
  later (ADR-0004).
- **Auth:** OIDC/Keycloak; server-side enforcement; token in memory (ADR-0005).
- **Real-time:** TanStack Query; polling behind the hooks, upgradeable to WebSocket (ADR-0006).
- **Frontend structure:** feature-based `src/features/{feature}/`; shadcn/Tailwind UI; i18n via
  react-i18next (`en`/`de`).
- **Deployment:** two pods (nginx + Quarkus), UBI9 images, Helm, JVM mode (ADR-0007).
