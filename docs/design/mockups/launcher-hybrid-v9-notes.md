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

- **Implementation correction (from first Claude Code pass):** standard errors are a
  house rule (PRIN-006) — they are *built in by default*, rendered as resting chips
  inside Answers ("400 we couldn't read the request" …), never behind an
  "Add standard errors" button or an "available — not answered yet" state. If an
  opt-out is ever wanted, reuse the paging ON/OFF toggle pattern. Dashed borders mean
  "off/empty" in this vocabulary (see 3·2) — don't wrap present content in them.
  When the list wraps to multiple lines, the codes lose their column rhythm — render
  each error as a discrete chip (code + phrase in one pill, as in the mockup's Answers
  card) laid out with flex + gap, so wrapping stays tidy; don't rely on text-flow
  spacing between code/label pairs. Keep the mockup's separator line between the 200
  answer row and the standard-errors row (thin border-top, see the Answers card in
  View 3). Status codes are color-coded as in the mockup: <b>200</b> olive green
  (#5E6A3F on its tint), <b>4xx</b> ochre (#9A6B33), <b>500</b> rust (#A65532) — the
  code number carries the color, the label stays neutral. Rule-toggles use the olive
  green ON state (#5E6A3F), same as the paging toggle. The "Standard errors" row label
  uses the body font, 12px semibold, muted brown (#8A7B63) — not mono, not uppercase —
  and carries the <b>RFC 9457</b> badge when enabled; when toggled off the badge
  is removed entirely (the guarantee no longer holds — the consequence line explains
  it), never dimmed. Disabled sections must recede, not shout: faint dashed border
  (#D8CAAE-weight, not a dark heavy dash), muted text, no bold phrases; ideally a
  disabled card collapses to one quiet line ("Paging — off · whole list in one
  response" + toggle) and shows the full consequence copy only on hover/focus. An
  off card should never be the most prominent element on the page.
- All v8 open questions carry over.
- Action capabilities (#10): page layout, section set, HTTP mapping (verb-as-subresource
  vs. POST on the id segment) — undesigned.
- Examples pass (v10): quiet affordance for `example` values on fields/resources.
- Palette: what does a materialized Sorting/Search card look like? Removal affordance?
- Scroll-spy behavior when a section is added/removed mid-edit.
