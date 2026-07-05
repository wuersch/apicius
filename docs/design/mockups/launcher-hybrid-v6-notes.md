# Design notes — Launcher Hybrid v6 (New API menu · brand mark)

> Companion to `launcher-hybrid-v6.html` (mockup — launcher, Create API dialog, New API
> menu, editor, operation — each light + dark).
> v6 = v4's launcher + dialog, with the **"New API" menu** consolidated from the v5
> exploration and the **final brand mark** applied. Everything in
> [`launcher-hybrid-v3-notes.md`](launcher-hybrid-v3-notes.md) (layout grid, tokens,
> semantics) and [`launcher-hybrid-v4-notes.md`](launcher-hybrid-v4-notes.md) (Create API
> dialog) still applies and is not repeated here.
> Depends on: FEAT-002, FEAT-003, FEAT-004, PRIN-001, PRIN-002, PRIN-003.

## Views ↔ frames

| View | `data-screen-label` | Scope |
|---|---|---|
| 1a · Workspace launcher | `apicius launcher v6 · 1a` (+ `— dark`) | All-APIs home, nothing open |
| 1b · Create API dialog | `apicius launcher v6 · 1b — create dialog` (+ ` dark`) | FEAT-003 dialog open (see v4 notes) |
| 1c · "New API" menu | `apicius launcher v6 · 1c — New API menu` (+ ` — dark`) | The creation entry menu open |
| 1d · Import picker | `apicius launcher v6 · 1d — import picker` (+ ` dark`) | Import dialog: drop zone / browse / URL |
| 1e · Import reading | `apicius launcher v6 · 1e — import reading` (+ ` dark`) | Processing state while the spec is read |
| 1f · Import review | `apicius launcher v6 · 1f — import review` (+ ` dark`) | 1b-style confirm with detected title/description/version |
| 2 · Editor | `apicius editor — Storefront API` (+ ` — dark`) | Resource-first editor shell (v3 notes) |
| 3 · Operation | `apicius operation — Browse all products` (+ ` — dark`) | Operation detail (v3 notes) |

## Settled from v5 (exploration closed)

v5 explored three entries for creation: split-button menu (1a), a ghost "Import" tile in
the API grid (1b), and an overflow menu (1c). **Settled: the CTA is a single dark
"New API ▾" button beside the greeting, and *import lives inside its menu*.** The grid
stays reserved for real APIs — no ghost tiles; creation affordances never masquerade as
content (PRIN-001).

## The "New API" menu (view 1c)

- **Anatomy:** anchored to the CTA (relative wrapper), panel at top 50px / right 0,
  **296px wide**, radius 10, padding 7, z-index 6. Items: 34px icon tile (radius 7, inset
  bg) + 13.5px/700 title + one-line 12px description.
- **Two items only** — "Start from scratch" (→ Create API dialog, view 1b, FEAT-003) and
  "Import a spec" (→ OpenAPI import; round-trip stays lossless per PRIN-003). Copy is
  plain-language, never HTTP or file-format jargon (PRIN-002).
- **Item hover:** `#F3EBDA` light / `#46392A` dark. **Elevation:**
  `0 4px 10px rgba(74,54,32,.10), 0 28px 64px rgba(74,54,32,.24)` — the strongest shadow
  on the screen; nothing else competes while the menu is open.
- **Behavior (for implementation):** opens on trigger click; closes on outside click, Esc,
  or selection; trigger carries `aria-expanded`. Keyboard: ArrowUp/Down cycles, Enter/Space
  selects, focus returns to the trigger on close. Build on the shadcn/radix DropdownMenu
  primitive.

## The import flow (views 1d → 1e → 1f)

Entry: "Import a spec" in the New API menu (1c). Three dialog states on the same scrim/card
shell as 1b:

- **1d · picker (640px):** one dashed drop zone (drag, "browse files" inline link, or paste
  raw YAML/JSON directly — the drop zone is also FEAT-004's paste entry;
  `YAML or JSON · up to 10 MB` in mono), an "or" divider, and a **From a URL** field with a
  Fetch button (URL renders in JetBrains Mono). Footer states the lossless promise up front:
  "Everything in your file is kept — even fields apicius doesn't show" (PRIN-003). No
  primary button — the actions *are* the drop/browse/fetch affordances; Cancel only.
- **1e · reading (480px):** spinner + filename chip (`storefront-api.yaml · 48 KB`) + a
  three-step trace — Parsed (done, olive check) → Checking house rules (current) →
  Preparing your review (pending). Footer: "Nothing is added to your workspace until you
  confirm." Cancel stays available throughout.
- **1f · review (640px, mirrors 1b):** summary chips (`5 resources · 21 operations ·
  3 schemas` + "All fields preserved") · Title and Description pre-filled, each marked
  `from your file` in mono · **OpenAPI version is detected, not picked** — a single
  check-marked card ("Detected" chip) replaces 1b's radio picker; caption points to a
  later Settings upgrade path. Primary action: **Add to workspace →**.

Vocabulary stays plain (PRIN-002): "spec", "your file" — never "parse", "deserialize", or
format jargon in user-facing copy (the step trace says "Parsed — OpenAPI 3.0, valid" as the
one deliberate exception, since version detection is the payload of that step).

## Brand mark (new in v6)

- The masthead logo is now the **custom olive mark**: a rounded-square frame with three
  node circles sitting *on* the border (API surface = where connections live), holding an
  olive twig — one olive, three leaves. Drawn in Affinity; original vector at
  `uploads/custom-olive-1127c4e9.svg`.
- Shipped tints: `icons/tinted/apicius-logo-light.svg` (`#5E6A3F`) and
  `…-dark.svg` (`#97A860`). One geometry, two tints — integrate via `currentColor` in the
  stack so themes need no asset swap. Renders 22px in the masthead lockup, gap tightened
  −3px against the wordmark (square canvas carries no side bearing).
- Replaces the hand-drawn twig from v3–v5. Candidate exploration (noun-project icons,
  amphoras, temples, circuit-olives) is archived in
  [`../../../Launcher 1a - Icon Options.dc.html`](../../../Launcher%201a%20-%20Icon%20Options.dc.html).

## Working-source tweaks (not shipping UI)

`Launcher Hybrid v6.dc.html` exposes mockup-only knobs: dialog version + title-error state
(view 1b, see v4 notes), menu open/closed (view 1c), activity panel visibility, and the
greeting first-name length stressor. The real app derives all of these from state.

## Open questions

- Post-create landing (empty-API editor state) — still unmocked, carried from v4 notes.
- Import error states (unparseable file, unreachable URL, unsupported version) — the happy
  path is mocked (1d–1f); failures are not.
- Post-import landing — 1f's "Add to workspace" should land on the imported API's editor;
  unmocked, same as post-create.
