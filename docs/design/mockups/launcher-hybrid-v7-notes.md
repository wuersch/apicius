# Design notes — Launcher Hybrid v7 (field editor)

> Companion to `launcher-hybrid-v7.html` (mockup — everything in v6, plus the **inline
> field editor** for a resource's shape: add / refine / guard / edit, light + dark).
> v7 = v6 unchanged + views **2c** and **2d**. Everything in
> [`launcher-hybrid-v3-notes.md`](launcher-hybrid-v3-notes.md) (layout grid, tokens,
> semantics), [`launcher-hybrid-v4-notes.md`](launcher-hybrid-v4-notes.md) (Create API
> dialog), and [`launcher-hybrid-v6-notes.md`](launcher-hybrid-v6-notes.md) (menu, import,
> new-resource dialog, brand mark) still applies and is not repeated here.
> Depends on: FEAT-006, PRIN-001, PRIN-002, PRIN-006, ADR-0011.

## Views ↔ frames (new in v7)

| View | `data-screen-label` | Scope |
|---|---|---|
| 2c · Add a field | `apicius editor — Storefront API · 2c — add a field` (+ ` — dark`) | Inline editor open in Product's shape table; `id` row visible + locked |
| 2d·1 · Kind menu | `field editor states · 2d·1 — kind menu` | The kind dropdown open — full vocabulary |
| 2d·2 · Refined + list | `field editor states · 2d·2 — refined + list` | Text as UUID, "a list of these" on (UC2) |
| 2d·3 · Password rule | `field editor states · 2d·3 — password house rule` | Write-only applied + explained + Override (UC2/AC5) |
| 2d·4 · Name collision | `field editor states · 2d·4 — name collision` | Duplicate name blocked, confirm disabled (UC5) |
| 2d·5 · Advanced open | `field editor states · 2d·5 — advanced open` | Normal/Auto/Write-only radio group, Auto selected |
| 2d·6 · Edit a field | `field editor states · 2d·6 — edit a field` | Edit `price` in place, Remove field, `id` locked above (UC3/UC4) |

## Settled: inline, not a dialog

Field edits happen **inline in the shape table**, docked at the point of the row they
create or edit — not in a modal, not in a view replacement. Rationale: a field edit is
small, atomic, and frequent; the rest of the shape is exactly what you check a new field
against (names, kinds), so it must stay visible (PRIN-001). The line drawn:
**dialogs create nouns** (resource — 2b; later datatype), **inline shapes them**.
One editor open at a time; everything else inert.

## Editor anatomy (both themes)

- **Bands:** header and footer share a darker band (`#F0E6D0` light / `#453A24` dark) over
  the lighter editing body (`#F7EFDD` / `#3A3122`) — the same darker–light–darker scheme as
  the dialogs. Header: stacked title + one-line description (both Hanken, no mono).
  A 2px top rule in the theme's ink color docks the editor to the table.
- **Name row:** label left, **Required toggle right** (required is part of the field's
  identity, and the label row gives it separation). Derived `property firstName` in mono
  directly beneath the input — live, never typed (PRIN-002). Description sits in close
  proximity below (one identity group, 19px section rhythm matching the dialogs).
- **Kind is a sentence:** `[Text ▾] as [plain ▾] · ☐ a list of these` — three quiet
  controls that are literally ADR-0011's model (core type · optional refinement · list
  wrapper). Dropdowns, not chips: a closed dropdown costs one value of visual weight, not
  the whole vocabulary.
- **Kind menu** (2d·1): grouped **Simple types** (the six core kinds) and **Data types**
  (sketched `→ Category`, `→ Status` — named datatypes land in the *same slot* later;
  matches the left-nav "Data types" vocabulary, not the spec-internal "complex kinds").
- **Advanced, collapsed by default:** `Advanced · normal` disclosure row. Expanded it
  holds one **radio group** — Normal · Auto ("the server sets it") · Write-only ("never
  returned"). Auto/write-only being one choice makes FEAT-006's UC6 (invisible field)
  **unrepresentable by construction** — feed back into the spec: AC10's block becomes a
  non-case. Required stays a toggle (independent axis).
- **Footer:** derived serialization in mono, de-emphasized (`→ string · in required`) ·
  Cancel · primary (Add field / Save changes). Edit mode adds **Remove field** (danger,
  left) — remove lives where edit lives (UC4). Blocked states disable the primary
  (opacity .4) and explain in a tinted banner, never a toast.
- **House rule** (2d·3): *Text as password* opens Advanced itself, selects Write-only with
  a mono `house rule` tag, and explains in a `#F3EBDA` banner with an **Override** action —
  applied visibly, overridable, never silent (PRIN-006, AC5).
- **`id` row:** always first in the shape table — muted, lock glyph, "identifier — the
  server sets it". Not editable, not removable (AC7); Product's field count includes it.

## Working-source tweaks (not shipping UI)

Views 2c/2d are static states in `Launcher Hybrid v7.dc.html`; the v6 knobs (dialog
version/error, menu open, activity panel, greeting stressor) carry over unchanged.

## Open questions

- All v6 open questions carry over (post-create/post-import landing, resource↔datatype
  promotion, `x-*` home, composition, import failures).
- Refinement vocabulary per kind (Text: email/UUID/URL/password; numbers: bit-width /
  float-double) is asserted here but the full ADR-0011 table isn't rendered anywhere a
  user can browse — candidate: the kind menu's second column or a "?" affordance.
- Long shapes: the inline editor assumes short tables; whether the editor pins or scrolls
  with very long shapes (20+ fields) is unresolved.
- Field reordering is out of scope (FEAT-006 non-goal): new fields append.
