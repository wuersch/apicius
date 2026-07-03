# Design notes — Apicius shell (Launcher Hybrid v3)

> Companion to `launcher-hybrid-v3.html` (mockup, 3 views × light/dark). The mockup shows
> *what*; this doc carries the decisions the HTML can't speak. Read both before building.
> Depends on: PRIN-001, PRIN-002, PRIN-003, PRIN-004, PRIN-006.

## Views ↔ frames

| View | `data-screen-label` | Scope |
|---|---|---|
| 1 · Workspace launcher | `apicius launcher v3` (+ `— dark`) | All-APIs home |
| 2 · API editor | `apicius editor — Storefront API` (+ `— dark`) | One API, Resources home |
| 3 · Operation detail | `apicius operation — Browse all products` (+ `— dark`) | One capability |

## Layout system

- **Column grid `198 / flex / 300`** (gaps 34px, page padding 44px). The masthead uses the
  *same* columns as the body — logo/breadcrumb over left rail, search over content, actions
  over the right panel. Preserve this mirroring; it's what makes the chrome feel gridded.
- **Chrome vs content indent.** Chrome (logo lockup, breadcrumb) sits at the column's 0 edge;
  rail content (section labels, nav items, footer) is indented 11px. This outdent is a
  deliberate hierarchy signal — do not "fix" it by aligning everything to one axis.
- **One primary CTA per view**, dark-filled, top-right of the content column:
  New API (V1) · New resource (V2) · Try it (V3). Everything else is quiet.
- **Search is a command-palette trigger**, centered, scoped per view ("Search APIs &
  resources" / "Search this API"). Results open as a centered ⌘K palette (~640px), grouped by
  type (Capabilities / Fields / Resources / Source), each hit naming its noun ("price — field
  on Product"), with a "Search all APIs" scope escape in the footer. Results navigate; palette
  actions are v2.

## Navigation model

- Masthead identity slot: V1 logo lockup → V2 `All APIs › Storefront API` → V3
  `All APIs › Storefront API › Product` (current segment bold, ancestors linked). API name +
  version badge appear exactly once prominently (the content h1), never duplicated in chrome.
- Left rail swaps per scope: workspace nav (Browse / Groups / Smart groups) → API nav
  (Design / Source / Guidelines) → capability nav ("Product can…" / Shape).
- **Source** is a rail section (PRIN-004): full-page editor, not a squeezed side-panel peek.
  A cramped code peek was tried and removed — don't reintroduce it.

## Semantics & copy

- **Status colors:** olive `#5E6A3F` read/pass · teal `#34736A` create/active · ochre
  `#9A6B33` update/needs-attention · terracotta `#A65532` delete/error. Never use olive for
  decoration — it means "good/read" everywhere.
- **"Guidelines", not "house rules"** (UI label for PRIN-006 / RULE-*). Findings are worded
  as plain-language fixes with the affected capabilities named; passing rules summarized in
  one quiet line ("10 followed quietly — naming, errors…").
- Plain language over abbreviations: "5 resources · 21 ops" is the ceiling; never "res".
- Capability rows: plain-language name primary, `VERB /path` in mono as secondary detail
  (PRIN-002). Both are searchable.

## States, not fixtures

- **Ghost tiles** ("Start from scratch" / "Import a spec", dashed) are an *onboarding state*:
  shown at the end of the grid only at low API counts (~≤8); hidden at scale; never shown in
  list view (its empty state shows them centered instead). The masthead "New API ▾"
  (create + import paths) is the permanent affordance.
- **"Load more" + "Showing 8 of 20"** — the grid paginates; never a "View all" label when the
  all-view is already selected.
- Dark mode is a full token swap (same layout, same semantics); the greeting copy follows
  time of day, which the light/dark pair dramatizes ("Good morning" / "Good evening").

## Theme tokens (light → dark)

Map onto shadcn CSS variables; do not re-derive by eye.

| Role | Light | Dark |
|---|---|---|
| Page background | `#F2E8D6` | `#262019` |
| Card / panel | `#FBF6EB` | `#322A20` |
| Muted chip / field | `#ECE0CB` | `#3D3426` |
| Chip hover | `#E8DCC4` | `#46392A` |
| Row hover / subtle | `#F3EBDA` | `#3A3122` |
| Hairline | `#EFE4CD` | `#3F3527` |
| Ink (primary text, dark CTA bg) | `#2B2118` | `#F0E6D2` |
| Text secondary | `#564E42` | `#CDC1A9` |
| Text tertiary | `#6E6456` | `#A99B82` |
| Text faint / counts | `#6F6349` | `#9C8C6C` |
| Olive (read/pass) | `#5E6A3F` | `#97A860` |
| Olive text on chip | `#57613A` / bg `#E6E7D4` | `#ABB97E` / bg `#3A4026` |
| Teal (create/active) | `#34736A` | `#6FB3A7` |
| Ochre (warn/update) | `#9A6B33` / bg `#F0E2C8` | `#D9A860` / bg `#4A3A22` |
| Terracotta (delete/error) | `#A65532` | `#D98D66` |
| Outline border | `#CBBFA6` | `#57492F` |
| Dashed (ghost) border | `#BFB18D` | `#5E5138` |

Type: **Hanken Grotesk** (UI) + **JetBrains Mono** (paths, counts, versions, code).
Radii: 6px controls · 8px tiles · 10px cards · 12px panels. Shadows are warm
(`rgba(74,54,32,…)` in light, black-based in dark), soft, two-layer.

## Brand

- Lockup: olive-branch icon + `apicius` (18px bold) + `STUDIO` (11.5px letterspaced,
  raised to the cap line — a deliberate superscript qualifier, not an alignment error).
  Spacing hierarchy: icon→wordmark 7px, wordmark→qualifier tight (nested), everything
  else far. Icon color = olive.
- No watermark art currently; a bay-leaf background was removed pending real artwork.

## Open questions (not settled by this mockup)

- Drafts/publish flow: "Ready to publish?" checklist exists in V2's right panel, but there is
  deliberately **no Publish button** anywhere — placement TBD with the drafts feature.
- Group label filtering: smart groups in V1's rail are directional; the label-filter +
  filter-reset UX needs its own pass.
- Command palette internals are described above but not mocked.
