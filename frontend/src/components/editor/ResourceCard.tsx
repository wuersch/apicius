import { useQueryClient } from '@tanstack/react-query'
import { ChevronDown } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import {
  getGetLastEditedLocationQueryKey,
  getGetSpecQueryKey,
  getListSpecsQueryKey,
  useUpdateResourceDescription,
} from '@/api/endpoints/specs/specs'
import type { ResourceResponse } from '@/api/model'
import { METHOD_DOT_CLASS } from '@/components/editor/method-dot'
import { ShapeSection } from '@/components/editor/ShapeSection'
import { QuietDescription } from '@/components/QuietDescription'

// FEAT-005 AC8: a created resource, shown by what it is (noun, description) and what it lets
// people do — the plain-language label leads, the derived verb/path is the de-emphasized
// mono column (PRIN-002). FEAT-006 adds the shape: the fields, edited inline.
export function ResourceCard({
  specId,
  resource,
  defaultOpen,
}: {
  specId: string
  resource: ResourceResponse
  defaultOpen?: boolean
}) {
  const [open, setOpen] = useState(defaultOpen ?? false)
  const fieldCount = resource.fields?.length ?? 0
  const capabilityCount = resource.capabilities?.length ?? 0
  const updateDescription = useUpdateResourceDescription()
  const queryClient = useQueryClient()

  // The shape-edit refresh (the FieldEditor trio): a resource edit re-projects the detail,
  // the list, and the jump-back-in pointer — no capability contract carries it.
  function invalidate() {
    queryClient.invalidateQueries({ queryKey: getGetSpecQueryKey(specId) })
    queryClient.invalidateQueries({ queryKey: getListSpecsQueryKey() })
    queryClient.invalidateQueries({ queryKey: getGetLastEditedLocationQueryKey() })
  }

  const toggle = () => setOpen((current) => !current)

  return (
    <article className="rounded-[10px] bg-card p-5 shadow-sm" aria-label={resource.name}>
      {/* The header splits so the note can edit in place (FEAT-012 UC3): the title row —
          title, dots, counts, chevron — keeps the expand gesture, the description tucks
          directly under the title (the mockup's tight noun block); the avatar is the
          button's silent double, hidden from the accessibility tree. */}
      <div className="flex w-full items-center gap-3">
        <button
          type="button"
          onClick={toggle}
          tabIndex={-1}
          aria-hidden="true"
          className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary text-sm font-bold text-primary-foreground"
        >
          {resource.name?.charAt(0)}
        </button>
        <div className="min-w-0 flex-1">
          <button
            type="button"
            onClick={toggle}
            aria-expanded={open}
            className="flex w-full items-center gap-3 text-left"
          >
            <h3 className="min-w-0 flex-1 truncate text-base font-bold">{resource.name}</h3>
            {!open && (
              <span aria-hidden className="flex gap-[5px]">
                {resource.capabilities?.map((capability) => (
                  <span
                    key={capability.capability}
                    className={`size-2 rounded-full ${METHOD_DOT_CLASS[capability.method ?? ''] ?? 'bg-ring'}`}
                  />
                ))}
              </span>
            )}
            <span className="font-mono text-xs whitespace-nowrap text-text-tertiary">
              {fieldCount} {fieldCount === 1 ? 'field' : 'fields'} · {capabilityCount}{' '}
              {capabilityCount === 1 ? 'capability' : 'capabilities'}
            </span>
            <ChevronDown
              aria-hidden
              strokeWidth={2.2}
              className={`size-[15px] shrink-0 text-text-faint transition-transform ${open ? 'rotate-180' : '-rotate-90'}`}
            />
          </button>
          {/* Read-only while the card is closed, edited in place while it is open. */}
          {open ? (
            <QuietDescription
              value={resource.description}
              ariaLabel="resource description"
              className="-mt-0.5 text-[13px] text-text-tertiary"
              onSave={(description) =>
                updateDescription
                  .mutateAsync({
                    specId,
                    schemaName: resource.name ?? '',
                    data: { description: description ?? undefined },
                  })
                  .then(invalidate)
              }
            />
          ) : (
            resource.description && (
              <p className="-mt-0.5 text-[13px] text-text-tertiary">{resource.description}</p>
            )
          )}
        </div>
      </div>

      {open && (
        <>
          <div className="mt-4 text-[11px] font-semibold tracking-[.1em] text-text-tertiary uppercase">
            What people can do
          </div>
          <ul className="mt-1.5">
            {resource.capabilities?.map((capability) => (
              // FEAT-009 UC1: each capability opens its contract view — the entry point is
              // the noun's "can do" list, never a path or method inventory.
              <li key={capability.capability}>
                <Link
                  to={`/apis/${specId}/resources/${resource.name}/capabilities/${capability.capability}`}
                  className="flex items-center gap-2.5 rounded-md px-2 py-1.5 transition-colors hover:bg-accent"
                >
                  <span
                    aria-hidden
                    className={`size-2 shrink-0 rounded-full ${METHOD_DOT_CLASS[capability.method ?? ''] ?? 'bg-ring'}`}
                  />
                  <span className="flex-1 text-sm font-medium">{capability.label}</span>
                  <span className="font-mono text-[11.5px] text-text-tertiary">
                    {capability.method} {capability.path}
                  </span>
                </Link>
              </li>
            ))}
          </ul>

          <ShapeSection specId={specId} resource={resource} />
        </>
      )}
    </article>
  )
}
