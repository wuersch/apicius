import { useState, type FormEvent, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { KeyRound } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import {
  getGetLastEditedLocationQueryKey,
  getGetSpecQueryKey,
  getListSpecsQueryKey,
  useAddResource,
} from '@/api/endpoints/specs/specs'
import type { Capability, ProblemDetail } from '@/api/model'
import {
  ALL_CAPABILITIES,
  articleFor,
  countDerived,
  deriveCapability,
  deriveNaming,
  displayNounOf,
  NAME_PATTERN,
} from '@/lib/derivation'
import { METHOD_DOT_CLASS } from '@/components/editor/method-dot'

// The canonical dialog copy, keyed off the violated rule — never echo the server's message
// text (contract wording ≠ UI copy).
const NAME_REQUIRED = "A name is required — it's the noun your API is about."
const NAME_INVALID = 'Use letters, digits and single spaces, starting with a letter.'
const NAME_CONFLICT = 'That name is already used in this API — pick a different noun.'
const NO_CAPABILITY = 'A resource is something people can act on — keep at least one capability.'

type FormError = 'name-required' | 'name-invalid' | 'name-conflict' | 'no-capability' | 'server' | null

// FEAT-005: the New resource dialog — noun first, then what people can do; every derived
// detail (schema, paths, verbs) is shown as you decide, never typed (PRIN-002). All five
// capabilities are pre-selected; deselecting is the deliberate override (PRIN-006, UC2).
export function NewResourceDialog({
  specId,
  existingNames,
  trigger,
}: {
  specId: string
  existingNames: string[]
  trigger: ReactNode
}) {
  const [open, setOpen] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [selected, setSelected] = useState<Capability[]>(ALL_CAPABILITIES)
  const [error, setError] = useState<FormError>(null)

  const queryClient = useQueryClient()
  const addResource = useAddResource()

  function handleOpenChange(next: boolean) {
    setOpen(next)
    if (!next) {
      setName('')
      setDescription('')
      setSelected(ALL_CAPABILITIES)
      setError(null)
    }
  }

  function toggle(capability: Capability) {
    setSelected((current) =>
      current.includes(capability)
        ? current.filter((item) => item !== capability)
        : ALL_CAPABILITIES.filter((item) => current.includes(item) || item === capability),
    )
    if (error === 'no-capability') setError(null)
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const trimmedName = name.trim()
    // UC3/UC4: blocked inline, nothing sent, nothing persisted.
    if (!trimmedName) {
      setError('name-required')
      return
    }
    if (!NAME_PATTERN.test(trimmedName)) {
      setError('name-invalid')
      return
    }
    const naming = deriveNaming(trimmedName)
    // Pre-check against the loaded projection; the server's 409 stays authoritative (AC6).
    if (existingNames.some((existing) => existing.toLowerCase() === naming?.schemaName.toLowerCase())) {
      setError('name-conflict')
      return
    }
    if (selected.length === 0) {
      setError('no-capability')
      return
    }

    addResource.mutate(
      {
        specId,
        data: {
          name: trimmedName,
          description: description.trim() || undefined,
          capabilities: selected,
        },
      },
      {
        onSuccess: (response) => {
          if (response.status !== 201) return
          // The editor re-renders from the invalidated projection — what it shows is
          // projected from the document, never echoed from this form (FEAT-005).
          queryClient.invalidateQueries({ queryKey: getGetSpecQueryKey(specId) })
          queryClient.invalidateQueries({ queryKey: getListSpecsQueryKey() })
          queryClient.invalidateQueries({ queryKey: getGetLastEditedLocationQueryKey() })
          handleOpenChange(false)
        },
        onError: (thrown) => {
          const envelope = thrown as { status?: number; data?: ProblemDetail }
          if (envelope.status === 409) {
            setError('name-conflict')
            return
          }
          const violated = (field: string) =>
            envelope.data?.violations?.some((violation) => violation.field === field)
          if (envelope.status === 400 && violated('capabilities')) setError('no-capability')
          else if (envelope.status === 400 && violated('name')) setError('name-invalid')
          else setError('server')
        },
      },
    )
  }

  const trimmedName = name.trim()
  const naming = deriveNaming(trimmedName)
  const noun = displayNounOf(trimmedName) ?? 'resource'
  const { operations, paths } = countDerived(selected)
  const nameError =
    error === 'name-required' ? NAME_REQUIRED
    : error === 'name-invalid' ? NAME_INVALID
    : error === 'name-conflict' ? NAME_CONFLICT
    : null

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      {/* 660px, anchored 56px from the top (mockup 2b): height varies with the error state. */}
      <DialogContent className="top-14 max-w-[660px] translate-y-0 gap-5">
        <DialogHeader>
          <DialogTitle>New resource</DialogTitle>
          <DialogDescription>
            Name the thing your API is about and choose what people can do with it — the schema
            and paths are derived for you.
          </DialogDescription>
        </DialogHeader>

        <form className="flex flex-col gap-5" onSubmit={handleSubmit} noValidate>
          <div className="flex flex-col gap-1.5">
            <label htmlFor="new-resource-name" className="text-[13px] font-semibold">
              Name
            </label>
            <input
              id="new-resource-name"
              value={name}
              onChange={(event) => {
                setName(event.target.value)
                if (nameError) setError(null)
              }}
              placeholder="e.g. Product"
              aria-invalid={nameError ? true : undefined}
              aria-describedby={nameError ? 'new-resource-name-error' : 'new-resource-derivation'}
              className="h-10 rounded-sm border border-transparent bg-input px-3 text-sm font-semibold outline-none placeholder:font-normal placeholder:text-text-faint focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 aria-invalid:border-terracotta aria-invalid:ring-3 aria-invalid:ring-terracotta/25"
            />
            {nameError ? (
              <p id="new-resource-name-error" role="alert" className="text-[12.5px] text-terracotta">
                {nameError}
              </p>
            ) : (
              // The live derivation line (PRIN-002): shown the moment the noun derives cleanly.
              naming && (
                <p id="new-resource-derivation" className="font-mono text-[11.5px] text-text-tertiary">
                  schema {naming.schemaName} · collection {naming.collectionPath}
                </p>
              )
            )}
          </div>

          <div className="flex flex-col gap-1.5">
            <label htmlFor="new-resource-description" className="flex items-baseline gap-2 text-[13px] font-semibold">
              Description
              <span className="text-xs font-normal text-text-tertiary">optional</span>
            </label>
            <input
              id="new-resource-description"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              placeholder={`What is ${articleFor(noun)} ${noun}?`}
              className="h-10 rounded-sm border border-transparent bg-input px-3 text-sm outline-none placeholder:text-text-faint focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <div className="flex items-baseline justify-between">
              <span id="new-resource-capabilities-label" className="text-[13px] font-semibold">
                What people can do
              </span>
              {/* UC4's rule, stated before it's violated. */}
              <span className="font-mono text-[11px] text-text-faint">at least one</span>
            </div>
            <div
              role="group"
              aria-labelledby="new-resource-capabilities-label"
              className="overflow-hidden rounded-lg border border-border"
            >
              {ALL_CAPABILITIES.map((capability) => {
                const checked = selected.includes(capability)
                const derived = deriveCapability(naming ? trimmedName : 'Product', capability)
                // Without a derivable noun yet, rows show only the generic verb column.
                const derivedColumn = naming ? `${derived.method} ${derived.path}` : derived.method
                const label = naming
                  ? derived.label
                  : { BROWSE: 'Browse all', LOOK_UP: 'Look up one', ADD: 'Add one', UPDATE: 'Update one', REMOVE: 'Remove one' }[capability]
                return (
                  <label
                    key={capability}
                    className="flex cursor-pointer items-center gap-3 border-b border-border px-3.5 py-2.5 transition-colors hover:bg-muted"
                  >
                    <Checkbox
                      checked={checked}
                      onCheckedChange={() => toggle(capability)}
                      aria-label={label}
                    />
                    <span
                      aria-hidden
                      className={`size-2 shrink-0 rounded-full ${checked ? METHOD_DOT_CLASS[derived.method] : 'bg-ring'}`}
                    />
                    <span className={`flex-1 text-sm font-medium ${checked ? '' : 'text-text-faint'}`}>
                      {label}
                    </span>
                    <span className={`font-mono text-[11.5px] ${checked ? 'text-text-tertiary' : 'text-text-faint'}`}>
                      {checked ? derivedColumn : "won't be created"}
                    </span>
                  </label>
                )
              })}
              {/* The identity house rule — not a choice, so not a checkbox (ADR-0010). */}
              <div className="flex items-start gap-2.5 bg-muted px-3.5 py-2.5">
                <KeyRound aria-hidden className="mt-0.5 size-3.5 shrink-0 text-text-tertiary" />
                <p className="text-[12.5px] leading-normal text-text-tertiary">
                  Every {noun} gets an identifier — <span className="font-mono text-[11.5px]">id</span>,
                  read-only. Add fields and links after creating.
                </p>
              </div>
            </div>
            {error === 'no-capability' && (
              <p role="alert" className="text-[12.5px] text-terracotta">
                {NO_CAPABILITY}
              </p>
            )}
          </div>

          {error === 'server' && (
            <p role="alert" className="text-[12.5px] text-terracotta">
              Something went wrong and the resource wasn't created. Try again.
            </p>
          )}

          <DialogFooter className="justify-between">
            {/* The atomic-derivation promise, made legible: exactly what confirming writes. */}
            <p className="text-[12.5px] text-text-tertiary">
              Creates <span className="font-mono text-[11.5px]">{operations} operation{operations === 1 ? '' : 's'}</span> on{' '}
              <span className="font-mono text-[11.5px]">{paths} path{paths === 1 ? '' : 's'}</span>.
            </p>
            <div className="flex items-center gap-2">
              <DialogClose asChild>
                <Button type="button" variant="outline">
                  Cancel
                </Button>
              </DialogClose>
              <Button type="submit" disabled={addResource.isPending}>
                Create resource →
              </Button>
            </div>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
