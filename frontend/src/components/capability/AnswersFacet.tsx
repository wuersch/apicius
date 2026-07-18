import { useState } from 'react'
import { ChevronDown, Plus } from 'lucide-react'
import { useUpdateSuccessAnswerDescription } from '@/api/endpoints/specs/specs'
import type { AnswersFacetResponse, Capability } from '@/api/model'
import { DeclarationList, type Editing } from '@/components/capability/DeclarationList'
import { useContractInvalidation } from '@/components/capability/useContractInvalidation'
import { QuietDescription } from '@/components/QuietDescription'

// FEAT-009's success answer, reworked to v10's grammar: answers collapse to one line each —
// code chip · sentence · "+ N headers" hint · chevron — and the expansion holds that
// answer's own response headers ("Headers on this Answer only", state 3·5): spec-accurate,
// a 200 can return a Sync-Token while a 401 never does (FEAT-011 AC3). The failures moved
// to their own Errors card (ErrorsFacet).
export function AnswersFacet({
  specId,
  schemaName,
  capability,
  answers,
}: {
  specId: string
  schemaName: string
  capability: Capability
  answers: AnswersFacetResponse
}) {
  const [expanded, setExpanded] = useState(false)
  const [editing, setEditing] = useState<Editing>(null)
  const headers = answers.successHeaders ?? []
  const updateDescription = useUpdateSuccessAnswerDescription()
  const invalidate = useContractInvalidation(specId, schemaName, capability)
  const toggle = () => setExpanded((current) => !current)

  return (
    <section aria-label="Answers" className="rounded-[10px] bg-card px-5 py-[17px] shadow-sm">
      <h2 className="text-[11px] font-semibold tracking-[.1em] text-text-tertiary uppercase">
        Answers
      </h2>

      {/* The one-line answer, split so the sentence edits in place (FEAT-012 UC2) while
          chip and chevron keep the expand gesture — the chevron button is the chip's
          double, hidden from the accessibility tree. */}
      <div className="mt-1 flex w-full items-center gap-[11px] py-1.5">
        <button
          type="button"
          onClick={toggle}
          aria-expanded={expanded}
          aria-label="Answer details"
          className="shrink-0"
        >
          <span className="block rounded-[5px] bg-olive-chip px-[9px] py-[3px] font-mono text-[12px] font-bold text-olive-chip-foreground">
            {answers.successStatus}
          </span>
        </button>
        <div className="flex min-w-0 flex-1 items-baseline gap-1.5 text-[13.5px]">
          {/* Blank saves restore the derived wording — an answer is never undescribed. */}
          <QuietDescription
            value={answers.successDescription}
            ariaLabel="answer description"
            className="min-w-0"
            onSave={(description) =>
              updateDescription
                .mutateAsync({
                  specId,
                  schemaName,
                  capability,
                  data: { description: description ?? undefined },
                })
                .then(invalidate)
            }
          />
          {!expanded && headers.length > 0 && (
            <span className="shrink-0 text-[11.5px] text-hint">
              + {headers.length} {headers.length === 1 ? 'header' : 'headers'}
            </span>
          )}
        </div>
        <button type="button" onClick={toggle} tabIndex={-1} aria-hidden="true">
          <ChevronDown
            strokeWidth={2.2}
            className={`size-[13px] shrink-0 text-hint transition-transform ${expanded ? 'rotate-180' : ''}`}
          />
        </button>
      </div>

      {expanded && (
        // The answer's own region, set off by the left rail (state 3·5).
        <div className="mt-0.5 mb-2 ml-[15px] border-l-2 border-band-border py-1 pl-5">
          <div className="flex items-baseline justify-between">
            <h3 className="text-[11px] font-semibold tracking-[.1em] text-hint uppercase">
              Headers on this answer only
            </h3>
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
          {headers.length === 0 && editing === null ? (
            <p className="mt-1.5 text-[12.5px] text-hint">
              None yet — this answer ships no extra headers.
            </p>
          ) : (
            <DeclarationList
              specId={specId}
              schemaName={schemaName}
              capability={capability}
              location="response-header"
              declarations={headers}
              requiredLabel="always sent"
              editing={editing}
              onEditingChange={setEditing}
            />
          )}
        </div>
      )}
    </section>
  )
}
