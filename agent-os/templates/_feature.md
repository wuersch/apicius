# FEAT-NNN: [name]

**ID:** FEAT-NNN
**Status:** proposed | specced | building | shipped
**Depends on:** PRIN-…, ADR-…
**Mockup:** docs/design/mockups/ — frame(s) …

> **Type:** Deterministic — behavior is fixed and verifiable. Every requirement maps to a
> pass/fail test.

## User Story
As a [role], I want [capability], so that [outcome].

> One line. Human intent only — the destination, not the route. Design lives below.

## Context / Notes
Links to related specs, constraints, decisions already made. Anything the agent needs to avoid
guessing.

> Mark any unknown inline as `[NEEDS CLARIFICATION: <question>]`. None may remain when
> Status → specced.

## Interaction Model
> How the user *reaches* the capability, not what they reach. The Apicius differentiation layer.
> Test: if a line would be equally true of a path-first / spec-shaped tool, it's domain, not
> interaction — delete it.

- **Entry point:** What the user starts *from* (resource-first invariant: never a path or verb).
- **Vocabulary:** Intent terms this feature exposes → what each serializes to
  ("fetch all" → GET /collection + 200). The user never types the right column.
- **Ordering invariant:** The fixed sequence (resource → behavior → serialization); what is
  *not* allowed to come first.
- **Projection direction:** Document-as-source-of-truth. The user edits a projection; the
  OpenAPI doc is preserved, never the primary surface.
- **Escape hatch:** Where raw / advanced (collapsed) editing is reachable, and the
  non-destruction guarantee it must honor.

## Use Cases
> Interaction-level, not widget-level. "Adds the 'fetch all' behavior to Person" — not "clicks
> the + button". A step must survive a full visual redesign. The Interaction Model states the
> invariants; use cases enact them — no invariants in steps, no flows in the section.

### UC1: [happy path]
- **Precondition:**
- **Flow:**
- **Outcome:**

### UC2: [edge / alternate path]
- **Precondition:**
- **Flow:**
- **Outcome:**

### UC3: [failure path]
- **Precondition:**
- **Flow:**
- **Outcome:**

## Scenarios — OPTIONAL
> Include ONLY when a use case fans out into many concrete cases (more than a handful of
> branches). Each scenario = one concrete instantiation of a use case with real values, mapping
> to exactly one test. Otherwise DELETE this section and let the acceptance criteria carry the
> concrete cases.

### Scenarios for UC[n]
| #  | Situation (concrete input) | Expected outcome |
|----|----------------------------|------------------|
| S1 |                            |                  |
| S2 |                            |                  |

*Rule: every branch of the use case appears as a scenario row. The set is the coverage
checklist — gaps here become bugs later.*

## Acceptance Criteria
> Given-When-Then. Each criterion maps to a use case (and scenario if present). The `(UCn)` /
> `(Sn)` tags are the traceability check — every use case and scenario must be covered by at
> least one criterion.

- **AC1 (UC1):** Given [state], when [action], then [result].
- **AC2 (UC2):** Given …, when …, then …
- **AC3 (UC3 / S3):** Given …, when …, then …

## Data / Domain
> Two domains. Keep them separate.

**Edited domain (OpenAPI) — link, don't reproduce.**
OpenAPI constructs this feature touches, by version-pinned, anchored reference. The spec is
authoritative; never restate its structure.
- e.g. [Operation Object](spec-3.1#operation-object)

**Application domain (Apicius) — describe fully.**
Apicius's own entities, fields, validation, states. Invented and owned; nothing external defines
it. Projection/sync state (un-serialized intent, document-dirty vs. saved) lives *here* — it's
Apicius state *about* OpenAPI, not OpenAPI.
- Entities / fields touched:
- Validation rules:
- States / transitions:

## Non-Goals
- Explicitly out of scope: …
