import { useState, type FormEvent, type KeyboardEvent } from 'react'
import { Switch } from '@/components/ui/switch'
import { KindDropdown } from '@/components/editor/KindDropdown'
import {
  useAddQueryParameter,
  useAddRequestHeader,
  useAddResponseHeader,
  useRemoveQueryParameter,
  useRemoveRequestHeader,
  useRemoveResponseHeader,
  useUpdateQueryParameter,
  useUpdateRequestHeader,
  useUpdateResponseHeader,
} from '@/api/endpoints/specs/specs'
import type {
  Capability,
  CoreType,
  DeclarationRequest,
  DeclarationResponse,
  ProblemDetail,
  Refinement,
} from '@/api/model'
import { CORE_TYPES, derivePropertyName, REFINEMENTS_BY_CORE_TYPE, serializedScalar } from '@/lib/fieldDerivation'
import {
  deriveHeaderName,
  isReservedHeaderName,
  oneOfValuesProblem,
  parseOneOfInput,
} from '@/lib/headerDerivation'

export type DeclarationLocation = 'query-parameter' | 'request-header' | 'response-header'

// The canonical editor copy, keyed off the violated rule — never echo the server's message
// text (contract wording ≠ UI copy).
const NAME_REQUIRED = "A name is required — it's what this declaration is called."
const NAME_UNDERIVABLE_PARAMETER =
  'Nothing derivable here — a parameter name needs letters or digits.'
const NAME_UNDERIVABLE_HEADER =
  'Nothing derivable here — header names use ASCII letters or digits.'
const NAME_RESERVED =
  'Accept, Content-Type and Authorization are managed by Apicius — pick another name.'
const NAME_PAGING_OWNED =
  'Paging owns page and limit on this capability — switch paging off to claim them.'
const NAME_CONFLICT_PARAMETER =
  'This capability already has a query parameter with this name — while paging is on, page and limit count too.'
const NAME_CONFLICT_HEADER = 'This capability already has a header with this name here.'

// FEAT-010's reserved pair, mirrored for the inline pre-check (backend: Paging.java). The
// reservation is document-state-dependent — it holds only while the capability pages.
const PAGING_PARAMETERS = ['page', 'limit']
const ONE_OF_EMPTY = '"One of" needs at least one value — separate them with commas.'
const ONE_OF_DUPLICATE = '"One of" values must be distinct.'

type FormError =
  | 'name-required'
  | 'name-underivable'
  | 'name-reserved'
  | 'name-paging-owned'
  | 'name-conflict'
  | 'one-of-invalid'
  | 'server'
  | null

type SaveVars = {
  specId: string
  schemaName: string
  capability: Capability
  data: DeclarationRequest
}
type MutationLike<V> = {
  mutate: (vars: V, options?: { onSuccess?: () => void; onError?: (e: unknown) => void }) => void
  isPending: boolean
}

// One hooks call per location keeps the rules of hooks intact; only the addressed trio is
// used. All three locations share DeclarationRequest — on a response header, `required`
// carries the "always sent" promise.
function useDeclarationMutations(location: DeclarationLocation): {
  add: MutationLike<SaveVars>
  update: MutationLike<SaveVars & { name: string }>
} {
  const mutations = {
    'query-parameter': { add: useAddQueryParameter(), update: useUpdateQueryParameter() },
    'request-header': { add: useAddRequestHeader(), update: useUpdateRequestHeader() },
    'response-header': { add: useAddResponseHeader(), update: useUpdateResponseHeader() },
  }
  return mutations[location]
}

// The removal trio, shared with the row's Remove action (remove lives where edit lives).
export function useDeclarationRemoval(location: DeclarationLocation): MutationLike<{
  specId: string
  schemaName: string
  capability: Capability
  name: string
}> {
  const mutations = {
    'query-parameter': useRemoveQueryParameter(),
    'request-header': useRemoveRequestHeader(),
    'response-header': useRemoveResponseHeader(),
  }
  return mutations[location]
}

