# ADR-0006: Polling-first real-time abstraction

**Date:** 2026-06-26
**Status:** Accepted
**Depends on:** ADR-0002

## Context
Single-user editing in v1 needs no live updates. But collaboration (presence, "edited by
others", shared specs) will eventually require near-real-time freshness. We want that to drop in
without re-architecting data fetching.

## Decision
Use **TanStack Query** as the frontend data/caching layer from day one. When freshness is needed,
implement it via `refetchInterval` **polling** behind the query hooks — no WebSocket
infrastructure in v1.

- Polling intervals live as named constants in shared config, not hardcoded in components.
- Latency-sensitive mutations use **optimistic updates** (update cache immediately; roll back on
  conflict — e.g. an optimistic-locking `version` clash — and surface the conflict).

**WebSocket upgrade path (mechanical, when needed):** the backend pushes
`{ entityType, entityId, action }` events; a `useChangeStream()` hook calls
`queryClient.invalidateQueries(...)` on receipt; `refetchInterval` is dropped or lengthened.
**Component and hook signatures don't change** — only the trigger (time-based → event-based).

## Consequences
- Nothing to build for v1; TanStack Query is already the data layer (ADR-0002).
- A proven, low-risk upgrade path to WebSocket when collaboration lands.
- Optimistic updates keep the editor responsive.
- Trade-off: polling isn't true real-time (changes appear within the interval). Acceptable until
  collaboration demands more.

## Alternatives Considered
- **WebSocket from day one:** sticky sessions / pub-sub, connection management — unjustified for
  v1. Rejected.
- **Server-Sent Events:** no advantage over polling here, and WebSocket is the better long-term
  target; skipping SSE avoids an intermediate step.
- **Custom polling (not TanStack Query):** reinvents caching/dedup/retry/optimistic updates.
  Rejected.
