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