// FEAT-011: the inline row editor — one anatomy for add and edit across all three locations,
// docked in the card at the row's position (the quiet-descriptions grammar, state 3·7: Enter
// saves, Esc cancels). Name first with the derived name previewed live (PRIN-002), the kind
// as a sentence — "one of …" swaps the refinement slot for a comma-separated values input —
// optionality everywhere, worded per location: inputs ask "Required", response headers ask
// "Always sent" (both serialize as the construct's `required`). Everything shown after a
// save is projected back through the contract invalidation, nothing echoed locally.
export function DeclarationEditor({
  specId,
  schemaName,
  capability,
  location,
  existingNames,
  pagingOn = false,
  declaration,
  onSaved,
  onClose,
}: {
  specId: string
  schemaName: string
  capability: Capability
  location: DeclarationLocation
  /** Every declared name in this location (AC6's collision set); the edited one is exempt. */
  existingNames: string[]
  /**
   * Whether the capability currently pages (FEAT-010): while it does, page/limit are the
   * Paging facet's constructs — excluded from the filter projection, so the collision set
   * can't catch them — and claiming them blocks inline (UC5).
   */
  pagingOn?: boolean
  /** The declaration being edited; absent in add mode. */
  declaration?: DeclarationResponse
  onSaved: () => void
  onClose: () => void
}) {
  const [name, setName] = useState(declaration?.name ?? '')
  const [description, setDescription] = useState(declaration?.description ?? '')
  const [kindChoice, setKindChoice] = useState<CoreType | 'ONE_OF'>(
    declaration?.oneOfValues ? 'ONE_OF' : (declaration?.coreType ?? 'TEXT'),
  )
  const [refinement, setRefinement] = useState<Refinement | null>(declaration?.refinement ?? null)
  const [oneOfInput, setOneOfInput] = useState(declaration?.oneOfValues?.join(', ') ?? '')
  const [required, setRequired] = useState(declaration?.required ?? false)
  const [error, setError] = useState<FormError>(null)

  const { add, update } = useDeclarationMutations(location)
  const pending = add.isPending || update.isPending

  const isHeader = location !== 'query-parameter'
  // The plain-language face of `required`: what a visitor must send vs. what the answer is
  // promised to carry.
  const requiredLabel = location === 'response-header' ? 'Always sent' : 'Required'
  const oneOf = kindChoice === 'ONE_OF'
  const refinements = oneOf ? [] : REFINEMENTS_BY_CORE_TYPE[kindChoice]

  const trimmedName = name.trim()
  const derivedName = isHeader ? deriveHeaderName(trimmedName) : derivePropertyName(trimmedName)
  const oneOfValues = parseOneOfInput(oneOfInput)

  // Pre-checks against the loaded projection and the shared rules; the server stays
  // authoritative for other clients (AC6/AC7).
  const reserved = isHeader && derivedName !== '' && isReservedHeaderName(derivedName)
  const pagingOwned =
    !isHeader && pagingOn && PAGING_PARAMETERS.includes(derivedName.toLowerCase())
  const conflicts = existingNames.some(
    (existing) =>
      existing !== declaration?.name && existing.toLowerCase() === derivedName.toLowerCase(),
  )
  const oneOfProblem = oneOf ? oneOfValuesProblem(oneOfValues) : null

  const nameError =
    error === 'name-required' ? NAME_REQUIRED
    : error === 'name-underivable' ? underivableCopy(isHeader)
    : error === 'name-reserved' || (derivedName !== '' && reserved) ? NAME_RESERVED
    : error === 'name-paging-owned' || pagingOwned ? NAME_PAGING_OWNED
    : error === 'name-conflict' || (derivedName !== '' && conflicts) ? conflictCopy(isHeader)
    : null
  // Duplicates announce live (they're visible in the input); emptiness only on a save
  // attempt — flagging it while the designer is still typing would be noise.
  const oneOfError =
    oneOfProblem === 'duplicate' ? ONE_OF_DUPLICATE
    : error === 'one-of-invalid' ? ONE_OF_EMPTY
    : null
  // Dim Save for every problem announced live; emptiness (announced only on a save
  // attempt) keeps the button active so the attempt can explain itself.
  const blocked = pending
    || (derivedName !== '' && (reserved || pagingOwned || conflicts))
    || oneOfProblem === 'duplicate'

  function selectKind(next: CoreType | 'ONE_OF') {
    setKindChoice(next)
    // The refinement belongs to a core type's row; "one of" takes none at all.
    setRefinement(null)
    if (error) setError(null)
  }

  function fail(thrown: unknown) {
    const envelope = thrown as { status?: number; data?: ProblemDetail }
    const violated = (violatedField: string) =>
      envelope.data?.violations?.some((violation) => violation.field === violatedField)
    if (envelope.status === 409 && violated('name')) setError('name-conflict')
    else if (envelope.status === 400 && violated('name')) setError('name-underivable')
    else if (envelope.status === 400 && violated('oneOfValues')) setError('one-of-invalid')
    else setError('server')
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    // UC5/AC6/AC7: blocked inline, nothing sent, nothing persisted.
    if (!trimmedName) return setError('name-required')
    if (!derivedName) return setError('name-underivable')
    if (reserved) return setError('name-reserved')
    if (pagingOwned) return setError('name-paging-owned')
    if (conflicts) return setError('name-conflict')
    if (oneOf && oneOfProblem !== null) return setError('one-of-invalid')

    const data: DeclarationRequest = {
      name: trimmedName,
      coreType: oneOf ? undefined : kindChoice,
      refinement: oneOf ? undefined : (refinement ?? undefined),
      oneOfValues: oneOf ? oneOfValues : undefined,
      required,
      description: description.trim() || undefined,
    }
    const options = { onSuccess: onSaved, onError: fail }
    if (declaration) {
      update.mutate(
        { specId, schemaName, capability, name: declaration.name ?? '', data },
        options,
      )
    } else {
      add.mutate({ specId, schemaName, capability, data }, options)
    }
  }

  function handleKeyDown(event: KeyboardEvent) {
    if (event.key === 'Escape') onClose()
  }

  const idPrefix = `declaration-editor-${location}`
  const input =
    'h-9 rounded-[7px] border-[1.5px] border-control-border bg-control px-3 text-[13px] outline-none placeholder:text-text-faint focus-visible:border-olive focus-visible:ring-3 focus-visible:ring-olive/15 aria-invalid:border-terracotta aria-invalid:ring-3 aria-invalid:ring-terracotta/25'
  return (
    // Docked at the row's position by the 2px ink rule (the FieldEditor scheme, row-sized).
    <li className="my-1.5 border-t-2 border-foreground bg-band px-3.5 py-3" onKeyDown={handleKeyDown}>
      <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-2.5">
        <div className="flex items-center gap-3">
          <input
            id={`${idPrefix}-name`}
            value={name}
            autoFocus
            onChange={(event) => {
              setName(event.target.value)
              if (error) setError(null)
            }}
            placeholder={isHeader ? 'e.g. Request id' : 'e.g. Price max'}
            aria-label="Name"
            aria-invalid={nameError ? true : undefined}
            className={`${input} flex-1 font-semibold placeholder:font-normal`}
          />
          <label className="flex cursor-pointer items-center gap-2 text-[12.5px] font-semibold">
            {requiredLabel}
            <Switch
              checked={required}
              onCheckedChange={() => setRequired((current) => !current)}
              aria-label={requiredLabel}
            />
          </label>
        </div>
        {nameError ? (
          <p role="alert" className="text-[12.5px] text-terracotta">
            {nameError}
          </p>
        ) : (
          // The live derivation line (PRIN-002): the serialized name is never typed.
          derivedName && (
            <p className="font-mono text-[11.5px] text-mono-derived">
              {isHeader ? 'header' : 'parameter'} {derivedName}
            </p>
          )
        )}

        {/* Kind is a sentence: [core or "one of…" ▾] as [refinement ▾] — picking "one of…"
            swaps the refinement slot for the value list (no refinement, no list wrapper). */}
        <div className="flex flex-wrap items-center gap-2.5">
          <KindDropdown<CoreType | 'ONE_OF'>
            value={kindChoice}
            options={[...CORE_TYPES, { value: 'ONE_OF' as const, label: 'one of …' }]}
            onChange={selectKind}
            ariaLabel="Kind"
            className="h-9 font-semibold"
          />
          {oneOf ? (
            <input
              id={`${idPrefix}-values`}
              value={oneOfInput}
              onChange={(event) => {
                setOneOfInput(event.target.value)
                if (error) setError(null)
              }}
              placeholder="available, pending, sold"
              aria-label="One of values"
              aria-invalid={oneOfError ? true : undefined}
              className={`${input} min-w-0 flex-1 font-mono text-[12.5px]`}
            />
          ) : (
            <>
              <span className="text-[13px] text-hint">as</span>
              <KindDropdown
                value={refinement}
                options={[{ value: null, label: 'plain' }, ...refinements]}
                onChange={setRefinement}
                ariaLabel="Refinement"
                disabled={refinements.length === 0}
                className="h-9 font-medium text-text-secondary"
              />
            </>
          )}
        </div>
        {oneOfError && (
          <p role="alert" className="text-[12.5px] text-terracotta">
            {oneOfError}
          </p>
        )}

        <input
          id={`${idPrefix}-description`}
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          placeholder="What this means — optional"
          aria-label="Description"
          className={input}
        />

        {error === 'server' && (
          <p role="alert" className="text-[12.5px] text-terracotta">
            That didn't save — try again.
          </p>
        )}

        <div className="flex items-center gap-3">
          <span className="min-w-0 flex-1 truncate font-mono text-[11.5px] text-mono-derived">
            {serializationLine(oneOf, kindChoice, refinement, oneOfValues, required)}
          </span>
          <span className="text-[11.5px] text-hint">Enter saves · Esc cancels</span>
          <button
            type="button"
            onClick={onClose}
            disabled={pending}
            className="text-[12.5px] font-semibold text-text-tertiary hover:text-foreground"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={blocked}
            className={`rounded-sm bg-primary px-3 py-1 text-[12.5px] font-semibold text-primary-foreground ${blocked ? 'opacity-40' : ''}`}
          >
            Save
          </button>
        </div>
      </form>
    </li>
  )
}

function underivableCopy(isHeader: boolean): string {
  return isHeader ? NAME_UNDERIVABLE_HEADER : NAME_UNDERIVABLE_PARAMETER
}

function conflictCopy(isHeader: boolean): string {
  return isHeader ? NAME_CONFLICT_HEADER : NAME_CONFLICT_PARAMETER
}

// The footer's derived-serialization line (the FieldEditor grammar): "→ string · enum
// [available · pending · sold]", "→ number · required".
function serializationLine(
  oneOf: boolean,
  kindChoice: CoreType | 'ONE_OF',
  refinement: Refinement | null,
  oneOfValues: string[],
  required: boolean,
): string {
  const parts: string[] = []
  if (oneOf) {
    parts.push('string', `enum [${oneOfValues.join(' · ')}]`)
  } else {
    const { type, format } = serializedScalar({ coreType: kindChoice as CoreType, refinement, list: false })
    parts.push(type)
    if (format) parts.push(format)
  }
  if (required) parts.push('required')
  return `→ ${parts.join(' · ')}`
}
