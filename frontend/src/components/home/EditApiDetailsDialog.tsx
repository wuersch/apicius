import { useState, type FormEvent } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Lock } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  getGetLastEditedLocationQueryKey,
  getListSpecsQueryKey,
  useUpdateSpecDetails,
} from '@/api/endpoints/specs/specs'
import type { ProblemDetail, SpecSummaryResponse } from '@/api/model'

// Canonical mockup copy, keyed off the violated field — never echo the server's message text.
const TITLE_REQUIRED = "A title is required — it's the name everyone will see."
const VERSION_REQUIRED = "A version is required — it's your API's own version number."

type FormError = 'title-required' | 'version-required' | 'server' | null

// FEAT-007 UC1: Edit details — the create-dialog shell reused for rewriting info. Controlled
// by the spec to edit (null = closed) so Duplicate can hand the new copy straight in.
export function EditApiDetailsDialog({
  spec,
  onOpenChange,
}: {
  spec: SpecSummaryResponse | null
  onOpenChange: (open: boolean) => void
}) {
  if (!spec) return null
  return (
    <Dialog open onOpenChange={onOpenChange}>
      {/* Same shell as CreateApiDialog: 640px, anchored 64px from the top. */}
      <DialogContent className="top-16 max-w-[640px] translate-y-0 gap-5">
        {/* Keyed so the form re-seeds whenever the target changes (duplicate handoff). */}
        <EditApiDetailsForm key={spec.id} spec={spec} close={() => onOpenChange(false)} />
      </DialogContent>
    </Dialog>
  )
}

function EditApiDetailsForm({ spec, close }: { spec: SpecSummaryResponse; close: () => void }) {
  const [title, setTitle] = useState(spec.title ?? '')
  const [version, setVersion] = useState(spec.apiVersion ?? '')
  const [description, setDescription] = useState(spec.description ?? '')
  const [error, setError] = useState<FormError>(null)

  const queryClient = useQueryClient()
  const updateDetails = useUpdateSpecDetails()

  // "OpenAPI 3.1" — the stored value is the full patch (e.g. 3.1.1); the dialog speaks minors.
  const specVersionMinor = (spec.specVersion ?? '').split('.').slice(0, 2).join('.')

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const trimmedTitle = title.trim()
    const trimmedVersion = version.trim()
    // UC4: blocked inline, nothing sent, nothing persisted.
    if (!trimmedTitle) {
      setError('title-required')
      return
    }
    // info.version is REQUIRED by the OpenAPI spec — an empty one would break the document.
    if (!trimmedVersion) {
      setError('version-required')
      return
    }

    updateDetails.mutate(
      {
        specId: spec.id ?? '',
        data: {
          title: trimmedTitle,
          version: trimmedVersion,
          // Blank means "not provided": the backend removes info.description (AC1).
          description: description.trim() || undefined,
        },
      },
      {
        onSuccess: (response) => {
          if (response.status !== 200) return
          // The card re-renders from the projection; the pointer moved server-side.
          queryClient.invalidateQueries({ queryKey: getListSpecsQueryKey() })
          queryClient.invalidateQueries({ queryKey: getGetLastEditedLocationQueryKey() })
          close()
        },
        onError: (thrown) => {
          const envelope = thrown as { status?: number; data?: ProblemDetail }
          const violated = (field: string) =>
            envelope.data?.violations?.some((violation) => violation.field === field)
          if (envelope.status === 400 && violated('title')) setError('title-required')
          else if (envelope.status === 400 && violated('version')) setError('version-required')
          else setError('server')
        },
      },
    )
  }

  const titleInvalid = error === 'title-required'
  const versionInvalid = error === 'version-required'

  return (
    <>
      <DialogHeader>
        <DialogTitle>Edit details</DialogTitle>
        <DialogDescription>
          The name, version and description everyone sees — nothing inside the API changes.
        </DialogDescription>
      </DialogHeader>

      <form className="flex flex-col gap-5" onSubmit={handleSubmit} noValidate>
        {/* Title + Version side by side — bumping the version is the most common reason
            to open this dialog. */}
        <div className="flex gap-3.5">
          <div className="flex flex-1 flex-col gap-1.5">
            <label htmlFor="edit-api-title" className="text-[13px] font-semibold">
              Title
            </label>
            <input
              id="edit-api-title"
              value={title}
              onChange={(event) => {
                setTitle(event.target.value)
                if (titleInvalid) setError(null)
              }}
              aria-invalid={titleInvalid || undefined}
              aria-describedby={titleInvalid ? 'edit-api-title-error' : undefined}
              className="h-10 rounded-sm border border-transparent bg-input px-3 text-sm outline-none placeholder:text-text-faint focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 aria-invalid:border-terracotta aria-invalid:ring-3 aria-invalid:ring-terracotta/25"
            />
          </div>
          <div className="flex w-[150px] flex-col gap-1.5">
            <label htmlFor="edit-api-version" className="text-[13px] font-semibold">
              Version
            </label>
            <input
              id="edit-api-version"
              value={version}
              onChange={(event) => {
                setVersion(event.target.value)
                if (versionInvalid) setError(null)
              }}
              aria-invalid={versionInvalid || undefined}
              aria-describedby={versionInvalid ? 'edit-api-version-error' : undefined}
              className="h-10 rounded-sm border border-transparent bg-input px-3 font-mono text-[13px] outline-none placeholder:text-text-faint focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 aria-invalid:border-terracotta aria-invalid:ring-3 aria-invalid:ring-terracotta/25"
            />
          </div>
        </div>
        {titleInvalid && (
          <p id="edit-api-title-error" role="alert" className="-mt-3.5 text-[12.5px] text-terracotta">
            {TITLE_REQUIRED}
          </p>
        )}
        {versionInvalid && (
          <p id="edit-api-version-error" role="alert" className="-mt-3.5 text-[12.5px] text-terracotta">
            {VERSION_REQUIRED}
          </p>
        )}

        <div className="flex flex-col gap-1.5">
          <label htmlFor="edit-api-description" className="flex items-baseline gap-2 text-[13px] font-semibold">
            Description
            <span className="text-xs font-normal text-text-tertiary">optional</span>
          </label>
          <textarea
            id="edit-api-description"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            rows={2}
            className="resize-none rounded-sm border border-transparent bg-input px-3 py-2 text-sm outline-none placeholder:text-text-faint focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
          />
        </div>

        {/* The locked spec-version row (AC2) — the shape table's id-row treatment: shown,
            explained, no input. Conversion is a real future feature; this row becomes
            actionable when it exists. */}
        <div className="flex items-center gap-3 rounded-sm bg-id-row px-3.5 py-2.5">
          <span className="font-mono text-[12.5px] font-medium text-mono-derived">
            OpenAPI {specVersionMinor}
          </span>
          <span className="flex-1 text-[13px] text-hint">spec version — fixed after creation</span>
          <Lock aria-hidden className="size-[13px] shrink-0 text-ring" />
        </div>

        {error === 'server' && (
          <p role="alert" className="text-[12.5px] text-terracotta">
            Something went wrong and the changes weren't saved. Try again.
          </p>
        )}

        <DialogFooter className="justify-between">
          <p className="text-[12.5px] text-text-tertiary">
            Rewrites <span className="font-mono">info</span> only.
          </p>
          <div className="flex items-center gap-2">
            <DialogClose asChild>
              <Button type="button" variant="outline">
                Cancel
              </Button>
            </DialogClose>
            <Button type="submit" disabled={updateDetails.isPending}>
              Save changes
            </Button>
          </div>
        </DialogFooter>
      </form>
    </>
  )
}
