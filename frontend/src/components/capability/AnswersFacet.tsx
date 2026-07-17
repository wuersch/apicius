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

// The mockup's status palette (View 3 Answers card): the success badge wears the olive
// chip pairing; failure codes color 4xx ochre / 5xx rust inside neutral badges, phrases
// staying neutral.
function failureCodeColor(status: string): string {
  return status.startsWith('5') ? 'text-terracotta' : 'text-ochre'
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

      <div className="mt-2.5 flex items-center gap-[11px] py-1.5">
        <span className="rounded-[5px] bg-olive-chip px-[9px] py-[3px] font-mono text-[12px] font-bold text-olive-chip-foreground">
          {answers.successStatus}
        </span>
        <span className="text-[13.5px]">{answers.successDescription}</span>
      </div>

      {failures.length > 0 && (
        // The View 3 Answers card's thin rule separates the success answer from the
        // standard-errors row.
        <div className="mt-2 border-t border-border pt-3">
          <div className="flex items-center gap-2">
            <span className="text-[12px] font-semibold text-mono-derived">
              Standard errors
            </span>
            {on && (
              // The guarantee's badge — carried only while it holds; toggled off it goes
              // away entirely (the consequence line explains), never dims.
              <span className="rounded-[4px] bg-input px-2 py-px text-[10px] font-bold text-teal">
                RFC 9457 · added for you
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
