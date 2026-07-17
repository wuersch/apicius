# FEAT-009: View a capability's contract

**ID:** FEAT-009
**Status:** shipped
**Depends on:** PRIN-001, PRIN-002, PRIN-006, ADR-0008, ADR-0009, FEAT-005
**Mockup:** `docs/design/mockups/launcher-hybrid-v10.html` — View 3 (operation — Browse all
products) and state 3·4 (standard errors off)

> **Type:** Deterministic — behavior is fixed and verifiable. Every requirement maps to a
> pass/fail test.

## User Story
As an API designer, I want to see everything a capability promises — what clients send, what
comes back, and how failures answer — in one plain-language place, so that I can understand
and refine the contract without reading spec syntax.

## Context / Notes
The first feature organized around a single capability rather than a resource. It establishes
two things: the **contract view** that later refinement features plug into (FEAT-010 paging,
FEAT-011 query parameters & headers, FEAT-012 descriptions), and the **standard error
answers** — the one contract change this feature ships. The error contract follows the
modern-petstore reference (petstoreapi.com): every failure answers in one shared shape.

Shipping this feature rewrites no existing document. Capabilities derived from now on carry
the standard errors from birth; capabilities that predate the feature show them as
available-but-absent, and only a deliberate adopt action writes (see UC3). FEAT-005's
Derivation section lists this feature as an extension of what derivation produces.

## Interaction Model
- **Entry point:** a capability, reached from its resource — the noun's "can do" list. The
  designer never starts from a path or method inventory. From the view, the resource's other
  capabilities stay one step away — the "can do" list travels with the capability, and it
  gains entries only as features add them (no placeholder or foreshadowed entries).
