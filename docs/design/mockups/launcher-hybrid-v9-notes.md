# Design notes — Launcher Hybrid v9 (View 3 iteration: TOC, paging toggle, palette, quiet descriptions)

> Companion to `launcher-hybrid-v9.html` (mockup — everything in v8, plus this round's
> changes to View 3 and the editor's shape tables, and state cards 3·1–3·3).
> v9 = v8 unchanged + the changes below. Everything in
> [`launcher-hybrid-v3-notes.md`](launcher-hybrid-v3-notes.md) through
> [`launcher-hybrid-v8-notes.md`](launcher-hybrid-v8-notes.md) still applies.
> Depends on: PRIN-001, PRIN-002, PRIN-005, PRIN-006.

## Views ↔ frames (new/changed in v9)

| View | `data-screen-label` | Scope |
|---|---|---|
| View 3 (changed) | `apicius operation — Browse all products` (+ ` — dark`) | Nested TOC rail · Restock entry · paging toggle · palette affordance |
| 2a/2b (changed) | `apicius editor — Storefront API · 2a` / `· 2b …` (+ dark) | Quiet descriptions in the shape table + "2 of 5 described" counter |
| 3·1 · Palette open | `apicius operation state 3·1 — palette open` | "What else can visitors do here?" menu |
| 3·2 · Paging off | `apicius operation state 3·2 — paging off` | Opted-out card + Guidelines echo |
| 3·3 · Quiet descriptions | `descriptions state 3·3 — quiet docs` | Resting / hover-ghost / in-place editing rows |

## Capability rail: nested TOC (replaces the "Shape" nav section)

The active capability in "Product can…" expands to show its page sections indented
beneath it (Filters · Paging · Headers · Answers), connected by a left border. Scroll-spy
highlights the current section. Rationale: the TOC visibly *belongs* to the capability,
and the **stable section order is a consistency signal** — a missing section stands out
right where capabilities are compared. Rejected: TOC below the whole list (unanchored);
breadcrumb dropdown for capabilities (poor for complex APIs).

## Action-shaped capability (foreshadow of TODO #10)

"Restock a product" sits in the rail as a sixth capability with a **zap glyph instead of
a CRUD dot** — plain-language actions that aren't CRUD get their own mark, same list,
same treatment. Unspecced beyond the rail entry; its page (different section set under
the same stable-TOC idea) is future work.

## Paging: built in, per-capability opt-out (TODO #6/#7 resolution)

- **No API-level paging convention setting, no deviation flag** — dropped for simplicity.
  Paging is applied automatically wherever applicable (PRIN-006).
- The Paging card header carries a plain **ON/OFF toggle** next to the "Built in" badge.
  No confirm flow: cheap, non-destructive, reversible.
- OFF state (3·2): card turns dashed/neutral, copy states the consequence ("whole list in
  one response — fine for a small, bounded catalog; risky if products grow"). The
  Guidelines rail echoes it with a **"1 overridden"** badge — the override is visible,
  never silent.

## Palette: "What else can visitors do here?" (TODO #8 resolution)

A single dashed affordance below the last card offers the concern menu (3·1): Sorting
(`?sort=`) · Search (`?q=`) · Pick fields (`?fields=`) · Bulk changes. Picking one
materializes its card in the page (and its TOC entry); removable any time. Rejected:
pre-rendered empty cards for every concern — dormant chrome fights "simple APIs should
be simple". The palette is phrased as capability vocabulary, not spec vocabulary.

## Quiet descriptions (TODO #18)

Almost every OpenAPI element has a `description`; the UI must expose that **without
adding chrome**:

- An existing description renders as **plain muted body copy** after the kind
  ("text — what shoppers see in the catalog"). Click to edit in place.
- A missing one shows **nothing** until hover, where a faint dotted-underline ghost
  ("add a note for readers…") invites a note.
- In-place editing (3·3): thin tan underline + caret, hint "Plain words — this becomes
  docs and SDK comments". Enter saves, Esc cancels.
- One **completeness signal** per group, quiet, in the section header: "2 of 5 described".
- Same pattern everywhere: resources, capabilities, filters, headers, answers.
- **`example` is NOT a description** — OpenAPI's `example`/`examples` are a separate
  concept with their own semantics, edited separately (candidate for a v10 example pass).

## Open questions

- All v8 open questions carry over.
- Action capabilities (#10): page layout, section set, HTTP mapping (verb-as-subresource
  vs. POST on the id segment) — undesigned.
- Examples pass (v10): quiet affordance for `example` values on fields/resources.
- Palette: what does a materialized Sorting/Search card look like? Removal affordance?
- Scroll-spy behavior when a section is added/removed mid-edit.
