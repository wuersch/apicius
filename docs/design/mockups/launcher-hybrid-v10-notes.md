# Design notes — Launcher Hybrid v10 (headers & answers pass: scopes, per-answer headers, access pill)

> Companion to `launcher-hybrid-v10.html` (mockup — everything in v9, plus this round's
> rework of the operation page's Headers and Answers, and state cards 3·3–3·7).
> v10 = v9 unchanged + the changes below. Everything in
> [`launcher-hybrid-v3-notes.md`](launcher-hybrid-v3-notes.md) through
> [`launcher-hybrid-v9-notes.md`](launcher-hybrid-v9-notes.md) still applies.
> Depends on: PRIN-001, PRIN-002, PRIN-003, PRIN-005, PRIN-006.

## Views ↔ frames (new/changed in v10)

| View | `data-screen-label` | Scope |
|---|---|---|
| View 3 (changed) | `apicius operation — Browse all products` (+ ` — dark`) | Request/Response header cards · scope badges · access pill · Answers/Errors split |
| 3·3 · Add header scope | `apicius operation state 3·3 — add header scope` | "Add header to…" scope popover |
| 3·5 · Answer expanded | `apicius operation state 3·5 — answer expanded` | 200 open with per-answer headers |
| 3·6 · Edit/remove header | `apicius operation state 3·6 — edit/remove header` | Row hover actions + inherited-edit prompt |
| 3·7 · Quiet descriptions | `descriptions state 3·7 — quiet docs` | Renumbered from v9's 3·3, unchanged |

## Header scopes: keep the flat lens, steal inheritance as provenance

OpenAPI's path-level vs. operation-level parameters are a DRY serialization mechanism,
not a concept users should navigate (PRIN-001/002). The UI shows every header flattened
on the capability, with a **provenance badge** per row — three tiers:

- **Built in** — supplied by a guideline (RateLimit-*, ETag, Cache-Control). Follows its
  guideline toggle; no per-row remove.
- **Every Product capability** — attached to the noun, inherited down (PRIN-005). Better
  than path level: the noun spans `/products` *and* `/products/{id}`.
- *(no badge)* — local to this capability ("just this capability" in muted text).
  **Absence of a badge means "yours"** — the v9 "your design" label is dropped.

"Add header" lives in each group's heading row (top right — same for "Add a filter",
"Add an answer") and asks scope once (3·3): *just here / every Product capability /
whole API (becomes a guideline)*. Editing an inherited row asks *everywhere vs.
override just here* once (3·6). Overriding off renders strike-through + amber
"Overridden · off just here" pill — visible, never silent (same principle as 3·2).

**Round-trip (PRIN-003):** import maps path-level params to the noun tier only when truly
shared; export writes params onto each operation that has them (the spec has no
"remove inherited param" mechanism, so no reliance on path-level inheritance where an
override exists).

## Card grammar: everything is the Filters row

Headers split into **two stacked cards** — "Request Headers" and "Headers for all
Responses" — using the Filters row grammar: mono name (140px column) · plain-language
value/description (flex) · scope badge right. Side-by-side columns and ad-hoc row styles
are gone. "Headers for all Responses" is a curated fiction (the spec only has per-response
headers); export fans them out onto every declared response.

## Per-answer headers · Answers stay one line each

- Response headers can attach to a **single answer** (spec-accurate: response objects own
  their headers). They live inside the answer's expansion — "Headers on this Answer only"
  (3·5) — so a `200` can return `X-Total-Count`/`Link` while a `401` never does.
- The Answers card lists **one line per answer**: code chip · sentence · "+ N headers"
  hint · chevron. `304` ("Nothing changed — use your cached copy") is Built in · caching.
  "Add an answer" sits in the card heading.
- **Errors moved to their own card** below Answers: RFC 9457 badge + toggle in the
  heading, the explanatory chips ("401 not signed in" …) as body. Answers = success/
  variant shapes you design; Errors = the guaranteed failure contract.

## Access is not a header

`Authorization` was removed from Request Headers — auth is OpenAPI `security`, not a
parameter (the spec ignores an Authorization header param). Access is a first-class
fact of the capability: a teal **"Public — no sign-in"** pill next to the verb chip in
the title row, dropdown for Public / Signed-in / API key…, inheriting the API default
with per-capability override (`security: []` on export — lossless and idiomatic).

## Add-affordance rule

Titled cards → add action in the card's heading row (no ellipsis: inline row insert).
Untitled lists/nav → trailing list item with ellipsis ("Add a capability…", the dashed
palette strip): the affordance sits where the new item will appear.

## Row editing (3·6)

Hover reveals Edit/Remove where the badge sits; Edit opens the row inline (Enter saves,
Esc cancels — same as quiet descriptions). Remove: local row = delete; inherited row =
override off just here (or remove everywhere); built-in rows follow their guideline.

## Open questions

- All v9 open questions carry over.
- Access pill dropdown contents + how API-default auth is set (guideline?).
- Per-answer body ("and later its body shape" in 3·5) — the answer expansion's schema
  region is undesigned.
- Where does an overridden-off inherited header get restored? (Presumably the strike-through
  row's hover offers "Restore".)
- Response-header fan-out on export: confirm all-responses vs. success-only is acceptable
  for RateLimit-*/Cache-Control.
