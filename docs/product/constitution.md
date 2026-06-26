# Constitution

> Apicius's governing principles — the durable "why" and the operational rules that follow from
> it. The tie-breaker for every decision. Changes rarely; cited by features (`PRIN-*`).

## The problem
Existing visual OpenAPI editors mirror the spec's JSON tree — Paths, Schemas, Responses,
Components. You edit the *storage format*, not your idea: features hide in nested menus, the
structure follows serialization rather than how anyone thinks about an API, and the style
guidelines no one remembers go unenforced. The trend deepens it — Apicurio's standalone Studio
editor was folded into Apicurio Registry, a spec *store*, leaving authoring secondary behind a
storage-oriented drafts/branches workflow. Apicius is editor-first by contrast.

## The bet
Organize around what the API **is** (its resources / nouns) and what it **lets people do**
(capabilities, in plain language). Make the spec a serialization detail — available on demand,
never the mental model. Bake best practices in so consistency is the default, not a discipline.

## What this means
- A backend engineer, a product designer, and a frontend consumer can all read the same screen.
- "Look up one product" is the unit of thought; `GET /products/{id}` is its derived address.
- Correctness (pagination, status codes, error format, naming) is supplied by the tool,
  explained in plain language, and overridable.

## The non-negotiable: opinionated lens, lossless model
The UI may be as curated and opinionated as we like — but the underlying model preserves 100% of
the spec. Anything we don't have a first-class control for is kept (shown under *Advanced* /
source), never silently dropped. **Opinionated presentation, lossless data.** This is what earns
trust for round-trips, and it is the line we do not cross.

## Inspirations
Camunda Modeler (approachable surface, deep capability), DaVinci Resolve (simple at first,
astonishing depth), Apple (simplicity as a feature). Anti-model: a UI that is a 1:1 form over the
spec, like the now-discontinued Apicurio Studio.

---

The principles below are the operational rules that follow from the above. They are stable,
cited by features in their `Depends on:` line, and change only occasionally.

## PRIN-001 — Resource-first
Model nouns (Product, Order). Operations, paths, and parameters are *derived* from resources and
their relationships. Never require the user to author a path.

## PRIN-002 — Capabilities, not HTTP
The primary expression of an operation is a plain-language capability ("Browse all products").
Method, path, query params, and headers are framed as derived/secondary detail (filters,
headers, answers) and de-emphasized in the UI.

## PRIN-003 — Opinionated lens, lossless model
Curate the UI freely; never drop or mangle spec data. Unknown or oddly-placed nodes from an
import are preserved and surfaced under *Advanced* or the source view. Round-trip is faithful by
default.

## PRIN-004 — Source on demand, not in your face
The raw YAML/JSON is always reachable (peek / split) but never the default surface. Most users
never need it; power users are one click away.

## PRIN-005 — Attach to the noun, inherit down, override locally
Examples, descriptions, validation, and deprecation attach to the resource and flow to every
operation that uses it. The inherited value is always shown; overriding is a deliberate local
act. One mental model, not five places to hunt.

## PRIN-006 — House rules, silently enforced
Best practices are applied by default, explained in plain language, and overridable. Detection is
deterministic and explainable; the optional AI layer only assists fixes/naming. Never a style
guide to memorize.

## PRIN-007 — Version is a detail, not a mode
OpenAPI version (3.0–3.2) is a tucked-away document setting, defaulted to 3.1. The intent layer
hides encoding differences. Version surfaces in exactly two places: capability **gating**
("needs 3.2") and the **downgrade guard** (never drop silently).
