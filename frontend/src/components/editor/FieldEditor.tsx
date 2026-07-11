import { useState, type FormEvent } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { ChevronRight, Info, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Switch } from '@/components/ui/switch'
import { KindDropdown } from '@/components/editor/KindDropdown'
import {
  getGetLastEditedLocationQueryKey,
  getGetSpecQueryKey,
  getListSpecsQueryKey,
  useAddField,
  useRemoveField,
  useUpdateField,
} from '@/api/endpoints/specs/specs'
import type {
  CoreType,
  FieldResponse,
  FieldVisibility,
  ProblemDetail,
  Refinement,
} from '@/api/model'
import {
  CORE_TYPES,
  defaultVisibility,
  derivePropertyName,
  REFINEMENTS_BY_CORE_TYPE,
  serializationPreview,
} from '@/lib/fieldDerivation'

// The canonical editor copy, keyed off the violated rule — never echo the server's message
// text (contract wording ≠ UI copy).
const NAME_REQUIRED = "A name is required — it's what this field is called."
const NAME_UNDERIVABLE = 'Nothing derivable here — a field name needs letters or digits.'
const NAME_CONFLICT =
  'This shape already has a field with this property name — id counts too.'
const HOUSE_RULE =
  'Passwords can be set but are never returned, so write-only was switched on for you.'

type FormError = 'name-required' | 'name-underivable' | 'name-conflict' | 'server' | null

const VISIBILITY_LABELS: Record<FieldVisibility, string> = {
  NORMAL: 'normal',
  AUTO: 'auto',
  WRITE_ONLY: 'write-only',
}

