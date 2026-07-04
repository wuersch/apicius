import { useMemo } from 'react'
import { useAuth } from 'react-oidc-context'
import { Plus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { ApiCard } from '@/components/home/ApiCard'
import { EmptyState } from '@/components/home/EmptyState'
import { JumpBackInCard } from '@/components/home/JumpBackInCard'
import { getFirstName } from '@/auth/initials'
import { formatFriendlyDate, timeOfDayGreeting } from '@/lib/greeting'
import { useGetCurrentUser } from '@/api/endpoints/users/users'
import { useGetLastEditedLocation, useListSpecs } from '@/api/endpoints/specs/specs'

// FEAT-002: the My APIs home — greeting, jump-back-in, and the card grid (or empty state).
// A read-only projection surface: the backend orders and shapes everything (ADR-0008).
export function HomePage() {
  const auth = useAuth()
  // Gated on the token so no query fires before auth is wired (FEAT-001 pattern).
  const enabled = Boolean(auth.user?.access_token)
  const { data: userResponse } = useGetCurrentUser({ query: { enabled } })
  const { data: listResponse } = useListSpecs({ query: { enabled } })
  const { data: lastEditedResponse } = useGetLastEditedLocation({ query: { enabled } })

  const user = userResponse?.status === 200 ? userResponse.data : undefined
  const list = listResponse?.status === 200 ? listResponse.data : undefined
  // 204 = nothing recorded yet → no jump-back-in card (AC1).
  const lastEdited = lastEditedResponse?.status === 200 ? lastEditedResponse.data : undefined

  // One clock read per render pass; all time-derived copy below shares it.
  const now = useMemo(() => new Date(), [])
  const firstName = getFirstName(auth.user?.profile, user?.displayName)

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-8 px-11 py-8">
      <header className="flex items-end justify-between gap-4">
        <div>
          {/* AC6: time of day + name; date below. */}
          <h1 className="text-3xl font-bold">
            {firstName ? `${timeOfDayGreeting(now)}, ${firstName}` : timeOfDayGreeting(now)}
          </h1>
          <p className="mt-1.5 text-sm text-text-tertiary">{formatFriendlyDate(now)}</p>
        </div>
        {/* The permanent create/import affordance (mockup v4 placement). Inert until FEAT-003/004. */}
        <Button disabled className="shrink-0">
          <Plus aria-hidden className="size-4" />
          New API
        </Button>
      </header>

      {lastEdited?.specId && <JumpBackInCard location={lastEdited} now={now} />}

      {list &&
        (list.items?.length ? (
          <section aria-label="All APIs">
            <div className="flex items-baseline gap-2">
              <h2 className="text-base font-bold">All APIs</h2>
              <span className="font-mono text-sm text-text-faint">{list.total}</span>
            </div>
            <div className="mt-3 grid grid-cols-1 gap-4 sm:grid-cols-2">
              {list.items.map((spec) => (
                <ApiCard key={spec.id} spec={spec} now={now} />
              ))}
            </div>
          </section>
        ) : (
          <EmptyState />
        ))}
    </div>
  )
}
