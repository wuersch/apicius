# ADR-0005: Authentication and authorization enforcement

**Date:** 2026-06-26
**Status:** Accepted
**Depends on:** ADR-0003

## Context
Apicius stores user specs server-side, so it needs authenticated access and (once collaboration
exists) per-spec authorization. We need to decide the mechanism and where enforcement lives. The
concrete permission model (roles, sharing) is **not yet specced** — this ADR fixes the
*mechanism*, not the policy.

## Decision

### Authentication
- **Frontend:** `react-oidc-context` runs the OIDC authorization-code flow with PKCE; the access
  token is held **in memory** (not `localStorage`).
- **Backend:** `quarkus-oidc` validates the Bearer token on every request (issuer/audience/JWKS
  are configuration-driven). Keycloak is the dev IdP (Quarkus Dev Services).

### User provisioning
On first authenticated request, a filter ensures an `app_user` row exists for the token's `sub`
claim (created from token claims; display name/email refreshed on later requests).

### Authorization — two levels
- **Resource layer (coarse):** `@Authenticated` on all API resources; `@RolesAllowed("ADMIN")`
  on admin-only endpoints.
- **Service layer (fine):** a single `AuthorizationService` exposes programmatic
  `require*`/context methods; service methods call the relevant check at the start of business
  logic, throwing a mapped `ForbiddenException` (403). **All authorization decisions are made
  server-side.** A request-scoped role context resolves once per request.

### Frontend role awareness
The frontend may fetch the current user's context (e.g. `/api/v1/users/me`) to show/hide UI —
**UX only;** the backend always re-checks.

### Deferred
The concrete role hierarchy and per-spec sharing/permission rules are TBD; they will be encoded
in `AuthorizationService` and an accompanying feature spec when collaboration is designed.

## Consequences
- Server-side authority: the frontend can never bypass security.
- One auditable, testable place (`AuthorizationService`) for all permission rules.
- Programmatic checks handle "owner OR collaborator"-style rules annotations can't express
  cleanly.
- Trade-off: services depend on the security context — acceptable, since they already own
  "who is doing what".

## Alternatives Considered
- **Annotation-only fine-grained auth:** can't express data-dependent rules (which spec? whose?)
  without complex annotation logic. Rejected for programmatic service-layer checks.
- **External policy engine (OPA, Casbin):** over-engineering for a simple, stable role model;
  adds deployment complexity. Rejected.
- **Resource-layer-only enforcement:** insufficient — scoping needs domain data available in the
  service layer.