// FEAT-006: the inline field editor — one anatomy for add and edit (mockup 2c, 2d·1–2d·6):
// name first (property name derived live, PRIN-002), kind as a sentence (the three ADR-0011
// slots), Advanced tucked away, the derived serialization in the footer. Edit mode adds
// Remove field — remove lives where edit lives (UC4).
export function FieldEditor({
  specId,
  resourceName,
  existingNames,
  field,
  onClose,
}: {
  specId: string
  resourceName: string
  /** Every property name of this shape, id included (AC9's collision set). */
  existingNames: string[]
  /** The field being edited; absent in add mode. */
  field?: FieldResponse
  onClose: () => void
}) {
  const [name, setName] = useState(field?.name ?? '')
  const [description, setDescription] = useState(field?.description ?? '')
  const [coreType, setCoreType] = useState<CoreType>(field?.coreType ?? 'TEXT')
  const [refinement, setRefinement] = useState<Refinement | null>(field?.refinement ?? null)
  const [list, setList] = useState(field?.list ?? false)
  const [required, setRequired] = useState(field?.required ?? false)
  // null = the house rule decides: the request omits visibility, so the server's own rule
  // stays authoritative (AC5). Editing starts from the persisted value — a past decision —
  // but the rule still fires on a kind change this session (UC2 covers add and change
  // alike) unless the session pinned visibility with an explicit pick.
  const initialVisibility: FieldVisibility | null = field ? (field.visibility ?? 'NORMAL') : null
  const [visibilityChoice, setVisibilityChoice] = useState<FieldVisibility | null>(initialVisibility)
  const [visibilityPinned, setVisibilityPinned] = useState(false)
  const [advancedOpen, setAdvancedOpen] = useState(false)
  const [error, setError] = useState<FormError>(null)

  const queryClient = useQueryClient()
  const addField = useAddField()
  const updateField = useUpdateField()
  const removeField = useRemoveField()
  const pending = addField.isPending || updateField.isPending || removeField.isPending

  const visibility = visibilityChoice ?? defaultVisibility(refinement)
  const houseRuleApplied = refinement === 'PASSWORD' && visibilityChoice === null
  const refinements = REFINEMENTS_BY_CORE_TYPE[coreType]

  const trimmedName = name.trim()
  const propertyName = derivePropertyName(trimmedName)
  // Pre-check against the loaded projection; the server's 409 stays authoritative (AC9).
  const conflicts = existingNames.some(
    (existing) =>
      existing !== field?.name && existing.toLowerCase() === propertyName.toLowerCase(),
  )
  const nameError =
    error === 'name-required' ? NAME_REQUIRED
    : error === 'name-underivable' ? NAME_UNDERIVABLE
    : error === 'name-conflict' || (propertyName !== '' && conflicts) ? NAME_CONFLICT
    : null
  const blocked = pending || (propertyName !== '' && conflicts)

  function selectCoreType(next: CoreType) {
    setCoreType(next)
    // The refinement belongs to its core type's row: switching cores resets it — and
    // retracts an unpinned house rule with it.
    if (refinement === 'PASSWORD' && !visibilityPinned) {
      setVisibilityChoice(initialVisibility)
    }
    setRefinement(null)
  }

  function selectRefinement(next: Refinement | null) {
    const previous = refinement
    setRefinement(next)
    if (visibilityPinned) return
    if (next === 'PASSWORD') {
      // The house rule announces itself (PRIN-006): Advanced opens so the applied default
      // is visible, explained, and overridable — never silent (mockup 2d·3).
      setVisibilityChoice(null)
      setAdvancedOpen(true)
    } else if (previous === 'PASSWORD') {
      // Leaving password retracts the rule: back to where the session started.
      setVisibilityChoice(initialVisibility)
    }
  }

  function invalidateAndClose() {
    // The shape re-renders from the invalidated projection — what it shows is projected
    // from the document, never echoed from this form (FEAT-006 Interaction Model).
    queryClient.invalidateQueries({ queryKey: getGetSpecQueryKey(specId) })
    queryClient.invalidateQueries({ queryKey: getListSpecsQueryKey() })
    queryClient.invalidateQueries({ queryKey: getGetLastEditedLocationQueryKey() })
    onClose()
  }

  function fail(thrown: unknown) {
    const envelope = thrown as { status?: number; data?: ProblemDetail }
    const violated = (violatedField: string) =>
      envelope.data?.violations?.some((violation) => violation.field === violatedField)
    if (envelope.status === 409 && violated('name')) setError('name-conflict')
    else if (envelope.status === 400 && violated('name')) setError('name-underivable')
    else setError('server')
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    // UC5: blocked inline, nothing sent, nothing persisted.
    if (!trimmedName) {
      setError('name-required')
      return
    }
    if (!propertyName) {
      setError('name-underivable')
      return
    }
    if (conflicts) {
      setError('name-conflict')
      return
    }
    const data = {
      name: trimmedName,
      coreType,
      refinement: refinement ?? undefined,
      list,
      required,
      visibility: visibilityChoice ?? undefined,
      description: description.trim() || undefined,
    }
    const options = { onSuccess: invalidateAndClose, onError: fail }
    if (field) {
      updateField.mutate({ specId, schemaName: resourceName, propertyName: field.name!, data }, options)
    } else {
      addField.mutate({ specId, schemaName: resourceName, data }, options)
    }
  }

  function handleRemove() {
    removeField.mutate(
      { specId, schemaName: resourceName, propertyName: field!.name! },
      { onSuccess: invalidateAndClose, onError: () => setError('server') },
    )
  }

  const idPrefix = `field-editor-${resourceName}`
  const label = 'text-xs font-semibold tracking-[.02em] text-text-secondary'
  const input =
    'h-10 rounded-[7px] border-[1.5px] border-control-border bg-control px-[13px] text-sm outline-none placeholder:text-text-faint focus-visible:border-olive focus-visible:ring-3 focus-visible:ring-olive/15 aria-invalid:border-terracotta aria-invalid:ring-3 aria-invalid:ring-terracotta/25'
  return (
    // Docked to the shape table by the 2px ink rule; the darker bands frame the lighter
    // editing body — the dialogs' darker–light–darker scheme (mockup 2c).
    <li className="border-t-2 border-foreground bg-surface">
      <form onSubmit={handleSubmit} noValidate>
        <div className="border-b border-band-border bg-band px-[18px] pt-3.5 pb-[13px]">
          <h4 className="flex items-baseline gap-2 text-[15px] font-bold tracking-[-.01em]">
            {field ? (
              <>
                Edit field
                <span className="font-mono text-[11.5px] font-normal tracking-normal text-text-faint">
                  {field.name}
                </span>
              </>
            ) : (
              'New field'
            )}
          </h4>
          <p className="mt-0.5 text-[12.5px] text-hint">
            {field
              ? 'Rewrites the property in place — required membership follows the field.'
              : 'Appends to this shape — the schema property is derived from the name.'}
          </p>
        </div>

        <div className="flex flex-col gap-[19px] px-[18px] pt-[17px] pb-4">
          <div className="flex flex-col gap-1.5">
            <div className="flex items-center">
              <label htmlFor={`${idPrefix}-name`} className={label}>
                Name
              </label>
              <label className="ml-auto flex cursor-pointer items-center gap-2 text-[12.5px] font-semibold">
                Required
                <Switch
                  checked={required}
                  onCheckedChange={() => setRequired((current) => !current)}
                  aria-label="Required"
                />
              </label>
            </div>
            <input
              id={`${idPrefix}-name`}
              value={name}
              onChange={(event) => {
                setName(event.target.value)
                if (error) setError(null)
              }}
              placeholder="e.g. First name"
              aria-invalid={nameError ? true : undefined}
              aria-describedby={nameError ? `${idPrefix}-name-error` : `${idPrefix}-derivation`}
              className={`${input} font-semibold placeholder:font-normal`}
            />
            {nameError ? (
              <p id={`${idPrefix}-name-error`} role="alert" className="text-[12.5px] text-terracotta">
                {nameError}
              </p>
            ) : (
              // The live derivation line (PRIN-002): the property name is never typed.
              propertyName && (
                <p id={`${idPrefix}-derivation`} className="font-mono text-[11.5px] text-mono-derived">
                  property {propertyName}
                </p>
              )
            )}
          </div>

          <div className="flex flex-col gap-1.5">
            <label htmlFor={`${idPrefix}-description`} className={`flex items-baseline gap-2 ${label}`}>
              Description
              <span className="text-[11.5px] font-normal tracking-normal text-hint">optional</span>
            </label>
            <input
              id={`${idPrefix}-description`}
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              placeholder="What this field means"
              className={input}
            />
          </div>

          {/* Kind is a sentence: [core ▾] as [refinement ▾] · ☐ a list of these — literally
              ADR-0011's three slots (mockup 2c). */}
          <div className="flex flex-col gap-[7px]">
            <span className={label}>Kind</span>
            <div className="flex flex-wrap items-center gap-[11px]">
              <KindDropdown
                value={coreType}
                options={CORE_TYPES}
                onChange={selectCoreType}
                ariaLabel="Core type"
                className="font-semibold"
              />
              <span className="text-[13px] text-hint">as</span>
              <KindDropdown
                value={refinement}
                options={[{ value: null, label: 'plain' }, ...refinements]}
                onChange={selectRefinement}
                ariaLabel="Refinement"
                disabled={refinements.length === 0}
                className="font-medium text-text-secondary"
              />
              <span aria-hidden className="mx-1 h-[18px] w-px bg-divider" />
              <label className="flex cursor-pointer items-center gap-2 text-[13px] font-medium text-text-secondary">
                <Checkbox
                  checked={list}
                  onCheckedChange={() => setList((current) => !current)}
                  aria-label="A list of these"
                  className="rounded-[5px] border-check-border bg-control"
                />
                a list of these
              </label>
            </div>
          </div>

          <div className="flex flex-col gap-2.5">
            <button
              type="button"
              onClick={() => setAdvancedOpen((current) => !current)}
              aria-expanded={advancedOpen}
              className="flex items-center gap-[7px] self-start text-[12.5px] font-semibold text-text-faint transition-colors hover:text-foreground"
            >
              <ChevronRight
                aria-hidden
                strokeWidth={2.4}
                className={`size-3 transition-transform ${advancedOpen ? 'rotate-90' : ''}`}
              />
              Advanced
              {!advancedOpen && (
                <span className="font-normal text-hint">· {VISIBILITY_LABELS[visibility]}</span>
              )}
            </button>

            {advancedOpen && (
              // Visibility is one choice of three (ADR-0011): auto and write-only at once —
              // a field nobody could ever see — is unrepresentable, not validated away.
              <RadioGroup
                value={visibility}
                onValueChange={(next) => {
                  setVisibilityChoice(next as FieldVisibility)
                  setVisibilityPinned(true)
                }}
                aria-label="Visibility"
                className="flex flex-wrap items-center gap-x-5 gap-y-2.5 pl-[19px]"
              >
                {(
                  [
                    { value: 'NORMAL', label: 'Normal', hint: null },
                    { value: 'AUTO', label: 'Auto', hint: '— the server sets it' },
                    { value: 'WRITE_ONLY', label: 'Write-only', hint: '— never returned' },
                  ] as const
                ).map((option) => (
                  <RadioGroupItem
                    key={option.value}
                    value={option.value}
                    className={`flex items-center gap-2 text-left text-[13px] font-medium ${visibility === option.value ? 'text-foreground' : 'text-text-secondary'}`}
                  >
                    {visibility === option.value ? (
                      <span
                        aria-hidden
                        className="flex size-[18px] shrink-0 items-center justify-center rounded-full bg-primary"
                      >
                        <span className="size-1.5 rounded-full bg-[#FBF4E9]" />
                      </span>
                    ) : (
                      <span
                        aria-hidden
                        className="size-[18px] shrink-0 rounded-full border-[1.5px] border-check-border bg-control"
                      />
                    )}
                    {option.label}
                    {option.hint && (
                      <span className="text-xs font-normal text-hint">{option.hint}</span>
                    )}
                    {option.value === 'WRITE_ONLY' && houseRuleApplied && (
                      <span className="rounded-sm bg-input px-[7px] py-[2.5px] font-mono text-[10px] tracking-[.08em] uppercase">
                        house rule
                      </span>
                    )}
                  </RadioGroupItem>
                ))}
              </RadioGroup>
            )}

            {houseRuleApplied && (
              // Applied visibly, explained, overridable — never silent (PRIN-006, AC5).
              <div className="flex items-start gap-2.5 rounded-[7px] bg-accent px-[13px] py-[11px]">
                <Info aria-hidden className="mt-0.5 size-3.5 shrink-0 text-text-faint" />
                <p className="flex-1 text-[12.5px] leading-normal text-text-secondary">{HOUSE_RULE}</p>
                <button
                  type="button"
                  onClick={() => {
                    setVisibilityChoice('NORMAL')
                    setVisibilityPinned(true)
                  }}
                  className="text-xs font-semibold underline-offset-2 hover:underline"
                >
                  Override
                </button>
              </div>
            )}
          </div>

          {error === 'server' && (
            <p role="alert" className="text-[12.5px] text-terracotta">
              Something went wrong and the field wasn't saved. Try again.
            </p>
          )}
        </div>

        {/* Footer band: Remove where edit lives, the derived serialization de-emphasized
            left, then the actions (mockup 2c/2d·6). */}
        <div className="flex items-center gap-3 border-t border-band-border bg-band px-[18px] py-3">
          {field && (
            <>
              <button
                type="button"
                onClick={handleRemove}
                disabled={pending}
                className="flex shrink-0 items-center gap-1.5 text-[12.5px] font-semibold text-terracotta underline-offset-2 hover:underline disabled:opacity-50"
              >
                <Trash2 aria-hidden className="size-[13px]" />
                Remove field
              </button>
              <span aria-hidden className="h-4 w-px shrink-0 bg-divider" />
            </>
          )}
          <span className="min-w-0 font-mono text-[11.5px] text-mono-derived">
            {serializationPreview({ coreType, refinement, list }, required, visibility)}
          </span>
          <span className="flex-1" />
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            disabled={pending}
            className="h-9 rounded-sm border-[1.5px] border-ring px-3.5 text-[13px] font-semibold"
          >
            Cancel
          </Button>
          <Button
            type="submit"
            disabled={blocked}
            className={`h-9 rounded-sm px-4 text-[13px] font-semibold ${blocked ? 'opacity-40' : ''}`}
          >
            {field ? 'Save changes' : 'Add field'}
          </Button>
        </div>
      </form>
    </li>
  )
}
