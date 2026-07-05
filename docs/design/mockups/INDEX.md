# Mock-ups

Design references for the UI, recreated in the stack (see `agent-os/conventions.md` for how
mock-ups are used and cited).

## Apicius shell — launcher · editor · operation
[`launcher-hybrid-v3.html`](launcher-hybrid-v3.html) — the Apicius visual identity across three
views, each in light and dark. Read [`launcher-hybrid-v3-notes.md`](launcher-hybrid-v3-notes.md)
alongside it for layout, status-color semantics, theme tokens, and the decisions the HTML can't
speak.

| Frame (`data-screen-label`) | View | Maps to |
|---|---|---|
| `apicius launcher v3` | All-APIs home | FEAT-002 |
| `apicius editor — Storefront API` | One API · Resources home | future — Resource & Shape editing (`roadmap.md`) |
| `apicius operation — Browse all products` | One capability | future — capability-first operation creation (`roadmap.md`) |

Each frame has a light and a `— dark` variant.

## Create API dialog — launcher v4
[`launcher-hybrid-v4.html`](launcher-hybrid-v4.html) — v3's launcher with the **Create API
dialog** added; the v3 editor/operation frames are carried over unchanged. Read
[`launcher-hybrid-v4-notes.md`](launcher-hybrid-v4-notes.md) alongside it — v3's notes still
apply for layout, tokens, and semantics and are not repeated.

| Frame (`data-screen-label`) | View | Maps to |
|---|---|---|
| `apicius launcher v4 · 1a` | Workspace launcher, no dialog | FEAT-002 |
| `apicius launcher v4 · 1b — create dialog` | Create API dialog over the launcher | FEAT-003 |

Each frame has a light and a `— dark` variant.

## New API menu · import flow — launcher v6
[`launcher-hybrid-v6.html`](launcher-hybrid-v6.html) — v4's launcher and Create API dialog with
the **"New API" menu** (settled from the v5 exploration), the **import flow** (picker → reading →
review), and the final brand mark; the v3 editor/operation frames are carried over unchanged.
Supersedes v4's launcher frames as the current launcher reference. Read
[`launcher-hybrid-v6-notes.md`](launcher-hybrid-v6-notes.md) alongside it — v3's notes (layout,
tokens, semantics) and v4's notes (Create API dialog) still apply and are not repeated.

| Frame (`data-screen-label`) | View | Maps to |
|---|---|---|
| `apicius launcher v6 · 1a` | Workspace launcher, no dialog | FEAT-002 |
| `apicius launcher v6 · 1b — create dialog` | Create API dialog over the launcher | FEAT-003 |
| `apicius launcher v6 · 1c — New API menu` | Creation entry menu (scratch / import) | FEAT-003, FEAT-004 |
| `apicius launcher v6 · 1d — import picker` | Import dialog: drop zone / browse / URL | FEAT-004 |
| `apicius launcher v6 · 1e — import reading` | Processing state while the spec is read | FEAT-004 |
| `apicius launcher v6 · 1f — import review` | Confirm with detected title/description/version | FEAT-004 |

Each frame has a light and a `— dark` variant.
