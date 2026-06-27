# Apicius — Agent Context

Apicius is a lightweight, modern, web-based **OpenAPI editor**, organized around what an API
*is* (its resources / nouns) and what it *lets people do* (plain-language capabilities) —
**not** the OpenAPI spec tree.

It's inspired by the now-discontinued **Apicurio Studio**, but takes a different stance: not a
harness over the spec, but a **concept-first editor with API design guidelines and best
practices built in**.

## How to work here

For any change, read in order and pull only what you need:

1. **This file** — the map and the ground rules.
2. **`agent-os/process.md`** — the loop a change follows.
3. **`docs/spec/features.md`** — the feature index; find the `FEAT-` you're working on.
4. **That one feature file** — and, from its `Depends on:` line, only the `PRIN-` / `ADR-` it
   cites.

The `Depends on:` citations exist so you can load the minimal slice — don't bulk-load the docs.

## How we work — `agent-os/`

Portable, project-agnostic process, reused across projects. Treat it as **read-only unless
we're explicitly iterating on the process itself** — don't edit the templates or drop
project-specific artifacts (feature files, ADRs) here; those live under `docs/`.

- **`process.md`** — the change loop (frame → design → decide → build → verify → ship) and the
  definition of done.
- **`conventions.md`** — IDs (`PRIN / FEAT / ADR-NNN`), the `proposed → specced →
  building → shipped` status flow, and the `Depends on:` citation graph
  (`grep -r <ID> .` returns a topic's full context: spec, rules, decisions, code).
- **`templates/`** — starting points for new feature specs and ADRs.

## What we're building — `docs/`

- **`product/`** — `constitution.md` (the durable *why* plus the `PRIN-*` design principles
  features cite) and `glossary.md` (core domain terms).
- **`spec/`** — `roadmap.md` (sequence), `features.md` (the load-first index), and `features/`
  (the `FEAT-*` specs).
- **`design/`** — `mockups/` (design references, recreated in code).
- **`architecture/`** — `overview.md` (C4 + quality drivers + conventions), `diagrams/`
  (Mermaid), `adrs/` (`ADR-*` decisions), `techstack.md` (two-tier tech stack), and
  `patterns.md` (recurring implementation patterns, added once settled).

## Ground rules

- **Don't infer undocumented features or tech** — if it isn't written down, ask.
- **Don't front-run intent — ask before generating spec artifacts** (requirements, features,
  even an ID-reserving placeholder). The gate is *new intent*, not *new text*: routine work
  needs no permission.
- **Update existing docs; don't invent structure.**
- **Docs state what *is*, not what's pending or meta** — capture durable process,
  requirements, and architecture. Leave out transient repo-status ("not scaffolded yet",
  "none imported yet"), tooling/automation speculation, self-referential maintenance notes
  ("keep this thin"), and import/provenance asides. Status that genuinely matters has a home
  (feature `Status:`, techstack `decided`/`proposed`) — put it there, not in prose.
- **Explain decisions** — record the *why*; this is also a learning project.
- **No undocumented shortcuts** — treat this as a long-lived system.
- **Memory is local and private** — promote insights worth sharing into the docs; use your
  own judgment on what matters, and ask only if genuinely unsure.

## Commands

**Backend** (`backend/`). Prefer the **Quarkus CLI** over manual generation:

- `quarkus create app` — scaffold a new project
- `quarkus dev` — live-coding dev mode (hot reload)
- `quarkus test` — continuous testing
- `quarkus build` — production build
- `quarkus ext add <name>` — add extensions (e.g. `quarkus ext add resteasy-reactive jackson`)
- `quarkus ext ls` — list project extensions
- `quarkus ext rm <name>` — remove an extension
- `quarkus image build` — build container image

If Maven must be invoked directly, use the wrapper `./mvnw` (never a global `mvn`).

**Frontend** (`frontend/`, npm):

- `npm run dev` — Vite dev server
- `npm run generate` — orval: regenerate the API client/types from the backend's `/q/openapi`
- `npm test` — Vitest
- `npm run build` — production build
