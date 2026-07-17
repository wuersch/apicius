import type { CapabilityContractResponse } from '@/api/model'
import { AnswersFacet } from '@/components/capability/AnswersFacet'
import { FieldRow } from '@/components/editor/FieldRow'

// FEAT-009 AC1: the contract in the stable facet order — identity, description, Request,
// Headers, Answers — plain language primary, serialized detail secondary (PRIN-002). A
// facet that doesn't apply is absent, never shown empty (AC2). Everything rendered is the
// backend's projection of the document.
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

  return (
    <div className="flex min-w-0 flex-1 flex-col gap-4">
      <header>
        <div className="flex items-baseline gap-3">
          <h1 className="text-2xl font-bold">{identity?.label}</h1>
          <span className="font-mono text-[13px] whitespace-nowrap text-text-tertiary">
            {identity?.method} {identity?.path}
          </span>
        </div>
        {contract.description && (
          <p className="mt-1.5 max-w-xl text-sm text-text-tertiary">{contract.description}</p>
        )}
      </header>

      {contract.request && (
        <section aria-label="Request" className="rounded-[10px] bg-card p-5 shadow-sm">
          <h2 className="text-[11px] font-semibold tracking-[.1em] text-text-tertiary uppercase">
            Request
          </h2>
          {contract.request.mergePatch ? (
            // AC5, Update: merge-patch semantics stated, nothing enumerated.
            <p className="mt-2.5 text-sm">
              Send only the fields you change — the rest of the {noun} stays as it is.
            </p>
          ) : (
            // AC5, Add: the resource's shape, the identity's auto visibility reading as
            // "the server sets it" through the locked row.
            <>
              <p className="mt-2.5 text-sm">
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

      <section aria-label="Headers" className="rounded-[10px] bg-card p-5 shadow-sm">
        <h2 className="text-[11px] font-semibold tracking-[.1em] text-text-tertiary uppercase">
          Headers
        </h2>
        <ul className="mt-2.5">
          {contract.headers?.map((header) => (
            <li key={header.name} className="flex items-center gap-3 py-1">
              <span className="w-[110px] shrink-0 font-mono text-[12.5px] font-medium">
                {header.name}
              </span>
              <span className="flex-1 font-mono text-[12.5px] text-text-tertiary">
                {header.value}
              </span>
              {header.derived && (
                // AC3: the content-negotiation line is supplied, read-only — nothing in the
                // document carries it.
                <span className="rounded-[5px] bg-input px-2 py-px text-[11px] font-semibold text-text-secondary">
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
