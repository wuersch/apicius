import { useState } from 'react'
import { Plus } from 'lucide-react'
import type { Capability, DeclarationResponse } from '@/api/model'
import { DeclarationList, type Editing } from '@/components/capability/DeclarationList'

// FEAT-010's pair as this card accounts for it: while paging is on, the operation's query
// string genuinely carries page & limit, so the card lists them — read-only, wearing the
// mockup's combined provenance chip (state 3·5's "Built in · paging" grammar), which alone
// carries the not-yours meaning. Frontend vocabulary keyed on the projected on-state, like
// the Paging card's own copy — the document facts stay FEAT-010's.
const PAGING_ROWS = [
  { name: 'page', text: 'whole number — which page to return; starts at 1' },
  { name: 'limit', text: 'whole number — results per page; 1–100, normally 20' },
]

// FEAT-011: the capability's query parameters — "Filters visitors can use" (the card label
// is design's business; the model is the mechanism, not one use). The add affordance lives
// in the heading row, top right (v10's rule for titled cards). Empty, the card recedes like
// paging-off (dashed, quiet, one resting line) rather than disappearing — no card would
// mean no way to add the first filter — and turns solid with its first row (paging's
// built-in pair counts as rows).
export function FiltersFacet({
  specId,
  schemaName,
  capability,
  queryParameters,
  pagingOn,
}: {
  specId: string
  schemaName: string
  capability: Capability
  queryParameters: DeclarationResponse[]
  /** While the capability pages, page/limit are paging's — listed locked, blocked inline. */
  pagingOn: boolean
}) {
  const [editing, setEditing] = useState<Editing>(null)
  const empty = queryParameters.length === 0 && editing === null && !pagingOn

  if (empty) {
    return (
      <section
        aria-label="Filters"
        className="rounded-[10px] border border-dashed border-control-border bg-card px-5 py-3"
      >
        <div className="flex items-center gap-2">
          <h2 className="text-[11px] font-semibold tracking-[.1em] text-hint uppercase">
            Filters visitors can use
          </h2>
          <span className="text-[12.5px] text-hint">none — the list comes as-is</span>
          <AddFilter onClick={() => setEditing({ mode: 'add' })} />
        </div>
      </section>
    )
  }

  return (
    <section aria-label="Filters" className="rounded-[10px] bg-card px-5 py-[17px] shadow-sm">
      <div className="flex items-baseline justify-between">
        <h2 className="text-[11px] font-semibold tracking-[.1em] text-text-tertiary uppercase">
          Filters visitors can use
        </h2>
        {editing === null && <AddFilter onClick={() => setEditing({ mode: 'add' })} />}
      </div>
      <div className="mt-1">
        {pagingOn && (
          <ul>
            {PAGING_ROWS.map((row) => (
              <li
                key={row.name}
                className="flex items-center gap-3 border-b border-border py-[9px]"
              >
                <span className="w-[140px] shrink-0 font-mono text-[12.5px] font-medium text-mono-derived">
                  {row.name}
                </span>
                <span className="min-w-0 flex-1 text-[13.5px] text-text-tertiary">
                  {row.text}
                </span>
                <span className="rounded-[4px] bg-olive-chip px-[7px] py-px text-[10px] font-bold text-olive-chip-foreground">
                  Built in · paging
                </span>
              </li>
            ))}
          </ul>
        )}
        <DeclarationList
          specId={specId}
          schemaName={schemaName}
          capability={capability}
          location="query-parameter"
          declarations={queryParameters}
          showOptionality
          pagingOn={pagingOn}
          editing={editing}
          onEditingChange={setEditing}
        />
      </div>
    </section>
  )
}

function AddFilter({ onClick }: { onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="ml-auto flex items-center gap-1.5 text-[12.5px] font-semibold text-text-tertiary transition-colors hover:text-foreground"
    >
      <Plus aria-hidden className="size-[11px]" strokeWidth={2.4} />
      Add a filter
    </button>
  )
}
