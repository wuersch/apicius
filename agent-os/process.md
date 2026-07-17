# Process — the SDLC loop

How any change moves from idea to shipped. Written as steps to follow.

## Principle: a ratchet, not a loop
Ownership moves forward, never syncs backward:

```
mock-up owns intent  →  until shipped  →  running app owns truth  →  next change forks a fresh mock-up
```

Don't chase mock-up↔code sync. The mock-up is a thinking/approval artifact until the
feature ships; after that, the running app is the source of truth and the mock-up
freezes as a dated record of intent. Mock-ups are not authoritative: the spec owns the
requirements; a mock-up suggests a possible direction, and a feature may diverge from or
drop mock-up elements.

## Steps
1. **Frame** — write/update the feature spec from `agent-os/templates/_feature.md`.
   Give it an ID, `Status: proposed`, and a `Depends on:` line citing the PRIN/ADR it
   touches. State Non-Goals explicitly. Surface unknowns as `[NEEDS CLARIFICATION: …]`
   rather than guessing.
2. **Design** (UI features only) — mock-ups are provided by the user (a separate
   design-focused session), not authored in this loop. The user points to the mock-up file
   and view during feature development; the spec's `Mockup:` line records that pointer.
   `Status → specced` once the spec's decisions are settled — no `[NEEDS CLARIFICATION]`
   markers may remain at this gate.
3. **Decide** — if a non-obvious technical choice appears, log an ADR
   (`agent-os/templates/_adr.md`) and cite it from the feature. Log an ADR only if the
   choice is about how the system is built; a non-obvious choice about what the system
   *produces* belongs in the feature spec (non-obvious ≠ architectural).
4. **Build** — branch first (`feat/FEAT-NNN-slug`, or `fix/slug` for fixes); never commit to
   `main` directly. Implement in the real stack; do **not** paste mock-up HTML. Keep the domain
   model lossless. `Status → building`.
5. **Verify** — write the test first and watch it fail, then implement (RED → GREEN); every
   acceptance criterion has a test; house-rule behavior you touched has a test.
6. **Ship** — open a PR; merge once review passes and tests are green. Keep `main` linear:
   rebase, no merge commits; delete and prune the branch after. `Status → shipped`; the app is
   now source of truth. Update `docs/spec/features.md`.

## Definition of done
AC met & tested · touched house-rule behavior tested · lossless round-trip intact · ADR logged if warranted ·
`features.md` current · shipped via rebased PR (tests green).

## Loading discipline (for agents — this is how context stays small)
`CLAUDE.md` → `docs/spec/features.md` (index) → the one `FEAT-` → only its cited
`PRIN/ADR`. Never pre-load the whole corpus. The ID citations exist so you can pull the
minimal slice.
