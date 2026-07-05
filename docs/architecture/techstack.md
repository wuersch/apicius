# Tech Stack

> Status per item: **decided** (cites an ADR) | **proposed**.
> The two-tier architecture is decided (ADR-0002); individual libraries are proposed unless
> load-bearing for a decision.

## Backend (`backend/` — Quarkus, Maven `./mvnw`)
- **Framework:** Quarkus (Java) — decided (ADR-0002).
- **Language level:** Java 25 LTS (build target / base image) — decided (ADR-0007).
- **REST:** Quarkus REST (RESTEasy Reactive) + Jackson — decided (ADR-0002).
- **API spec:** SmallRye OpenAPI (code-first; `/q/openapi`) — decided (ADR-0002).
- **Persistence:** Hibernate ORM Panache + PostgreSQL; specs stored as JSONB documents, with
  relational tables for users/sharing/audit — decided (ADR-0004).
- **Domain model:** the superset OpenAPI model lives here, server-side — single source of truth.
- **OpenAPI model / parse / serialize / edit:** `apitomy-data-models` (the Apicurio Studio model,
  now under Apitomy) — object model, visitor traversal, and a JSON-serializable command layer;
  backend-only, consumed through the Apicius-owned `dev.apicius.document.DocumentEngine` seam —
  decided (ADR-0009).
- **Rules engine:** `apitomy-data-models` validation rules + `IValidationSeverityRegistry`,
  extended with Apicius house rules (PRIN-006) — decided (ADR-0009).
- **Auth:** quarkus-oidc + Keycloak (Dev Services) — decided (ADR-0005).
- **Migrations:** Hibernate drop-and-create → Flyway later — decided (ADR-0004).
- **Test:** JUnit 5 (`@QuarkusTest`) + REST-assured + Mockito — proposed.

## Frontend (`frontend/` — React + Vite, npm)
- **Framework:** React + Vite + TypeScript — decided (ADR-0002).
- **Components:** Tailwind + shadcn/ui — proposed.
- **Data layer:** TanStack Query — decided (ADR-0006). State held here is **view state only**;
  the domain model is server-side.
- **API client:** orval — generated types + TanStack Query hooks from `/q/openapi` — decided
  (ADR-0002).
- **Routing:** react-router — proposed.
- **Source editor:** CodeMirror 6 — proposed (lighter than Monaco, editing-friendly).
- **Auth:** react-oidc-context (token in memory) — decided (ADR-0005).
- **i18n:** react-i18next (`en` / `de`) — proposed.
- **Canvas (later):** React Flow — proposed.
- **Test:** Vitest + React Testing Library — proposed.
- **E2E:** Playwright (later) — proposed.

## Shared / ops
- **Repo:** monorepo `backend/` + `frontend/` + `docs/` (+ `k8s/` later) — decided (ADR-0001).
- **Contract flow:** code-first backend → orval-generated frontend client — decided (ADR-0002).
- **Deployment:** two pods (nginx + Quarkus), UBI9 images, Helm, JVM mode — decided (ADR-0007).
