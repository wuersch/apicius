import type { CapabilityContractResponse } from '@/api/model'
import { AnswersFacet } from '@/components/capability/AnswersFacet'
import { PagingFacet } from '@/components/capability/PagingFacet'
import { FieldRow } from '@/components/editor/FieldRow'

// FEAT-009 AC1: the contract in the stable facet order — identity, description, Request,
// Paging (FEAT-010), Headers, Answers — plain language primary, serialized detail secondary
// (PRIN-002). A facet that doesn't apply is absent, never shown empty (AC2). Everything
// rendered is the backend's projection of the document.
export function CapabilityContract({
  specId,
  schemaName,
  contract,
}: {
  specId: string
  schemaName: string
  contract: CapabilityContractResponse
}) {
  const identity = contract.capability
  const noun = contract.singularNoun ?? ''
  // The plural, read from the derived collection path ("/order-items" → "order items") —
  // projected, not re-pluralized client-side.
  const pluralNoun = identity?.path?.split('/')[1]?.replace(/-/g, ' ') ?? ''

  return (
    <div className="flex min-w-0 flex-1 flex-col gap-3.5">
      <header className="mb-2.5">
        <div className="flex items-center gap-2.5">
          <h1 className="truncate text-3xl font-bold tracking-[-.02em]">{identity?.label}</h1>
          {/* The derived address as a quiet badge — the EditorPage version-chip idiom. */}
          <span className="shrink-0 rounded-sm bg-input px-2 py-0.5 font-mono text-[11px] text-text-tertiary">
            {identity?.method} {identity?.path}
          </span>
        </div>
        {contract.description && (
          <p className="mt-1.5 max-w-[560px] text-sm text-text-tertiary">
            {contract.description}
          </p>
        )}
      </header>

      {contract.request && (
        <section aria-label="Request" className="rounded-[10px] bg-card px-5 py-[17px] shadow-sm">
          <h2 className="text-[11px] font-semibold tracking-[.1em] text-text-tertiary uppercase">
            Request
          </h2>
          {contract.request.mergePatch ? (
            // AC5, Update: merge-patch semantics stated, nothing enumerated.
            <p className="mt-3 text-sm">
              Send only the fields you change — the rest of the {noun} stays as it is.
            </p>
          ) : (
            // AC5, Add: the resource's shape, the identity's auto visibility reading as
            // "the server sets it" through the locked row.
            <>
              <p className="mt-3 text-sm">
                Clients send the {noun}'s fields — the identifier is assigned by the server.
              </p>
              <ul className="mt-3 overflow-hidden rounded-md border border-band-border">
                {contract.request.fields?.map((field) => (
                  <FieldRow key={field.name} field={field} locked={field.name === 'id'} />
                ))}
              </ul>
            </>
          )}
        </section>
      )}

      {contract.paging && (
        <PagingFacet
          specId={specId}
          schemaName={schemaName}
          capability={contract.capability?.capability ?? 'BROWSE'}
          pluralNoun={pluralNoun}
          paging={contract.paging}
        />
      )}

      <section aria-label="Headers" className="rounded-[10px] bg-card px-5 py-[17px] shadow-sm">
        <h2 className="text-[11px] font-semibold tracking-[.1em] text-text-tertiary uppercase">
          Headers
        </h2>
        {/* FEAT-009 ships the request group only; FEAT-011 adds the authored columns. */}
        <div className="mt-3 text-[12px] font-semibold text-mono-derived">
          Read from the request
        </div>
        <ul className="mt-2 flex flex-col gap-[7px]">
          {contract.headers?.map((header) => (
            <li key={header.name} className="flex items-center gap-2">
              <span className="font-mono text-[12.5px]">{header.name}</span>
              <span className="text-[11px] text-mono-derived">{header.value}</span>
              {header.derived && (
                // AC3: the content-negotiation line is supplied, read-only — nothing in the
                // document carries it. The olive chip is the mockup's built-in marker.
                <span className="rounded-[4px] bg-olive-chip px-[7px] py-px text-[10px] font-bold text-olive-chip-foreground">
                  derived
                </span>
              )}
            </li>
          ))}
        </ul>
      </section>

      {contract.answers && (
        <AnswersFacet
          specId={specId}
          schemaName={schemaName}
          capability={contract.capability?.capability ?? 'BROWSE'}
          singularNoun={noun}
          answers={contract.answers}
        />
      )}
    </div>
  )
}
