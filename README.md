# Apicius
![License](https://img.shields.io/github/license/wuersch/apicius)
![LOC](https://sloc.xyz/github/wuersch/apicius?category=code)
![LOC](https://sloc.xyz/github/wuersch/apicius?category=cocomo)
![LOC](https://sloc.xyz/github/wuersch/apicius?category=effort)

[![Claude](https://img.shields.io/badge/Claude-D97757?logo=claude&logoColor=fff)](#)
[![Java](https://img.shields.io/badge/Java-%23ED8B00.svg?logo=openjdk&logoColor=white)](#)
[![React](https://img.shields.io/badge/React-%2320232a.svg?logo=react&logoColor=%2361DAFB)](#)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-6BA539?logo=openapiinitiative&logoColor=white)](#)
[![Postgres](https://img.shields.io/badge/Postgres-%23316192.svg?logo=postgresql&logoColor=white)](#)

A concept-first, web-based **OpenAPI editor** — organized around what your API *is* (its
resources / nouns) and what it *lets people do* (plain-language capabilities), not the spec's
JSON tree.

## What it is

Most visual OpenAPI editors mirror the spec's storage format — Paths, Schemas, Responses,
Components — so you end up editing serialization rather than your idea. Apicius takes the
opposite stance: model the API as **resources** and the **capabilities** they offer ("Look up
one product"), and treat the concrete `GET /products/{id}` as a *derived* detail. Best practices
are baked in so consistency is the default rather than a discipline — the aim is APIs that are
**beautiful by default**: consistent naming, paging, error shapes, and documentation without
anyone having to remember the rules.

Apicius is inspired by the great, unfortunately discontinued
[Apicurio Studio](https://www.apicur.io/studio/) — and even builds on the same OpenAPI document
model its lineage produced — but takes a different stance: not a harness over the spec tree, but
a concept-first editor with the design guidelines built in.

And unlike the cloud-only turn API tooling has taken (Postman, Insomnia and friends), Apicius is
**self-hostable by design**: open source under MIT, running on your own PostgreSQL and Keycloak.
Your API designs never leave your infrastructure.

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

Early alpha — and already a working editor. You can sign in, create an API, model resources
with typed fields, and work each derived capability's full contract in plain language: paging
on list capabilities, the standard error answers (RFC 9457), query parameters and headers,
and descriptions on every part of the contract — then export the document as YAML or JSON.
Importing an existing OpenAPI document is the next feature up; see
[`docs/spec/features.md`](docs/spec/features.md) for the live feature-by-feature status.

## Tech stack

- **Backend:** Quarkus (Java 25), PostgreSQL, Keycloak / OIDC.
- **Document engine:** `io.apitomy:apitomy-data-models` (the Apicurio Studio lineage's
  OpenAPI model) as the document model, edit, and validation engine — consumed behind an
  Apicius-owned `DocumentEngine` seam, so the UI's concept-first view is projected from a
  real, lossless OpenAPI document (ADR-0009). Specs persist as the serialized document plus
  denormalized summary columns for the list views (ADR-0008).
- **Frontend:** React + Vite + TypeScript, TanStack Query, Tailwind + shadcn/ui.
- **Contract:** the backend publishes a code-first OpenAPI document at `/q/openapi`, from which
  the frontend's typed client is generated with [orval](https://orval.dev).

## Repository layout

| Path | Contents |
|------|----------|
| `backend/` | Quarkus API — the `DocumentEngine` seam and its apitomy adapter, derivation, persistence. |
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
