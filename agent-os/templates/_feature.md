# FEAT-NNN: [name]

**ID:** FEAT-NNN
**Status:** proposed | specced | building | shipped
**Depends on:** PRIN-…, ADR-…
**Mockup:** docs/design/mockups/ — frame(s) …

> **Type:** Deterministic — behavior is fixed and verifiable. Every requirement maps to a
> pass/fail test.

## User Story
As a [role], I want [capability], so that [outcome].

## Context / Notes
Links to related specs, constraints, decisions already made. Anything the agent needs to avoid
guessing.

> Mark any unknown inline as `[NEEDS CLARIFICATION: <question>]`. None may remain when
> Status → specced.

## Use Cases

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
Entities, fields, validation rules, states touched by this feature.
(Add a States/Transitions block here if behavior is meaningfully stateful.)

## Non-Goals
- Explicitly out of scope: …
