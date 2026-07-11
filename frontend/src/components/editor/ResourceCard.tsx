import type { ResourceResponse } from '@/api/model'
import { METHOD_DOT_CLASS } from '@/components/editor/method-dot'

// FEAT-005 AC8: a created resource, shown by what it is (noun, description) and what it lets
// people do — the plain-language label leads, the derived verb/path is the de-emphasized
// mono column (PRIN-002).
export function ResourceCard({ resource }: { resource: ResourceResponse }) {
  return (
    <article className="rounded-[10px] bg-card p-5 shadow-sm" aria-label={resource.name}>
      <div className="flex items-center gap-3">
        <div
          aria-hidden
          className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary text-sm font-bold text-primary-foreground"
        >
          {resource.name?.charAt(0)}
        </div>
        <div className="min-w-0">
          <h3 className="text-base font-bold">{resource.name}</h3>
          {resource.description && (
            <p className="text-[13px] text-text-tertiary">{resource.description}</p>
          )}
        </div>
      </div>

      <div className="mt-4 text-[11px] font-semibold tracking-[.1em] text-text-tertiary uppercase">
        What people can do
      </div>
      <ul className="mt-1.5">
        {resource.capabilities?.map((capability) => (
          <li key={capability.capability} className="flex items-center gap-2.5 rounded-md px-2 py-1.5">
            <span
              aria-hidden
              className={`size-2 shrink-0 rounded-full ${METHOD_DOT_CLASS[capability.method ?? ''] ?? 'bg-ring'}`}
            />
            <span className="flex-1 text-sm font-medium">{capability.label}</span>
            <span className="font-mono text-[11.5px] text-text-tertiary">
              {capability.method} {capability.path}
            </span>
          </li>
        ))}
      </ul>
    </article>
  )
}
