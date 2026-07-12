import { useState } from 'react'
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
} from '@/components/ui/dialog'
import {
  getGetLastEditedLocationQueryKey,
  getListSpecsQueryKey,
  useDeleteSpec,
} from '@/api/endpoints/specs/specs'
import type { SpecSummaryResponse } from '@/api/model'

// FEAT-007 UC3: deletion is permanent — no archive, no undo — so it earns the type-the-name
// ritual (AC5): the primary action stays disarmed until the typed name matches exactly.
// If Archive ever ships, this downgrades to a plain confirm.
export function DeleteApiDialog({
  spec,
  open,
  onOpenChange,
}: {
  spec: SpecSummaryResponse
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const [typed, setTyped] = useState('')
  const [serverError, setServerError] = useState(false)

  const queryClient = useQueryClient()
  const deleteSpec = useDeleteSpec()
  const title = spec.title ?? ''

  function handleOpenChange(next: boolean) {
    onOpenChange(next)
    // Abandoning changes nothing, and the ritual starts over on reopen (AC5).
    if (!next) {
      setTyped('')
      setServerError(false)
    }
  }

  const armed = typed === title

  function handleDelete() {
    if (!armed) return
    deleteSpec.mutate(
      { specId: spec.id ?? '' },
      {
        onSuccess: (response) => {
          if (response.status !== 204) return
          // The card disappears; a jump-back-in pointer at this API was cleared server-side.
          queryClient.invalidateQueries({ queryKey: getListSpecsQueryKey() })
          queryClient.invalidateQueries({ queryKey: getGetLastEditedLocationQueryKey() })
          handleOpenChange(false)
        },
        onError: () => setServerError(true),
      },
    )
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="top-16 max-w-[520px] translate-y-0 gap-5">
        <DialogHeader>
          <DialogTitle>Delete {title}?</DialogTitle>
          {/* The blast radius in plain language, from the summary the card already holds. */}
          <DialogDescription>
            {`${spec.resourceCount} resources, ${spec.operationCount} capabilities and the full spec are deleted for everyone. There is no undo.`}
          </DialogDescription>
        </DialogHeader>

        <div className="flex flex-col gap-2.5">
          <p className="text-[13px]">
            Type{' '}
            <span className="rounded-sm bg-secondary px-1.5 py-0.5 font-mono text-xs font-semibold">
              {title}
            </span>{' '}
            to confirm.
          </p>
          <input
            value={typed}
            onChange={(event) => setTyped(event.target.value)}
            placeholder={title}
            aria-label={`Type ${title} to confirm`}
            className="h-10 rounded-sm border border-transparent bg-input px-3 text-sm outline-none placeholder:text-text-faint focus-visible:border-terracotta focus-visible:ring-3 focus-visible:ring-terracotta/25"
          />
        </div>

        {serverError && (
          <p role="alert" className="text-[12.5px] text-terracotta">
            Something went wrong and the API wasn't deleted. Try again.
          </p>
        )}

        <DialogFooter>
          <DialogClose asChild>
            <Button type="button" variant="outline">
              Cancel
            </Button>
          </DialogClose>
          {/* Solid danger, unlike the soft destructive variant — this is the point of no return. */}
          <Button
            type="button"
            variant="destructive"
            className="bg-destructive text-primary-foreground hover:bg-destructive/90 focus-visible:ring-destructive/40"
            disabled={!armed || deleteSpec.isPending}
            onClick={handleDelete}
          >
            Delete forever
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
