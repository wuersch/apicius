import { useState, type FormEvent, type ReactNode } from 'react'
import { useNavigate } from 'react-router'
import { useQueryClient } from '@tanstack/react-query'
import { Button } from '@/components/ui/button'
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
import { SpecVersionPicker, type SpecVersionOption } from '@/components/home/SpecVersionPicker'
import {
  getGetLastEditedLocationQueryKey,
  getListSpecsQueryKey,
  useCreateSpec,
} from '@/api/endpoints/specs/specs'
import type { ProblemDetail } from '@/api/model'

// The canonical mockup copy is owned here, keyed off the violated field — never echo the
// server's message text (contract wording ≠ UI copy).
const TITLE_REQUIRED = "A title is required — it's the name everyone will see."

type FormError = 'title-required' | 'server' | null

// FEAT-003: the Create API dialog — title, optional description, and a one-time spec version.
// Validation is submit-time and inline; the button stays clickable and explains (PRIN-006).
export function CreateApiDialog({ trigger }: { trigger: ReactNode }) {
  const [open, setOpen] = useState(false)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [specVersion, setSpecVersion] = useState<SpecVersionOption>('3.1')
  const [error, setError] = useState<FormError>(null)

  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const createSpec = useCreateSpec()

  function handleOpenChange(next: boolean) {
    setOpen(next)
    if (!next) {
      setTitle('')
      setDescription('')
      setSpecVersion('3.1')
      setError(null)
    }
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const trimmedTitle = title.trim()
    // UC3: blocked inline, nothing sent, nothing persisted.
    if (!trimmedTitle) {
      setError('title-required')
      return
    }

    createSpec.mutate(
      {
        data: {
          title: trimmedTitle,
          description: description.trim() || undefined,
          specVersion,
        },
      },
      {
        onSuccess: (response) => {
          if (response.status !== 201) return
          // The home must reflect the new API and jump-back-in pointer on its next render.
          queryClient.invalidateQueries({ queryKey: getListSpecsQueryKey() })
          queryClient.invalidateQueries({ queryKey: getGetLastEditedLocationQueryKey() })
          handleOpenChange(false)
          navigate(`/apis/${response.data.id}`)
        },
        onError: (thrown) => {
          // customFetch throws the response envelope; a 400 problem+json names the fields.
          const envelope = thrown as { status?: number; data?: ProblemDetail }
          const titleViolation = envelope.data?.violations?.some(
            (violation) => violation.field === 'title',
          )
          setError(envelope.status === 400 && titleViolation ? 'title-required' : 'server')
        },
      },
    )
  }

  const titleInvalid = error === 'title-required'

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      {/* 640px, anchored 64px from the top (not centered): the form's height varies with the
          error state, and the dialog should read as belonging to the launcher. */}
      <DialogContent className="top-16 max-w-[640px] translate-y-0 gap-5">
        <DialogHeader>
          <DialogTitle>Create a new API</DialogTitle>
          <DialogDescription>
            Name it and pick a spec version — you'll model your first resource right after.
          </DialogDescription>
        </DialogHeader>

        <form className="flex flex-col gap-5" onSubmit={handleSubmit} noValidate>
          <div className="flex flex-col gap-1.5">
            <label htmlFor="create-api-title" className="text-[13px] font-semibold">
              Title
            </label>
            <input
              id="create-api-title"
              value={title}
              onChange={(event) => {
                setTitle(event.target.value)
                if (titleInvalid) setError(null)
              }}
              placeholder="e.g. Storefront API"
              aria-invalid={titleInvalid || undefined}
              aria-describedby={titleInvalid ? 'create-api-title-error' : undefined}
              className="h-10 rounded-sm border border-transparent bg-input px-3 text-sm outline-none placeholder:text-text-faint focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 aria-invalid:border-terracotta aria-invalid:ring-3 aria-invalid:ring-terracotta/25"
            />
            {titleInvalid && (
              <p id="create-api-title-error" role="alert" className="text-[12.5px] text-terracotta">
                {TITLE_REQUIRED}
              </p>
            )}
          </div>

          <div className="flex flex-col gap-1.5">
            <label htmlFor="create-api-description" className="flex items-baseline gap-2 text-[13px] font-semibold">
              Description
              <span className="text-xs font-normal text-text-tertiary">optional</span>
            </label>
            <textarea
              id="create-api-description"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              placeholder="What does this API let people do?"
              rows={2}
              className="resize-none rounded-sm border border-transparent bg-input px-3 py-2 text-sm outline-none placeholder:text-text-faint focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <div className="flex items-baseline justify-between">
              <span id="create-api-version-label" className="text-[13px] font-semibold">
                OpenAPI version
              </span>
              {/* The immutability contract, stated up front, quietly (v1: recreate is the recourse). */}
              <span className="font-mono text-[11px] text-text-tertiary">fixed after creation</span>
            </div>
            <SpecVersionPicker value={specVersion} onChange={setSpecVersion} />
          </div>

          {error === 'server' && (
            <p role="alert" className="text-[12.5px] text-terracotta">
              Something went wrong and the API wasn't created. Try again.
            </p>
          )}

          <DialogFooter className="justify-between">
            <p className="text-[12.5px] text-text-tertiary">
              Starts at <span className="font-mono">v1.0.0</span> — change it any time later.
            </p>
            <div className="flex items-center gap-2">
              <DialogClose asChild>
                <Button type="button" variant="outline">
                  Cancel
                </Button>
              </DialogClose>
              <Button type="submit" disabled={createSpec.isPending}>
                Create API →
              </Button>
            </div>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
