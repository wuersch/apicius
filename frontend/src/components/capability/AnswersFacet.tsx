import { useQueryClient } from '@tanstack/react-query'
import { Switch } from '@/components/ui/switch'
import {
  getGetCapabilityContractQueryKey,
  getGetLastEditedLocationQueryKey,
  getGetSpecQueryKey,
  getListSpecsQueryKey,
  useAdoptStandardErrors,
  useRemoveStandardErrors,
} from '@/api/endpoints/specs/specs'
import type { AnswersFacetResponse, Capability } from '@/api/model'
import { failureName } from '@/lib/errorAnswers'

// The mockup's status palette: 2xx olive, 4xx ochre, 5xx rust — the code carries the
// color, the phrase stays neutral.
function statusColor(status: string): string {
  if (status.startsWith('2')) return 'text-olive'
  if (status.startsWith('5')) return 'text-terracotta'
  return 'text-ochre'
}

// FEAT-009: what comes back — the success answer the document declares, then the standard
// failures as resting badges (the FieldRow chip idiom): derived furniture, not a feature
// banner. The ON/OFF
// toggle is the built-in default's deliberate override (UC5, PRIN-006) — plain, no
// confirm, reversible; OFF leaves the chips dashed and faint with the consequence stated.
// Both directions mutate then invalidate (the FieldEditor convention).
export function AnswersFacet({
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
  const queryClient = useQueryClient()
  const adopt = useAdoptStandardErrors()
  const remove = useRemoveStandardErrors()
  const failures = answers.failures ?? []
  const on = failures.every((failure) => failure.present)
  const pending = adopt.isPending || remove.isPending

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
    const mutation = next ? adopt : remove
    mutation.mutate({ specId, schemaName, capability }, { onSuccess: invalidate })
  }

  return (
    <section aria-label="Answers" className="rounded-[10px] bg-card p-5 shadow-sm">
      <h2 className="text-[11px] font-semibold tracking-[.1em] text-text-tertiary uppercase">
        Answers
      </h2>

      <div className="mt-3 flex items-baseline gap-3">
        <span
          className={`rounded-[5px] bg-input px-2 py-px font-mono text-[12px] font-semibold ${statusColor(answers.successStatus ?? '')}`}
        >
          {answers.successStatus}
        </span>
        <span className="text-sm">{answers.successDescription}</span>
      </div>

      {failures.length > 0 && (
        // The View 3 Answers card's thin rule separates the success answer from the
        // standard-errors row.
        <div className="mt-3.5 border-t border-border pt-3.5">
          <div className="flex items-center gap-2">
            <span className="text-[11px] text-text-faint">standard errors</span>
            <Switch
              checked={on}
              disabled={pending}
              onCheckedChange={handleToggle}
              aria-label="Standard errors"
              // Rule-toggles signal ON in olive — the house-rule color, not the ink primary.
              className="ml-auto data-[state=checked]:bg-olive"
            />
          </div>
          <div className="mt-2 flex flex-wrap items-center gap-1.5">
            {failures.map((failure) => (
              <span
                key={failure.status}
                className={`rounded-[5px] px-2 py-px text-[11px] whitespace-nowrap ${
                  failure.present
                    ? 'bg-input font-semibold text-text-secondary'
                    : 'border border-dashed border-ring text-text-faint'
                }`}
              >
                <span className={`font-mono ${statusColor(failure.status ?? '')}`}>
                  {failure.status}
                </span>{' '}
                {failureName(failure.status ?? '', singularNoun)}
              </span>
            ))}
          </div>
          {!on && (
            <p className="mt-2 text-[12.5px] text-hint">
              Failures answer without a shared shape — clients can't handle every error the
              same way.
            </p>
          )}
          {(adopt.isError || remove.isError) && (
            <p className="mt-2 text-[12.5px] text-terracotta" role="alert">
              That didn't save — try again.
            </p>
          )}
        </div>
      )}
    </section>
  )
}
