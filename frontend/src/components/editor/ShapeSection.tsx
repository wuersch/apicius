import { Fragment, useState } from 'react'
import { Plus } from 'lucide-react'
import type { ResourceResponse } from '@/api/model'
import { FieldEditor } from '@/components/editor/FieldEditor'
import { FieldRow } from '@/components/editor/FieldRow'

// The open editor: docked at a field's row, or the add editor at the end of the list. A
// discriminated pair, not a sentinel string — a field can legitimately be named anything
// derivation lets through, so no property name may double as a mode marker.
type Editing = { mode: 'add' } | { mode: 'edit'; name: string } | null

// FEAT-006: a resource's shape — the id row locked first (AC7/AC11), one row per field, and
// the inline editor docked at the point of the row it creates or edits (never a dialog:
// dialogs create nouns, inline shapes them). One editor open at a time; everything else
// inert while it is — the row being edited stays visible, highlighted (mockup 2d·6).
export function ShapeSection({
  specId,
  resource,
}: {
  specId: string
  resource: ResourceResponse
}) {
  const [editing, setEditing] = useState<Editing>(null)
  const fields = resource.fields ?? []
  const existingNames = fields.map((field) => field.name ?? '')
  const close = () => setEditing(null)

  return (
    <section aria-label={`Shape of ${resource.name}`}>
      <div className="mt-4 text-[11px] font-semibold tracking-[.1em] text-text-tertiary uppercase">
        Shape
      </div>
      <ul className="mt-2 overflow-hidden rounded-md border border-band-border">
        {fields.map((field) => {
          const isEditing = editing?.mode === 'edit' && editing.name === field.name
          return (
            <Fragment key={field.name}>
              <FieldRow
                field={field}
                locked={field.name === 'id'}
                editing={isEditing}
                onEdit={
                  editing === null
                    ? () => setEditing({ mode: 'edit', name: field.name ?? '' })
                    : undefined
                }
              />
              {isEditing && (
                <FieldEditor
                  specId={specId}
                  resourceName={resource.name ?? ''}
                  existingNames={existingNames}
                  field={field}
                  onClose={close}
                />
              )}
            </Fragment>
          )
        })}
        {editing?.mode === 'add' ? (
          <FieldEditor
            specId={specId}
            resourceName={resource.name ?? ''}
            existingNames={existingNames}
            onClose={close}
          />
        ) : (
          <li>
            <button
              type="button"
              onClick={editing === null ? () => setEditing({ mode: 'add' }) : undefined}
              disabled={editing !== null}
              className="flex w-full items-center gap-2 px-3.5 py-2 text-[13px] font-medium text-text-faint transition-colors enabled:hover:bg-accent enabled:hover:text-foreground disabled:cursor-default"
            >
              <Plus aria-hidden className="size-[13px]" />
              Add a field…
            </button>
          </li>
        )}
      </ul>
    </section>
  )
}
