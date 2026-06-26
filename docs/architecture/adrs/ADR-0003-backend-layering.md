# ADR-0003: Backend layering — Resource → Service → Repository

**Date:** 2026-06-26
**Status:** Accepted
**Depends on:** ADR-0002

## Context
The Quarkus backend needs one consistent code-organisation pattern that humans and AI agents can
follow reliably, with clear separation of concerns, testability, and a single place to enforce
cross-cutting concerns (audit, authorization).

## Decision
A **three-layer architecture**, with a domain layer beneath it.

- **Resource (`resource/`)** — JAX-RS classes: path mapping, request validation (`@Valid`), DTO
  conversion, status codes, coarse-grained security (`@Authenticated`, `@RolesAllowed`).
  Delegates all logic to services; **never calls repositories directly.**
- **Service (`service/`)** — `@ApplicationScoped`, `@Transactional` business logic: validation,
  domain transitions, authorization checks (ADR-0005), audit emission. No HTTP concerns. This is
  where cross-cutting enforcement lives.
- **Repository (`repository/`)** — `PanacheRepositoryBase<Entity, UUID>`; queries only, no logic.
- **Domain (`domain/`)** — JPA entities carrying their own invariants; enums for fixed state
  sets; value objects. No dependency on services/repositories/CDI.

**DTOs:** `Create{Entity}Request` / `Update{Entity}Request` / `{Entity}Response`, beside the
resource or in a `dto/` sub-package. Mapping via **explicit static methods — no MapStruct, no
Lombok** (keep mappings visible and debuggable).

## Consequences
- Consistency: every feature follows the same shape; agents generate code that fits.
- Testability: services unit-tested with mocked repositories; resources via `@QuarkusTest`.
- Reliable cross-cutting: all mutations flow through services, so audit/authorization are
  guaranteed enforcement points.
- Trade-off: simple CRUD feels over-structured. Accepted — consistency beats local optimality.

## Alternatives Considered
- **Active Record (Panache entity methods):** blurs domain/data boundaries and complicates
  audit. Rejected.
- **CQRS / event sourcing:** overkill for v1; the audit log gives lightweight history.
- **Hexagonal / ports-and-adapters:** same separation with more indirection than this size needs.
