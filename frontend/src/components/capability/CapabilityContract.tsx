import type { CapabilityContractResponse } from '@/api/model'
import { AnswersFacet } from '@/components/capability/AnswersFacet'
import { ErrorsFacet } from '@/components/capability/ErrorsFacet'
import { FiltersFacet } from '@/components/capability/FiltersFacet'
import { HeadersFacet } from '@/components/capability/HeadersFacet'
import { PagingFacet } from '@/components/capability/PagingFacet'
import { FieldRow } from '@/components/editor/FieldRow'

// FEAT-009 AC1: the contract in the stable facet order — identity, description, Request,
// Filters (query parameters, FEAT-011), Paging (FEAT-010), Headers, Answers, Errors —
// plain language primary, serialized detail secondary (PRIN-002). A facet that doesn't
// apply is absent, never shown empty (AC2); Filters and Headers apply everywhere, so their
// cards stand (empty ones recede). Everything rendered is the backend's projection of the
// document.
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
  const capability = identity?.capability ?? 'BROWSE'
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

      <FiltersFacet
        specId={specId}
        schemaName={schemaName}
        capability={capability}
        queryParameters={contract.queryParameters ?? []}
        pagingOn={contract.paging?.on ?? false}
      />

      {contract.paging && (
        <PagingFacet
          specId={specId}
          schemaName={schemaName}
          capability={capability}
          pluralNoun={pluralNoun}
          paging={contract.paging}
        />
      )}

      {contract.headers && (
        <HeadersFacet
          specId={specId}
          schemaName={schemaName}
          capability={capability}
          headers={contract.headers}
        />
      )}

      {contract.answers && (
        <>
          <AnswersFacet
            specId={specId}
            schemaName={schemaName}
            capability={capability}
            answers={contract.answers}
          />
          {(contract.answers.failures ?? []).length > 0 && (
            <ErrorsFacet
              specId={specId}
              schemaName={schemaName}
              capability={capability}
              singularNoun={noun}
              answers={contract.answers}
            />
          )}
        </>
      )}
    </div>
  )
}