- **Vocabulary:** the contract is presented as plain-language **facets**: the capability's
  label (↔ the operation's `summary`), its description, **Request** — what clients send,
  **Headers** — what travels alongside, **Answers** — what comes back (the success answer and
  the failure answers). Failure answers carry plain-language names ("we couldn't read the
  request", "not signed in", "no product with this id", "bad input", "too many requests",
  "our fault"); methods, paths, and status codes are derived, de-emphasized detail
  (PRIN-002).
- **Ordering invariant:** facets keep one stable order on every capability — identity,
  description, Request, Headers, Answers. A facet that doesn't apply is absent, never shown
  empty. The stable order is itself a consistency signal: compared across capabilities, a
  missing or extra facet stands out.
- **Projection direction:** everything shown is projected from the document. Adopting the
  standard errors is one atomic document mutation through the engine seam (ADR-0009).
- **Escape hatch:** none yet (source peek is future). The guarantee it inherits: adopting
  standard errors writes only the constructs named below — nothing else in the document
  changes.

## Standard error answers — the contract
The requirements decision this feature owns:

- One shared error shape, **`Error`**, under `components/schemas`: `type`, `title`, `status`
  (required), `detail`, `instance`, and `errors` — an optional list of field-level validation
  problems. Failure bodies are served as `application/problem+json` (RFC 9457).
- Reusable failure answers under `components/responses`, referenced from operations:
  `BadRequest` (400), `Unauthorized` (401), `NotFound` (404), `UnprocessableEntity` (422),
  `TooManyRequests` (429), `InternalServerError` (500) — each with a plain-language
  description.
- Which operations answer what:

  | Failure answer | Applies to |
  |---|---|
  | 400, 401, 429, 500 | every operation |
  | 404 | operations addressing one resource (the `{id}` paths) |
  | 422 | operations accepting input beyond the id: Add, Update, Browse |

- The error furniture is derived plumbing, not user data: it never appears as a datatype in
  shared-data views and never counts toward resources or capabilities (ADR-0008 counts
  unchanged). Its schema name is therefore reserved: a resource or datatype named `Error`
  is rejected — otherwise a later adoption would wire failure bodies to user data.
- **Per-capability opt-out** (PRIN-006 — the built-in default, deliberately overridable):
  switching the standard errors off removes exactly the operation's applicable
  failure-answer references and nothing else; switching on restores them (UC3's adopt).
  Cheap, non-destructive, reversible — no confirm. The shared furniture, once created, is
  never removed; opted-out and pre-feature operations are structurally the same "absent".
- Why: a uniform failure shape is the highest-leverage consistency win — client code handles
  every failure the same way. 401 is included now so the error contract doesn't reshape when
  security schemes arrive (a future feature owns their semantics).

## Use Cases

### UC1: Inspect a capability (happy path)
- **Precondition:** an API with at least one resource and capability.
- **Flow:** the designer opens a capability from its resource.
- **Outcome:** sees, in plain language and stable facet order: the label and its derived
  operation (method + path, de-emphasized); the description; what clients send, when the
  capability takes input; the headers, including the derived content-negotiation line; and
  every answer the document holds for it — success and failures.

### UC2: Inspect a capability that takes a body (alternate)
- **Precondition:** a resource with Add or Update.
- **Flow:** the designer opens Add, or Update.
- **Outcome:** the Request facet states what clients send, derived from the resource's shape —
  Add: the resource's fields, with the identity stated as server-assigned; Update: "send only
  the fields you change" (merge-patch). Derived, never authored.

### UC3: Adopt standard errors on an older capability (alternate)
- **Precondition:** a capability derived before this feature; its operation lacks the
  standard failure answers.
- **Flow:** the Answers facet shows the standard failure answers as available-but-absent;
  the designer adopts them with one action.
- **Outcome:** the operation answers its standard set per the table above. The shared
  furniture (`Error` schema, reusable responses) is created on first adoption and reused by
  every later one. Nothing else in the document changes.

### UC4: Create a resource after this feature ships (alternate)
- **Precondition:** any API.
- **Flow:** the designer creates a resource with capabilities (FEAT-005).
- **Outcome:** every derived operation carries its standard failure answers from birth, all
  referencing the one shared error shape.

### UC5: Opt out of the standard errors (alternate)
- **Precondition:** a capability whose operation answers the standard set.
- **Flow:** the designer switches the standard errors off — a plain, unconfirmed,
  reversible toggle (the deliberate override of a built-in default, PRIN-006).
- **Outcome:** the operation stops declaring its applicable standard failure answers;
  nothing else in the document changes — the shared furniture remains. Switching back on
  restores the standard set.

## Acceptance Criteria
- **AC1 (UC1):** Given a capability, when the designer views it, then its label, derived
  operation, description, and every applicable facet are presented in the stable facet order,
  each projected from the document, plain language primary and serialized detail secondary.
- **AC2 (UC1):** Given a facet that doesn't apply (e.g. Browse takes no body), then that
  facet is absent — never presented empty.
- **AC3 (UC1):** Given any capability, then the Headers facet includes the derived
  content-negotiation line (responses are `application/json`), marked derived and read-only —
  no corresponding parameter is written to the document.
- **AC4 (UC1):** Given a document whose capabilities predate this feature, when the designer
  views them, then no document mutation occurs — absent standard answers are shown as
  available, and only the adopt action writes.
- **AC5 (UC2):** Given Add, then the Request facet presents the resource's shape with the
  identity field stated as server-assigned; given Update, it states merge-patch semantics —
  both derived from the document.
- **AC6 (UC3):** Given a capability without standard errors, when the designer adopts them,
  then its operation gains exactly the failure-answer references per the table, the `Error`
  schema and reusable responses exist under `components`, no `x-` extension or other content
  is written, ADR-0008 counts are unchanged, and the designer's last-edited location moves to
  this API and capability in the same transaction — the first capability-level pointer write.
- **AC7 (UC3):** Given the shared error furniture already exists from an earlier adoption,
  when adopting on another capability, then the existing furniture is referenced, not
  duplicated.
- **AC8 (UC4):** Given resource creation after this feature ships, then each derived
  operation carries its standard failure answers per the table from birth.
- **AC9 (UC3, UC4):** Given the error furniture exists in a document, then it is never
  presented as a datatype, resource, or capability anywhere.
- **AC10 (UC5):** Given a capability whose standard answers are present, when the designer
  switches them off, then its operation loses exactly the applicable failure-answer
  references and nothing else — the shared furniture and the ADR-0008 counts are unchanged,
  and the last-edited location moves to this API and capability; switching them on again
  yields a document identical to the one adoption first produced.

## Data / Domain

**Edited domain (OpenAPI) — link, don't reproduce.**
- [Responses Object](spec-3.1#responses-object) / [Response Object](spec-3.1#response-object)
  — failure answers on operations, reusable under
  [Components](spec-3.1#components-object) (`schemas`, `responses`).
- [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) — problem details,
  `application/problem+json`.

**Application domain (Apicius) — describe fully.**
- Entities / fields touched: the `spec` row — `body` JSONB mutated through the engine seam
  (ADR-0009) on adopt only; `last_edited_location` written at the same chokepoint; ADR-0008
  counts unaffected.
- Validation rules: the on/off state is derived structurally — on iff every applicable
  answer references its shared response; resource and datatype names deriving to `Error`
  are rejected (reserved for the shared error shape).
- States / transitions: a capability's error state (standard answers present / absent) is
  derived structurally from the document — no marker, no extension.

## Non-Goals
- "Try it" (mock server — roadmap Later).
- Action-shaped capabilities (Restock) — the rail foreshadows them; their page is undesigned.
- Renaming a capability — the label is the operation's `summary`, a different field from its
  `description` (FEAT-012); rename is its own future feature.
- Editing the standard error set (codes, shape) per capability — on/off is the only
  per-capability control (UC5).
- Built-in response headers (RateLimit-*, ETag, Cache-Control) — deliberately dropped; all
  headers are designer-authored (FEAT-011).
- Security semantics behind 401 — security schemes are a future feature.
- Guidelines rail — house-rules display is deferred. Nested TOC, scroll-spy, palette —
  design chrome owned by the mockup; the capability list itself ships (see Interaction
  Model), its entries growing per feature.
