# FEAT-001: Authenticate

**ID:** FEAT-001
**Status:** shipped
**Depends on:** ADR-0004, ADR-0005
**Mockup:** none yet — login is IdP-hosted; the in-app surface is the identity indicator (an initials avatar) with sign-out, in the app chrome.

> **Type:** Deterministic — behavior is fixed and verifiable. Every requirement maps to a
> pass/fail test.

## User Story
As an API designer, I want to authenticate myself, so that I have a personal identity in Apicius.

## Context / Notes
Every other feature assumes an authenticated identity — the home (FEAT-002) greets the user and
resumes their last edit; Create (FEAT-003) and Import (FEAT-004) stamp who authored a spec. The
authentication *mechanism* is fixed in ADR-0005; this feature specifies only the user-facing
behavior over it.

Two stances a reader can't guess: the app is **fully gated** (no anonymous surface), and APIs are
**not private** — every authenticated user sees every API, with `owner` kept as provenance, not an
access filter. Roles and sharing are deferred (see Non-Goals).

## Use Cases

### UC1: First sign-in (happy path)
- **Precondition:** an unauthenticated designer with an account at the configured IdP.
- **Flow:** opens the app; is redirected to the IdP; authenticates; returns to the app. On the
  first authenticated request a filter provisions an `app_user` from the token claims.
- **Outcome:** the designer lands on the home, greeted by name, with an initials avatar shown in
  the app chrome.

### UC2: Returning sign-in (alternate)
- **Precondition:** a designer who already has an `app_user` (their `sub` was seen before).
- **Flow:** signs in via the IdP and returns to the app.
- **Outcome:** the existing `app_user` is reused (matched by `sub`); display name / email are
  refreshed from the current token claims; no duplicate user is created.

### UC3: Sign out (alternate)
- **Precondition:** an authenticated designer.
- **Flow:** chooses Sign out from the identity control.
- **Outcome:** the access token is dropped from memory; protected routes require re-authentication.

### UC4: Unauthenticated or expired access (failure path)
- **Precondition:** a request made with no valid token (never signed in, or the session expired).
- **Flow:** the request reaches a protected route / API.
- **Outcome:** the backend rejects it (401) and serves no protected data; the frontend redirects
  the user to the IdP to authenticate.

## Acceptance Criteria
- **AC1 (UC1):** Given an unauthenticated designer, when they authenticate at the IdP and make
  their first authenticated request, then an `app_user` is provisioned for the token's `sub` with
  display name and email from the claims, and the home greets them by name.
- **AC2 (UC2):** Given a designer whose `sub` already has an `app_user`, when they sign in again,
  then that same row is reused (matched by `sub`, `uq_app_user_oidc_subject`) with display name /
  email refreshed from the current claims — no duplicate user is created.
- **AC3 (UC4):** Given a request with no valid token, when it reaches a protected route, then the
  backend rejects it with 401 and serves no protected data, and the frontend redirects the user to
  the IdP.
- **AC4 (UC3):** Given an authenticated designer, when they sign out, then the access token is no
  longer held in memory and protected routes require re-authentication.
- **AC5 (IdP-agnostic):** Given any OIDC-compliant IdP configured by issuer / audience / JWKS
  (Keycloak in dev, Entra ID in production), when a designer authenticates, then sign-in succeeds
  through the same code path — no IdP-specific handling.
- **AC6 (UC1/UC2):** Given an authenticated designer, when the app chrome renders, then it shows an
  identity avatar with the user's initials (first + last name from the name claims; a graceful
  fallback when name parts are unavailable), and Sign out is reachable from it.

## Data / Domain

**Edited domain (OpenAPI) — link, don't reproduce.**
None — authentication does not touch the OpenAPI document.

**Application domain (Apicius) — describe fully.**
- Entities / fields touched: `app_user` (ADR-0004) — UUID `id`, unique `oidc_subject` (the token
  `sub`), `display_name`, `email`, `created_at` / `updated_at`. The avatar initials are derived
  from the name claims (not a stored field).
- Validation rules: the Bearer token must validate (issuer / audience / expiry / signature via
  JWKS); `oidc_subject` is unique (`uq_app_user_oidc_subject`), so a returning `sub` reuses its row.
- States / transitions: unauthenticated → authenticated → signed out; the first authenticated
  request for a new `sub` triggers provisioning.

## Non-Goals
- Roles / permission hierarchy — the app is gated by authentication alone in v1.
- Per-API visibility, ownership-based access, or RW/RO levels — all authenticated users see all
  APIs; `owner` is provenance only.
- Per-spec sharing and collaboration — later authorization work (ADR-0005).
- Notifications and activity feed.
- Self-service account management (profile, password) — owned by the IdP.
- IdP-specific claim / role mapping.
- Uploaded / custom / gravatar-style avatar images — initials-only avatar in v1.
