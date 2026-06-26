# ADR-0007: Container strategy

**Date:** 2026-06-26
**Status:** Accepted
**Depends on:** ADR-0001, ADR-0002

## Context
Apicius should be self-hostable and open-source, deployable on any conformant Kubernetes cluster.
We need container topology, base images, and a build approach.

## Decision

### Topology — two pods
- **Frontend pod:** nginx serves the built React assets and reverse-proxies `/api/*` to the
  backend.
- **Backend pod:** Quarkus API in **JVM mode**.

Independent pods → independent scaling and deploy cycles.

### nginx reverse proxy
The browser talks to a single origin (nginx); `/api/*` is proxied to the backend. This
**eliminates CORS entirely** — no headers, no preflight, no per-environment config.

### Multi-stage builds
Both images use multi-stage Dockerfiles (builder compiles/bundles; runtime carries only
artefacts) → small images, no build tools in production.

### Base images — Red Hat UBI9
Frontend `ubi9/nodejs-*` → `ubi9/nginx-*`; backend `ubi9/openjdk-25` → `*-runtime` (Java 25 LTS).
UBI images are freely redistributable, regularly patched, and container-aware.

### Portable manifests
Standard Ingress / Deployments / Services only (no OpenShift-specific features); a Helm chart for
parameterised deployment.

### Native compilation deferred
GraalVM native image is **not** used now; JVM mode is sufficient and simpler. Revisit via a new
ADR if startup/memory becomes a concern.

## Consequences
- No CORS complexity (single origin via nginx).
- Independent scaling of frontend and backend; small, secure images.
- Portable across local (OrbStack/Docker Desktop), managed (GKE/EKS/AKS), and self-hosted clusters.
- Trade-off: two images instead of one (mitigated by Helm) and minimal nginx proxy config.

## Alternatives Considered
- **Single container (backend serves static frontend):** simpler but couples deploys and blocks
  independent scaling. Fine for dev/demo, rejected for production.
- **Non-UBI base images (Debian slim / Alpine):** viable; UBI chosen for patch cadence and
  container-aware JVM tuning. Doesn't limit self-hosting (UBI is freely available).
