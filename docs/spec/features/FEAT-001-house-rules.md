# FEAT-001: House rules — embedded best practices

**ID:** FEAT-001
**Status:** proposed
**Depends on:** PRIN-006
**Mockup:** docs/design/mockups/ — *Shape + House rules* frame

> **Type:** Deterministic — behavior is fixed and verifiable. Every requirement maps to a
> pass/fail test.

## User Story
As an API author, I want the editor to apply API design best practices for me — silently, in
plain language, and overridably — so that simply using Apicius yields a spec that follows the
gold standard without my having to memorize a style guide.

## Context / Notes
This is the product's headline promise (PRIN-006): best practices built in, not a linter bolted
on. The gold-standard reference to embed is **https://docs.petstoreapi.com/**.

The concrete rule set (naming, collection wrapping, pagination, status codes, error format,
`operationId` derivation, nesting/relationship guidance, …) and its enforcement mechanics
(deterministic detection, live nudges, one-click fixes) are **deliberately not specced here** —
they will be defined when this feature is taken up. This file holds the intent and reserves the
ID; it is a placeholder, not a committed design.

## Non-Goals
- Not specifying the individual rules or their mechanics yet (deferred to when this is specced).
- The optional AI layer (fix/naming assistance) is out of scope for the MVP.
