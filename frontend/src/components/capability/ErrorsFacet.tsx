import { Switch } from '@/components/ui/switch'
import { useAdoptStandardErrors, useRemoveStandardErrors } from '@/api/endpoints/specs/specs'
import type { AnswersFacetResponse, Capability } from '@/api/model'
import { useContractInvalidation } from '@/components/capability/useContractInvalidation'
import { failureName } from '@/lib/errorAnswers'

// The mockup's status palette: failure codes color 4xx ochre / 5xx rust inside neutral
// badges, phrases staying neutral.
function failureCodeColor(status: string): string {
  return status.startsWith('5') ? 'text-terracotta' : 'text-ochre'
}

// FEAT-009's standard failures in their own card (the v10 Answers/Errors split): Answers =
// what you design, Errors = the guaranteed failure contract, RFC 9457 badge and toggle in
// the heading. The ON/OFF toggle is the built-in default's deliberate override (UC5,
// PRIN-006) — plain, no confirm, reversible; OFF removes the badge (never dims it), the
// chips go dashed and faint, the consequence line explains (state 3·4 — the same
// recede-when-off rule as Paging). Both directions mutate then invalidate.
export function ErrorsFacet({
  specId,
  schemaName,
  capability,
  singularNoun,
  answers,
}: {
  specId: string
  schemaName: string
  capability: Capability
  singularNoun: string
  answers: AnswersFacetResponse
}) {
  const adopt = useAdoptStandardErrors()
  const remove = useRemoveStandardErrors()
  const failures = answers.failures ?? []
  const on = failures.every((failure) => failure.present)
  const pending = adopt.isPending || remove.isPending
  const invalidate = useContractInvalidation(specId, schemaName, capability)

  function handleToggle(next: boolean) {
    const mutation = next ? adopt : remove
    mutation.mutate({ specId, schemaName, capability }, { onSuccess: invalidate })
  }

  return (
    <section aria-label="Errors" className="rounded-[10px] bg-card px-5 py-[17px] shadow-sm">
      <div className="flex items-center gap-2">
        <h2 className="text-[11px] font-semibold tracking-[.1em] text-text-tertiary uppercase">
          Errors
        </h2>
        {on && (
          // The guarantee's badge — carried only while it holds; toggled off it goes away
          // entirely (the consequence line explains), never dims.
          <span className="rounded-[4px] bg-input px-2 py-px text-[10px] font-bold text-teal">
            RFC 9457 compliant
          </span>
        )}
        <Switch
          checked={on}
          disabled={pending}
          onCheckedChange={handleToggle}
          aria-label="Standard errors"
          // Rule-toggles signal ON in olive — the house-rule color, not the ink primary.
          className="ml-auto data-[state=checked]:bg-olive"
        />
      </div>
      <div className="mt-2.5 flex flex-wrap items-center gap-2">
        {failures.map((failure) => (
          <span
            key={failure.status}
            className={`rounded-md px-2.5 py-1 font-mono text-[12px] whitespace-nowrap ${
              failure.present
                ? 'bg-input'
                : 'border border-dashed border-ring text-text-faint'
            }`}
          >
            <span
              className={failure.present ? `font-bold ${failureCodeColor(failure.status ?? '')}` : 'font-bold'}
            >
              {failure.status}
            </span>{' '}
            {failureName(failure.status ?? '', singularNoun)}
          </span>
        ))}
      </div>
      {!on && (
        <p className="mt-2 text-[12.5px] text-hint">
          Failures answer without a shared shape — clients can't handle every error the same
          way.
        </p>
      )}
      {(adopt.isError || remove.isError) && (
        <p className="mt-2 text-[12.5px] text-terracotta" role="alert">
          That didn't save — try again.
        </p>
      )}
    </section>
  )
}
