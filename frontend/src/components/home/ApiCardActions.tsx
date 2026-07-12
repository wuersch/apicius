import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Copy, EllipsisVertical, Pencil, Trash2 } from 'lucide-react'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { DeleteApiDialog } from '@/components/home/DeleteApiDialog'
import { EditApiDetailsDialog } from '@/components/home/EditApiDetailsDialog'
import { getListSpecsQueryKey, useDuplicateSpec } from '@/api/endpoints/specs/specs'
import type { SpecSummaryResponse } from '@/api/model'

// FEAT-007: the card's ⋯ menu — the API as an object (details, copy, delete), never its
// contents. Download items join when FEAT-008 lands. The dialogs are controlled siblings of
// the menu, never children of its content, so closing the menu doesn't unmount them.
export function ApiCardActions({ spec }: { spec: SpecSummaryResponse }) {
  // Non-null = the Edit dialog is open, pre-filled from exactly this summary — the card's own
  // spec, or the fresh copy a duplicate hands over (its card may not be rendered yet).
  const [editingSpec, setEditingSpec] = useState<SpecSummaryResponse | null>(null)
  const [deleteOpen, setDeleteOpen] = useState(false)

  const queryClient = useQueryClient()
  const duplicateSpec = useDuplicateSpec()
  const title = spec.title ?? ''

  function duplicate() {
    duplicateSpec.mutate(
      { specId: spec.id ?? '' },
      {
        onSuccess: (response) => {
          if (response.status !== 201) return
          queryClient.invalidateQueries({ queryKey: getListSpecsQueryKey() })
          // The settled handoff: open Edit details on the copy so "<title> (copy)" can be
          // renamed immediately. The pointer never moves, so last-edited stays untouched.
          setEditingSpec(response.data)
        },
      },
    )
  }

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button
            type="button"
            aria-label={`Actions for ${title}`}
            className="flex size-7 items-center justify-center rounded-md outline-none hover:bg-muted focus-visible:ring-2 focus-visible:ring-ring aria-expanded:bg-muted"
          >
            <EllipsisVertical aria-hidden className="size-4" />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-[230px]">
          <DropdownMenuItem onSelect={() => setEditingSpec(spec)}>
            <Pencil aria-hidden className="text-text-secondary" />
            Edit details…
          </DropdownMenuItem>
          <DropdownMenuItem onSelect={duplicate}>
            <Copy aria-hidden className="text-text-secondary" />
            Duplicate
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem variant="destructive" onSelect={() => setDeleteOpen(true)}>
            <Trash2 aria-hidden />
            Delete…
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      <EditApiDetailsDialog
        spec={editingSpec}
        onOpenChange={(open) => {
          if (!open) setEditingSpec(null)
        }}
      />
      <DeleteApiDialog spec={spec} open={deleteOpen} onOpenChange={setDeleteOpen} />
    </>
  )
}
