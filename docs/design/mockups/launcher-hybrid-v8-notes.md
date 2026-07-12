# Design notes — Launcher Hybrid v8 (API card actions)

> Companion to `launcher-hybrid-v8.html` (mockup — everything in v7, plus **View 1g**:
> the API card's overflow menu, Edit details dialog, and delete confirmation).
> v8 = v7 unchanged + view **1g**. Everything in
> [`launcher-hybrid-v3-notes.md`](launcher-hybrid-v3-notes.md),
> [`launcher-hybrid-v4-notes.md`](launcher-hybrid-v4-notes.md),
> [`launcher-hybrid-v6-notes.md`](launcher-hybrid-v6-notes.md), and
> [`launcher-hybrid-v7-notes.md`](launcher-hybrid-v7-notes.md) still applies.
> Depends on: PRIN-002, PRIN-003, PRIN-006.

## Views ↔ frames (new in v8)

| View | `data-screen-label` | Scope |
|---|---|---|
| 1g·1 · Overflow menu | `launcher 1g·1 — card overflow menu` | API card (Payments API) with the ⋯ menu open |
| 1g·2 · Edit details | `launcher 1g·2 — edit details dialog` | Create-dialog shell reused for editing `info` |
| 1g·3 · Delete confirm | `launcher 1g·3 — delete confirm` | Type-the-name confirmation, export escape hatch |

## Overflow menu (1g·1)

The ⋯ menu on a launcher card operates on the **API as an object**, never its contents
(same noun/contents split as fields): **Edit details…** · **Duplicate** · ─ ·
**Download as YAML** · **Download as JSON** · ─ · **Delete…** (danger, isolated).

- Download is **two direct items, no intermediate dialog** — a two-choice format pick
  doesn't earn a dialog. If per-download options ever appear (OpenAPI version conversion,
  resolved refs), it graduates to one.
- Deliberately absent: editor-level actions (add resource, validate), and Rename as a
  separate item — renaming lives in Edit details.
- Plausible later: Share/access, History, Archive (would soften Delete to plain confirm).

## Edit details (1g·2)

Same shell as the Create API dialog (header/footer bands, 640px), but:

- **Title + Version side by side** — `info.version` is editable (bumping it is the most
  common reason to open this); starts from the current value, mono.
- **No template/import step** — birth-time decision only.
- **OpenAPI spec version shown but locked** — same row treatment as the shape table's
  `id` row (muted, lock glyph, "fixed after creation"). Conversion is a real feature,
  not specced; when it exists this row becomes actionable.
- Footer: "Rewrites `info` only." + **Save changes**. No house-rules banner re-run.

## Delete confirm (1g·3)

APIs are not recoverable (no archive yet) → **type-the-name** pattern:

- Header states the blast radius in plain language (resources, capabilities, "no undo").
- Primary **Delete forever** (danger color) disabled until the typed name matches.
- **Download as YAML** escape hatch in the footer — the lossless model (PRIN-003) means
  a download *is* a full backup; offer it at the moment of destruction.
- If Archive ever ships, this downgrades to a plain confirm dialog.

## Open questions

- All v7 open questions carry over.
- Duplicate: naming of the copy ("Payments API (copy)"?), and whether it opens the
  Edit details dialog pre-filled — undesigned.
- Where do card actions live in list view (same menu, same order)?
