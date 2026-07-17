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
    <nav aria-label={`Capabilities of ${resource.name}`} className="w-[198px] shrink-0">
      <div className="px-[11px] pt-1 pb-2 text-[11px] font-semibold tracking-[.1em] text-text-tertiary uppercase">
        {resource.name} can…
      </div>
      <ul className="flex flex-col">
        {resource.capabilities?.map((capability) => {
          const active = capability.capability === current
          return (
            <li key={capability.capability}>
              <Link
                to={`/apis/${specId}/resources/${resource.name}/capabilities/${capability.capability}`}
                aria-current={active ? 'page' : undefined}
                // The open capability lifts on the card surface (View 3); siblings rest
                // quiet and warm on hover.
                className={`flex items-center gap-[11px] rounded-md px-[11px] py-[9px] text-sm transition-colors ${
                  active
                    ? 'bg-card font-semibold shadow-sm'
                    : 'text-text-secondary hover:bg-input'
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
