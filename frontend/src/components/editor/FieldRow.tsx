import { Lock } from 'lucide-react'
import type { FieldResponse } from '@/api/model'
import { describeKind } from '@/lib/fieldDerivation'

// FEAT-006 AC11: one field of the shape — property name leads in a fixed mono column (it is
// the field's identity and display, ADR-0011), the plain-language kind and attribute chips
// follow, derived detail de-emphasized. The id variant is locked by construction: it renders
// no edit affordance at all (AC7). While its editor is open a row stays visible, highlighted
// with an "editing" tag (mockup 2d·6), inert like everything else.
export function FieldRow({
  field,
  locked,
  editing,
  onEdit,
}: {
  field: FieldResponse
  locked?: boolean
  editing?: boolean
  onEdit?: () => void
}) {
  const kind = {
    coreType: field.coreType!,
    refinement: field.refinement ?? null,
    list: field.list ?? false,
  }

  if (locked) {
    return (
      <li className="flex items-center gap-3 border-b border-border bg-id-row px-3.5 py-2">
        <span className="w-[110px] shrink-0 font-mono text-[12.5px] font-medium text-mono-derived">
          {field.name}
        </span>
        <span className="flex-1 text-[13px] text-hint">identifier — the server sets it</span>
        <Lock aria-hidden className="size-[13px] shrink-0 text-ring" />
      </li>
    )
  }

  if (editing) {
    return (
      <li className="flex items-center gap-3 bg-accent px-3.5 py-2">
        <span className="w-[110px] shrink-0 font-mono text-[12.5px] font-semibold">
          {field.name}
        </span>
        <span className="flex-1 text-[13px] text-text-tertiary">{describeKind(kind)}</span>
        <span className="font-mono text-[10.5px] tracking-[.08em] text-text-faint uppercase">
          editing
        </span>
      </li>
    )
  }

  return (
    <li className="border-b border-border">
      <button
        type="button"
        onClick={onEdit}
        disabled={!onEdit}
        aria-label={`Edit field ${field.name}`}
        className="flex w-full items-center gap-3 px-3.5 py-2 text-left transition-colors enabled:hover:bg-surface disabled:cursor-default"
      >
        <span className="w-[110px] shrink-0 font-mono text-[12.5px] font-medium">{field.name}</span>
        <span className="flex-1 text-[13px] text-text-tertiary">{describeKind(kind)}</span>
        {field.required && <Chip>required</Chip>}
        {field.visibility === 'AUTO' && <Chip>auto</Chip>}
        {field.visibility === 'WRITE_ONLY' && <Chip>write-only</Chip>}
      </button>
    </li>
  )
}

function Chip({ children }: { children: string }) {
  return (
    <span className="rounded-[5px] bg-input px-2 py-px text-[11px] font-semibold text-text-secondary">
      {children}
    </span>
  )
}
