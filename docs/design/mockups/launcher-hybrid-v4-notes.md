# Design notes — Create API dialog (Launcher Hybrid v4 · FEAT-003)

> Companion to `launcher-hybrid-v4.html` (mockup — View 1a launcher, View 1b Create API
> dialog, each light + dark).
> v4 = v3's launcher with the **Create API dialog** added; everything in
> [`launcher-hybrid-v3-notes.md`](launcher-hybrid-v3-notes.md) (layout grid, tokens,
> semantics, brand) still applies and is not repeated here.
> Depends on: FEAT-003, PRIN-002, PRIN-006, ADR-0004.

## Views ↔ frames

| View | `data-screen-label` | Scope |
|---|---|---|
| 1a · Workspace launcher | `apicius launcher v4 · 1a` (+ `— dark`) | All-APIs home, no dialog |
| 1b · Create API dialog | `apicius launcher v4 · 1b — create dialog` (+ ` dark`) | Same launcher with the Create API modal open |

The dialog is a full token swap in dark per the v3 table (fields `#3D3426`, selection olive
`#97A860` on `#37381F`, primary CTA inverts to cream-on-ink).

## Settled from v3

- **New API placement is settled: beside the greeting** (dark-filled CTA, top-right of the
  content column). The v3 placement exploration (6 options) is closed; the alternate CTA
  markup remains inert in the working source only as history.

## The dialog (FEAT-003)

- **Entry:** the "New API" CTA — never a file path or spec node (resource-first; creation is
  the one moment that precedes resource authoring).
- **Modal, 640px**, card bg over a `rgba(43,33,24,.38)` scrim; top-aligned (64px from the
  artboard top), not vertically centered — it should read as anchored to the launcher, and
  the form's height varies with the error state.
- **Fields, in order:** Title (required) → Description (marked `optional`, quiet tertiary
  label — not an asterisk system) → OpenAPI version picker. Nothing else: `info.version` is
  auto-seeded, so it is *footer copy*, not a field.
- **Vocabulary:** "Title" → `info.title`, "Description" → `info.description` (omitted when
  blank, per AC2). No mention of `openapi`, paths, or JSON anywhere in the dialog.

### Version picker (adapted from Editor Concepts "A · Pick a target once")

- Three stacked radio cards — **3.0 / 3.1 (Recommended, default) / 3.2 (Newest)** — each with
  a one-line plain-language tradeoff, *not* a changelog: the user reads capability
  consequences, never the encoding (PRIN-002).
- Adaptation from the concept: violet selection → **olive** (`#5E6A3F` border + `#F4F2E6`
  tint); "Recommended" uses the olive chip pair, "Newest" the ochre chip pair — same
  semantic colors as everywhere else (olive = good/safe, ochre = attention).
- **"fixed after creation"** sits right-aligned on the section label, in JetBrains Mono —
  the immutability contract (v1: recreate is the recourse) stated up front, quietly. The
  concept's live "what this unlocks" gating panel is **not** carried into the dialog — it
  belongs to Settings/editing; at creation it would be noise.
- Persisted `openapi` value = latest patch of the chosen minor (AC1/AC3).

### States

- **Error (AC5):** confirming with an empty title turns the field terracotta
  (`#B1453D` border + soft red glow) with an inline message under it — "A title is
  required — it's the name everyone will see." Blocking is inline; no toast, no disabled
  button (the button stays clickable and explains, rather than silently refusing).
- **Footer:** left, the seed note ("Starts at `v1.0.0` — change it any time later.") ·
  right, Cancel (outline) + **Create API →** (dark fill, the view's single primary action —
  the launcher CTA behind the scrim no longer counts while the modal is open).
- Duplicate titles are allowed (AC4) — deliberately **no** uniqueness warning in the UI.

### Working-source tweaks (not shipping UI)

`Launcher Hybrid v4.dc.html` exposes mockup-only knobs on View 1b: selected version and
title-error state. They exist to demo the states above; the real dialog derives them from
input.

## Open questions

- Post-create landing: FEAT-003 says "the editor opens on the empty API" — the empty-API
  editor state (ghost "first resource" prompt?) is not yet mocked.
- Import-a-spec shares the entry menu eventually ("New API ▾" in v3 notes); the dialog↔import
  relationship is unmocked.
