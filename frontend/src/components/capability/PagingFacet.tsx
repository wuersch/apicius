import { useQueryClient } from '@tanstack/react-query'
import { Switch } from '@/components/ui/switch'
import {
  getGetCapabilityContractQueryKey,
  getGetLastEditedLocationQueryKey,
  getGetSpecQueryKey,
  getListSpecsQueryKey,
  useDisablePaging,
  useEnablePaging,
} from '@/api/endpoints/specs/specs'
import type { Capability, PagingFacetResponse } from '@/api/model'

// FEAT-010: how the list pages — built-in behavior (PRIN-006), never parameters the designer
// authors. The ON/OFF toggle is the deliberate per-capability opt-out (UC2/UC3): plain, no
// confirm, reversible; OFF turns the whole card dashed/neutral (mockup state 3·2) with the
// consequence stated. A designer-authored page/limit parameter blocks enabling (UC5) — the
// projection names it, the switch locks, and the copy says why. Both directions mutate then
// invalidate (the AnswersFacet convention) — nothing echoed locally.
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
  const queryClient = useQueryClient()
  const enable = useEnablePaging()
  const disable = useDisablePaging()
  const on = paging.on ?? false
  const conflicts = paging.conflicts ?? []
  const pending = enable.isPending || disable.isPending

  function invalidate() {
    // The page re-renders from the invalidated projections — nothing echoed locally.
    queryClient.invalidateQueries({
      queryKey: getGetCapabilityContractQueryKey(specId, schemaName, capability),
    })
    queryClient.invalidateQueries({ queryKey: getGetSpecQueryKey(specId) })
    queryClient.invalidateQueries({ queryKey: getListSpecsQueryKey() })
    queryClient.invalidateQueries({ queryKey: getGetLastEditedLocationQueryKey() })
  }

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
          : // Dashed means off/empty in this vocabulary — the whole card, not chips (3·2).
            'rounded-[10px] border-2 border-dashed border-ghost bg-card p-5'
      }
    >
      <div className="flex items-center gap-2">
        <h2
          className={`text-[11px] font-semibold tracking-[.1em] uppercase ${
            on ? 'text-olive-chip-foreground' : 'text-text-tertiary'
          }`}
        >
          Paging
        </h2>
        {on && (
          // The built-in guarantee's badge — carried only while it holds; toggled off it
          // goes away entirely (the consequence line explains), never dims.
          <span className="rounded-[5px] bg-card px-2 py-px text-[11px] font-bold text-olive-chip-foreground">
            Built in
          </span>
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

      {on ? (
        <>
          <p className="mt-2 text-sm text-olive-chip-foreground">
            Results come back 20 at a time — a <b>wrapped list</b>, never a bare array, so
            answers can grow without breaking anyone. Visitors pass{' '}
            <span className="font-mono text-[12.5px]">page</span> &{' '}
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
      ) : (
        <p className="mt-2 text-sm text-text-tertiary">
          Visitors get the <b>whole list in one response</b> — still wrapped, just not in
          pages. Fine for a small, bounded set; risky as the {pluralNoun} grow. Flip it back
          on any time.
        </p>
      )}

      {!on && conflicts.length > 0 && (
        // UC5: the blocked enable, named — the document already claims the parameter name.
        <p className="mt-2 text-[12.5px] text-hint">
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
      {(enable.isError || disable.isError) && (
        <p className="mt-2 text-[12.5px] text-terracotta" role="alert">
          That didn't save — try again.
        </p>
      )}
    </section>
  )
}
