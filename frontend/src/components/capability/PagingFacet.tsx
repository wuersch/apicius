import { Check } from 'lucide-react'
import { Switch } from '@/components/ui/switch'
import { useDisablePaging, useEnablePaging } from '@/api/endpoints/specs/specs'
import type { Capability, PagingFacetResponse } from '@/api/model'
import { useContractInvalidation } from '@/components/capability/useContractInvalidation'

// FEAT-010: how the list pages — built-in behavior (PRIN-006), never parameters the designer
// authors. The ON/OFF toggle is the deliberate per-capability opt-out (UC2/UC3): plain, no
// confirm, reversible. OFF recedes rather than shouts (v9 notes correction on state 3·2):
// one quiet dashed line stating the consequence, the elaboration only on hover/focus — an
// off card is never the most prominent element on the page. A designer-authored page/limit
// parameter blocks enabling (UC5) — the projection names it, the switch locks, and the copy
// says why. Both directions mutate then invalidate (the AnswersFacet convention) — nothing
// echoed locally.
export function PagingFacet({
  specId,
  schemaName,
  capability,
  pluralNoun,
  paging,
}: {
  specId: string
  schemaName: string
  capability: Capability
  pluralNoun: string
  paging: PagingFacetResponse
}) {
  const enable = useEnablePaging()
  const disable = useDisablePaging()
  const on = paging.on ?? false
  const conflicts = paging.conflicts ?? []
  const pending = enable.isPending || disable.isPending
  const invalidate = useContractInvalidation(specId, schemaName, capability)

  function handleToggle(next: boolean) {
    const mutation = next ? enable : disable
    mutation.mutate({ specId, schemaName, capability }, { onSuccess: invalidate })
  }

  return (
    <section
      aria-label="Paging"
      className={
        on
          ? 'rounded-[10px] bg-olive-chip p-5 shadow-sm'
          : // Dashed means off/empty — faint and collapsed, receding behind the live cards.
            'group rounded-[10px] border border-dashed border-control-border bg-card px-5 py-3'
      }
    >
      <div className="flex items-center gap-2">
        <h2
          className={`text-[11px] font-semibold tracking-[.1em] uppercase ${
            on ? 'text-olive-chip-foreground' : 'text-hint'
          }`}
        >
          Paging
        </h2>
        {on ? (
          // The built-in guarantee's badge — carried only while it holds; toggled off it
          // goes away entirely (the consequence line explains), never dims.
          <span className="inline-flex items-center gap-[5px] rounded-[5px] bg-card px-2 py-px text-[11px] font-bold text-olive-chip-foreground">
            <Check className="size-2.5" strokeWidth={2.6} />
            Built in
          </span>
        ) : (
          <span className="text-[12.5px] text-hint">off — whole list in one response</span>
        )}
        <Switch
          checked={on}
          disabled={pending || (!on && conflicts.length > 0)}
          onCheckedChange={handleToggle}
          aria-label="Paging"
          // Rule-toggles signal ON in olive — the house-rule color, not the ink primary.
          className="ml-auto data-[state=checked]:bg-olive"
        />
      </div>

      {on && (
        <>
          <p className="mt-2 text-sm text-olive-chip-foreground">
            Results come back <b>in pages</b> — by default 20 at a time, up to 100;
            visitors steer with <span className="font-mono text-[12.5px]">page</span> &{' '}
            <span className="font-mono text-[12.5px]">limit</span>.
          </p>
          <div className="mt-2.5 rounded-lg bg-card px-3.5 py-3 font-mono text-[12px] leading-[1.85] text-olive-chip-foreground">
            {'{'}
            <br />
            &nbsp;&nbsp;data: <b>[ …the {pluralNoun}… ]</b>
            <br />
            &nbsp;&nbsp;pagination:{' '}
            <span className="text-mono-derived">
              {'{ page, limit, totalItems, totalPages }'}
            </span>
            <br />
            {'}'}
          </div>
        </>
      )}

      {!on && (
        <div className="hidden group-focus-within:block group-hover:block">
          <p className="mt-1.5 text-[12.5px] text-hint">
            Fine for a small, bounded set; risky as the {pluralNoun} grow.
          </p>
          {conflicts.length > 0 && (
            // UC5: the blocked enable, named — the document already claims the parameter name.
            <p className="mt-1.5 text-[12.5px] text-hint">
              Can't switch on — this capability already has{' '}
              {conflicts.map((name, index) => (
                <span key={name}>
                  {index > 0 && ' and '}a query parameter named{' '}
                  <span className="font-mono">{name}</span>
                </span>
              ))}
              .
            </p>
          )}
        </div>
      )}
      {(enable.isError || disable.isError) && (
        <p className="mt-2 text-[12.5px] text-terracotta" role="alert">
          That didn't save — try again.
        </p>
      )}
    </section>
  )
}
