import { useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  getGetCapabilityContractQueryKey,
  getGetLastEditedLocationQueryKey,
  getGetSpecQueryKey,
  getListSpecsQueryKey,
  useAdoptStandardErrors,
} from '@/api/endpoints/specs/specs'
import type { AnswersFacetResponse, Capability } from '@/api/model'
import { failureName } from '@/lib/errorAnswers'

// FEAT-009: what comes back — the success answer the document declares, then the standard
// failures with their plain-language names. Absent answers are shown as available (AC4);
// the one adopt action is the only writer (UC3), invalidate-then-reproject like every
// mutation (the FieldEditor convention).
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
  const failures = answers.failures ?? []
  const allPresent = failures.every((failure) => failure.present)

  function handleAdopt() {
    adopt.mutate(
      { specId, schemaName, capability },
      {
        onSuccess: () => {
          // The page re-renders from the invalidated projections — nothing echoed locally.
          queryClient.invalidateQueries({
            queryKey: getGetCapabilityContractQueryKey(specId, schemaName, capability),
          })
          queryClient.invalidateQueries({ queryKey: getGetSpecQueryKey(specId) })
          queryClient.invalidateQueries({ queryKey: getListSpecsQueryKey() })
          queryClient.invalidateQueries({ queryKey: getGetLastEditedLocationQueryKey() })
        },
      },
    )
  }

  return (
    <section aria-label="Answers" className="rounded-[10px] bg-card p-5 shadow-sm">
      <h2 className="text-[11px] font-semibold tracking-[.1em] text-text-tertiary uppercase">
        Answers
      </h2>

      <div className="mt-3 flex items-baseline gap-3">
        <span className="rounded-[5px] bg-input px-2 py-px font-mono text-[12px] font-semibold">
          {answers.successStatus}
        </span>
        <span className="text-sm">{answers.successDescription}</span>
      </div>

      {failures.length > 0 && (
        <div
          className={`mt-4 rounded-md border p-3.5 ${
            allPresent ? 'border-band-border' : 'border-dashed border-ring'
          }`}
        >
          <div className="flex items-baseline gap-2">
            <span className="text-[13px] font-semibold">Standard errors</span>
            <span className="text-[11px] text-text-faint">
              {allPresent ? 'RFC 9457 · added for you' : 'available — not answered yet'}
            </span>
          </div>
          <ul className="mt-2 flex flex-wrap gap-x-5 gap-y-1">
            {failures.map((failure) => (
              <li
                key={failure.status}
                className={`text-[13px] ${failure.present ? '' : 'text-text-faint'}`}
              >
                <span className="font-mono font-semibold">{failure.status}</span>{' '}
                {failureName(failure.status ?? '', singularNoun)}
              </li>
            ))}
          </ul>
          {!allPresent && (
            <div className="mt-3">
              <Button
                onClick={handleAdopt}
                disabled={adopt.isPending}
                className="h-8 rounded-md px-3 text-[12.5px] font-semibold"
              >
                <Plus aria-hidden className="size-3" />
                Add standard errors
              </Button>
              {adopt.isError && (
                <p className="mt-2 text-[12.5px] text-terracotta" role="alert">
                  That didn't save — try again.
                </p>
              )}
            </div>
          )}
        </div>
      )}
    </section>
  )
}
