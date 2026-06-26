# Apicius

A concept-first, web-based **OpenAPI editor** — organized around what your API *is* (its
resources / nouns) and what it *lets people do* (plain-language capabilities), not the spec's
JSON tree.

## What it is

Most visual OpenAPI editors mirror the spec's storage format — Paths, Schemas, Responses,
Components — so you end up editing serialization rather than your idea. Apicius takes the
opposite stance: model the API as **resources** and the **capabilities** they offer ("Look up
one product"), and treat the concrete `GET /products/{id}` as a *derived* detail. Best practices
are baked in so consistency is the default rather than a discipline. It's inspired by the
now-discontinued Apicurio Studio, but is editor-first and opinionated by design.

## Core ideas

- **Resource-first** — model nouns; operations and paths are derived, never hand-authored.
- **Capabilities, not HTTP** — an operation reads as plain language; method/path/params are secondary.
- **House rules, built in** — naming, pagination, status codes, and error formats applied by
  default, explained plainly, and overridable.
- **Opinionated lens, lossless model** — the UI is as curated as we like, but the underlying
  model preserves 100% of the spec. Nothing is silently dropped; round-trips stay faithful.
- **Source on demand** — the raw YAML/JSON is always one click away, never the default surface.
- **Version is a detail** — OpenAPI 3.0–3.2 differences are hidden behind the intent layer.

## Status

Early / pre-alpha. The project is currently docs-driven: the backend and frontend are scaffolded
and wired together, but the product features above are not yet implemented.

## Tech stack

- **Backend:** Quarkus (Java 25), PostgreSQL, Keycloak / OIDC.
- **Frontend:** React + Vite + TypeScript, TanStack Query, Tailwind + shadcn/ui.
- **Contract:** the backend publishes a code-first OpenAPI document at `/q/openapi`, from which
  the frontend's typed client is generated with [orval](https://orval.dev).

## Repository layout

| Path | Contents |
|------|----------|
| `backend/` | Quarkus API — domain model, rules engine, persistence. |
| `frontend/` | React + Vite single-page app — the intent UI. |
| `docs/` | Product constitution, architecture (C4 + ADRs), and feature specs. |
| `agent-os/` | The development process and conventions. |
| `CLAUDE.md` | Entry-point map for working in the repo. |

## Getting started (development)

**Prerequisites:** JDK 25, Node 24+, and a container runtime (e.g. [OrbStack](https://orbstack.dev)
or Docker) — Quarkus Dev Services starts PostgreSQL and Keycloak in containers automatically.

**Backend** — from `backend/`:

```sh
quarkus dev          # API on :8080, OpenAPI at /q/openapi, Dev UI at /q/dev
./mvnw test          # run tests
```

**Frontend** — from `frontend/`:

```sh
npm install
npm run dev          # app on :5173, proxies /api to the backend
npm run generate     # regenerate the API client from the backend's /q/openapi (backend must be running)
npm test             # run tests
```

## Documentation

Start with [`CLAUDE.md`](CLAUDE.md) for the map, then the
[product constitution](docs/product/constitution.md) (the *why* and design principles) and the
[architecture overview](docs/architecture/overview.md) (C4 model, ADRs, tech stack). The repo is
structured for agent-assisted development.

## License

Apicius is open-source under the [MIT License](LICENSE).
