# ADR-0001: Monorepo structure

**Date:** 2026-06-26
**Status:** Accepted
**Depends on:** —

## Context
Apicius is a Quarkus backend plus a React frontend, with shared documentation and (later)
deployment manifests. We need a source-control layout. Options: one monorepo, a repo per
component, or a monorepo with workspace tooling (Nx, Turborepo).

## Decision
A **single Git repository** with top-level directories — no workspace build tooling; the two
build systems (Maven for the backend, npm for the frontend) stay independent.

```
apicius/
├── backend/    # Quarkus (Maven)
├── frontend/   # React + Vite (npm)
├── docs/       # product, spec, design, architecture
├── agent-os/   # portable process
├── k8s/        # deployment manifests (later)
└── CLAUDE.md   # agent entry point
```

## Consequences
- A single branch/PR can span docs and code; cross-cutting changes (API contract + frontend +
  spec) are atomic.
- Backend and frontend build and test independently; CI can run them in parallel.
- No Nx/Turborepo learning curve or maintenance for a two-project repo.
- Trade-off: at large team scale (20+), workspace tooling might pay off — not expected; revisit
  via a new ADR if it does.

## Alternatives Considered
- **Separate repos:** harder to coordinate API-contract and spec changes atomically; multi-repo
  overhead unjustified at this size.
- **Monorepo + Nx/Turborepo:** the two build systems share no dependencies; tooling adds
  complexity without benefit at this scale. Adoptable later if needed.
