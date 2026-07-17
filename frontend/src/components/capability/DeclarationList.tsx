import { Fragment, useState } from 'react'
import type { Capability, DeclarationResponse } from '@/api/model'
import { DeclarationRow } from '@/components/capability/DeclarationRow'
import {
  DeclarationEditor,
  useDeclarationRemoval,
  type DeclarationLocation,
} from '@/components/capability/DeclarationEditor'
import { useContractInvalidation } from '@/components/capability/useContractInvalidation'

/** The open editor: docked at a row, or the add editor at the end — never a sentinel name. */
export type Editing = { mode: 'add' } | { mode: 'edit'; name: string } | null

// FEAT-011: one location's declarations as a card body — rows in document order, the inline
// editor docked at the point of the row it creates or edits (the ShapeSection discipline:
// one editor open at a time per card, everything else inert while it is). Every mutation
// invalidates the contract — what is shown is projected back, nothing echoed locally.
export function DeclarationList({
  specId,
  schemaName,
  capability,
  location,
  declarations,
  showOptionality,
  pagingOn,
  editing,
  onEditingChange,
}: {
  specId: string
  schemaName: string
  capability: Capability
  location: DeclarationLocation
  declarations: DeclarationResponse[]
  showOptionality: boolean
  /** Passed through to the editor's page/limit pre-check (query parameters only). */
  pagingOn?: boolean
  editing: Editing
  onEditingChange: (editing: Editing) => void
}) {
  const remove = useDeclarationRemoval(location)
  const invalidate = useContractInvalidation(specId, schemaName, capability)
  const [removeFailed, setRemoveFailed] = useState(false)
  const existingNames = declarations.map((declaration) => declaration.name ?? '')
  const close = () => onEditingChange(null)
  const saved = () => {
    invalidate()
    onEditingChange(null)
  }

  function handleRemove(name: string) {
    setRemoveFailed(false)
    // A local row's Remove deletes outright — everything here is capability-local.
    remove.mutate(
      { specId, schemaName, capability, name },
      { onSuccess: invalidate, onError: () => setRemoveFailed(true) },
    )
  }

  const idle = editing === null && !remove.isPending
  return (
    <>
      <ul>
        {declarations.map((declaration) => {
          const isEditing = editing?.mode === 'edit' && editing.name === declaration.name
          return (
            <Fragment key={declaration.name}>
              <DeclarationRow
                declaration={declaration}
                showOptionality={showOptionality}
                editing={isEditing}
                onEdit={
                  idle
                    ? () => onEditingChange({ mode: 'edit', name: declaration.name ?? '' })
                    : undefined
                }
                onRemove={idle ? () => handleRemove(declaration.name ?? '') : undefined}
              />
              {isEditing && (
                <DeclarationEditor
                  specId={specId}
                  schemaName={schemaName}
                  capability={capability}
                  location={location}
                  existingNames={existingNames}
                  pagingOn={pagingOn}
                  declaration={declaration}
                  onSaved={saved}
                  onClose={close}
                />
              )}
            </Fragment>
          )
        })}
        {editing?.mode === 'add' && (
          <DeclarationEditor
            specId={specId}
            schemaName={schemaName}
            capability={capability}
            location={location}
            existingNames={existingNames}
            pagingOn={pagingOn}
            onSaved={saved}
            onClose={close}
          />
        )}
      </ul>
      {removeFailed && (
        <p role="alert" className="mt-2 text-[12.5px] text-terracotta">
          That didn't save — try again.
        </p>
      )}
    </>
  )
}
