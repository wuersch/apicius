---
name: verify
description: Build, launch, and drive Apicius (Quarkus backend + Vite frontend) to verify a change end-to-end in the real app.
---

# Verifying Apicius end-to-end

## Launch

```bash
# Backend (starts Postgres + Keycloak dev-services containers; needs Docker):
cd backend && ./mvnw quarkus:dev -Dquarkus.analytics.disabled=true
# Ready when http://127.0.0.1:8080/q/openapi returns 200 (~60-90s cold, containers pull).
# "Port 8080 seems to be in use": usually an Apicurio Studio reference container
# (docker ps | grep apicurio) squatting 8080 — stop it; don't remap Apicius (Vite proxy
# targets 8080). A 404 from RESTEasy on :8080 means you're talking to Apicurio, not Apicius.

# Frontend:
cd frontend && npm run dev   # Vite on http://localhost:5173, proxies /api → :8080
```

## Log in

Opening http://localhost:5173 redirects to the dev Keycloak (port 8180).
Fixture users from `backend/src/main/resources/dev-realm.json`: `ada`/`ada`, `grace`/`grace`.
The `apicius-frontend` client has **no direct-access grants** — you cannot curl a token with
`grant_type=password`; drive the browser instead.

## Drive

- The seeded specs in `import.sql` carry full engine-written bodies and open fine in the
  editor — they form a deliberate variability matrix (Storefront: everything on; Billing:
  one standard-errors opt-out; Fleet: a paging opt-out; Notifications: everything off).
  Verify against them freely, but remember dev mode is drop-and-create: a backend live
  reload reseeds and discards your edits — create a fresh API when the change must survive
  reloads mid-verification.
- New resource → the editor page shows the resource card; FEAT-006's shape section is at the
  bottom of each card ("SHAPE" + Add a field…).

## Inspect persisted state

```bash
docker ps | grep postgres   # dev-services container, user/db/password all "quarkus"
docker exec <id> psql -U quarkus -d quarkus -t -A \
  -c "SELECT body::json->'components'->'schemas'->'Product' FROM spec WHERE title='...'"
```

Note: `spec.body` is a `json` (not `jsonb`) column on purpose — key order is load-bearing
(ADR-0004); don't cast through `::jsonb` when asserting order.
