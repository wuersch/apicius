# ADR-0002: Decoupled React frontend + Quarkus backend, code-first contract

**Date:** 2026-06-26
**Status:** Accepted
**Depends on:** ADR-0001, PRIN-003

## Context
Apicius needs a server that owns the data: the superset OpenAPI domain model, parse/serialize,
the rules engine, and persistence of user specs.
The frontend is a rich, interactive editor. We must decide the tier split and how the two halves
agree on the API contract.

Note two distinct OpenAPI surfaces, which must not be conflated:
1. **The user's documents** — the specs being edited. Domain data, persisted server-side.
2. **Apicius's own management API** — how the frontend talks to the backend.

## Decision
**A decoupled frontend and backend**, with a **code-first** contract for Apicius's own API:

- The **Quarkus backend** owns the domain model, parse/serialize, the rules engine, and
  persistence. It is the source of truth.
- JAX-RS resources are written directly; **SmallRye OpenAPI** auto-generates the spec for the
  management API at `/q/openapi` (always in sync with the code).
- The **React frontend** generates its TypeScript types and TanStack Query hooks from that spec
  via **orval** — it holds no domain state of its own, only view state.
- Apicius's own management API **follows the same API best practices the product preaches**:
  plural resources, wrapped+paginated collections (`page`/`limit`), RFC 9457 `problem+json`
  errors. Base path `/api/v1/`, UUID path params, `application/json`.

## Consequences
- Fast iteration: resources are the contract source; no hand-written YAML for our own API.
- End-to-end type safety: orval regenerates the client from the live spec.
- We eat our own dog food — the management API demonstrates the practices the product preaches.
- Trade-off: no compile-time contract enforcement on the backend; a resource can drift from
  intent. Mitigated by review and the frontend codegen surfacing mismatches.
- Trade-off: frontend codegen needs the backend running (or a saved spec). Orval points at the
  Quarkus dev server.

## Alternatives Considered
- **Spec-first (write OpenAPI YAML, generate interfaces):** upfront contract + compile-time
  enforcement, but the agent writes both sides and YAML editing adds friction without
  proportional benefit. Rejected.
- **Backend serves the frontend (coupled):** simpler topology but couples deploys and blocks
  independent scaling; see ADR-0007.
- **Frontend owns the domain model (client-only):** can't persist or share specs without a
  backend.
