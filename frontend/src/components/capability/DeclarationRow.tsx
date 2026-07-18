import { Pencil, X } from 'lucide-react'
import type { DeclarationResponse } from '@/api/model'
import { describeKind } from '@/lib/fieldDerivation'

// FEAT-011: one declaration as its card displays it — the Filters row grammar shared by all
// three locations (v10's "everything is the Filters row"): derived name in a fixed mono
// column (it is the declaration's identity), the plain-language kind and description as
// body copy. Optionality marks only the exception: filters are optional by nature, so
// absence is the default and a trailing "required" is the one word that speaks (a settled
// divergence from the v10 mockup, which labels every row). Hover swaps the trailing slot for
// Edit/Remove (state 3·6) — remove deletes outright: everything here is capability-local.
export function DeclarationRow({
  declaration,
  showOptionality,
  editing,
  onEdit,
  onRemove,
}: {
  declaration: DeclarationResponse
  /** Inputs only — a response header has no optionality to show. */
  showOptionality: boolean
  editing?: boolean
  onEdit?: () => void
  onRemove?: () => void
}) {
  if (editing) {
    return (
      <li className="flex items-center gap-3 bg-accent px-1 py-[9px]">
        <span className="w-[140px] shrink-0 font-mono text-[12.5px] font-semibold">
          {declaration.name}
        </span>
        <KindText declaration={declaration} />
        <span className="font-mono text-[10.5px] tracking-[.08em] text-text-faint uppercase">
          editing
        </span>
      </li>
    )
  }

  return (
    <li className="group/row flex items-center gap-3 border-b border-border py-[9px] last:border-b-0">
      <span className="w-[140px] shrink-0 font-mono text-[12.5px] font-medium">
        {declaration.name}
      </span>
      <KindText declaration={declaration} />
      {showOptionality && declaration.required && (
        <span className="shrink-0 text-[11px] font-medium text-text-tertiary group-hover/row:hidden">
          required
        </span>
      )}
      <span
        className={`hidden shrink-0 items-center gap-3 group-hover/row:flex ${onEdit ? '' : 'invisible'}`}
      >
        <button
          type="button"
          onClick={onEdit}
          disabled={!onEdit}
          aria-label={`Edit ${declaration.name}`}
          className="flex items-center gap-1.5 text-[12px] font-semibold text-text-tertiary hover:text-foreground"
        >
          <Pencil aria-hidden className="size-[11px]" />
          Edit
        </button>
        <button
          type="button"
          onClick={onRemove}
          disabled={!onRemove}
          aria-label={`Remove ${declaration.name}`}
          className="flex items-center gap-1.5 text-[12px] font-semibold text-terracotta hover:underline"
        >
          <X aria-hidden className="size-[11px]" strokeWidth={2.4} />
          Remove
        </button>
      </span>
    </li>
  )
}

// The kind as body copy: FEAT-006's vocabulary through the shared describeKind, or the
// "one of" value chips (View 3's Filters card), the description trailing muted.
function KindText({ declaration }: { declaration: DeclarationResponse }) {
  return (
    <span className="min-w-0 flex-1 text-[13.5px] text-text-tertiary">
      {declaration.oneOfValues ? (
        <>
          one of{' '}
          {declaration.oneOfValues.map((value) => (
            <span
              key={value}
              className="mr-1 rounded-[5px] bg-input px-2 py-px text-[12.5px]"
            >
              {value}
            </span>
          ))}
        </>
      ) : (
        describeKind({
          coreType: declaration.coreType!,
          refinement: declaration.refinement ?? null,
          list: false,
        })
      )}
      {declaration.description && (
        <span className="text-hint"> — {declaration.description}</span>
      )}
    </span>
  )
}
