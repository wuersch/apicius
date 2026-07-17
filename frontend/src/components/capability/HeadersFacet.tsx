import { useState } from 'react'
import { Plus } from 'lucide-react'
import type { Capability, HeadersFacetResponse } from '@/api/model'
import { DeclarationList, type Editing } from '@/components/capability/DeclarationList'

// FEAT-011: the Headers facet — request headers only (response headers live with the
// success answer they ship with). The derived content-negotiation line leads, read-only
// with its chip (FEAT-009 AC3 — display, not document content); authored rows follow in the
// shared Filters row grammar. Never empty: the derived line always shows, so the card never
// needs the receding treatment.
export function HeadersFacet({
  specId,
  schemaName,
  capability,
  headers,
}: {
  specId: string
  schemaName: string
  capability: Capability
  headers: HeadersFacetResponse
}) {
  const [editing, setEditing] = useState<Editing>(null)
  const derived = headers.derived ?? []
  const authored = headers.authored ?? []

  return (
    <section aria-label="Headers" className="rounded-[10px] bg-card px-5 py-[17px] shadow-sm">
      <div className="flex items-baseline justify-between">
        <h2 className="text-[11px] font-semibold tracking-[.1em] text-text-tertiary uppercase">
          Request headers
        </h2>
        {editing === null && (
          <button
            type="button"
            onClick={() => setEditing({ mode: 'add' })}
            className="flex items-center gap-1.5 text-[12.5px] font-semibold text-text-tertiary transition-colors hover:text-foreground"
          >
            <Plus aria-hidden className="size-[11px]" strokeWidth={2.4} />
            Add header
          </button>
        )}
      </div>
      <ul className="mt-1">
        {derived.map((header) => (
          <li
            key={header.name}
            className={`flex items-center gap-3 py-[9px] ${authored.length > 0 || editing !== null ? 'border-b border-border' : ''}`}
          >
            <span className="w-[140px] shrink-0 font-mono text-[12.5px] font-medium">
              {header.name}
            </span>
            <span className="min-w-0 flex-1 text-[13.5px] text-text-tertiary">
              {header.value}
            </span>
            {/* AC3: supplied, read-only — nothing in the document carries it. */}
            <span className="rounded-[4px] bg-olive-chip px-[7px] py-px text-[10px] font-bold text-olive-chip-foreground">
              derived
            </span>
          </li>
        ))}
      </ul>
      <DeclarationList
        specId={specId}
        schemaName={schemaName}
        capability={capability}
        location="request-header"
        declarations={authored}
        showOptionality
        editing={editing}
        onEditingChange={setEditing}
      />
    </section>
  )
}
