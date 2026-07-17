import { Link } from 'react-router'
import type { ResourceResponse } from '@/api/model'
import { METHOD_DOT_CLASS } from '@/components/editor/method-dot'

// FEAT-009: the noun's "can do" list travels with the capability — siblings one step away,
// the open one marked. Entries grow only as features add them: no TOC, no palette, no
// foreshadowed rows (Interaction Model).
export function CapabilityRail({
  specId,
  resource,
  current,
}: {
  specId: string
  resource: ResourceResponse
  current: string
}) {
  return (
    <nav aria-label={`Capabilities of ${resource.name}`} className="w-52 shrink-0">
      <div className="text-[11px] font-semibold tracking-[.1em] text-text-tertiary uppercase">
        {resource.name} can…
      </div>
      <ul className="mt-2 flex flex-col gap-0.5">
        {resource.capabilities?.map((capability) => {
          const active = capability.capability === current
          return (
            <li key={capability.capability}>
              <Link
                to={`/apis/${specId}/resources/${resource.name}/capabilities/${capability.capability}`}
                aria-current={active ? 'page' : undefined}
                className={`flex items-center gap-2.5 rounded-md px-2.5 py-1.5 text-[13px] transition-colors ${
                  active ? 'bg-accent font-semibold' : 'font-medium hover:bg-accent/60'
                }`}
              >
                <span
                  aria-hidden
                  className={`size-2 shrink-0 rounded-full ${METHOD_DOT_CLASS[capability.method ?? ''] ?? 'bg-ring'}`}
                />
                <span className="truncate">{capability.label}</span>
              </Link>
            </li>
          )
        })}
      </ul>
    </nav>
  )
}
