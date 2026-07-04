import { Link } from 'react-router'
import { ArrowRight } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { monogram } from '@/lib/monogram'
import { formatRelativeAge } from '@/lib/relative-time'
import type { LastEditedLocationResponse } from '@/api/model'

// FEAT-002 AC1: resume where the designer left off — names the capability when one was
// recorded, otherwise resolves to API-level. Navigates like a card (AC3).
export function JumpBackInCard({ location, now }: { location: LastEditedLocationResponse; now: Date }) {
  const title = location.specTitle ?? ''
  return (
    <section aria-label="Jump back in">
      <h2 className="text-[11px] font-semibold tracking-[.1em] uppercase text-text-faint">Jump back in</h2>
      <Link to={`/apis/${location.specId}`} className="group mt-2 block outline-none">
        <Card className="flex-row items-center gap-3.5 rounded-lg px-4 py-3.5 shadow-xs ring-border transition-shadow group-hover:shadow-md group-focus-visible:ring-2 group-focus-visible:ring-ring">
          {/* The resume tile inverts ink/cream (primary), unlike the grid tiles — mockup rule. */}
          <div
            aria-hidden
            className="flex size-[46px] shrink-0 items-center justify-center rounded-md bg-primary text-base font-bold text-primary-foreground"
          >
            {monogram(title)}
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2">
              <span className="truncate text-[15.5px] font-bold">{title}</span>
              <span className="rounded-sm bg-secondary px-1.5 py-0.5 font-mono text-[11px] text-text-secondary">
                v{location.apiVersion}
              </span>
            </div>
            <p className="mt-0.5 truncate text-[13px] text-text-tertiary">
              {location.capabilityName ? (
                <>
                  You were editing <strong className="font-semibold text-text-secondary">{location.capabilityName}</strong>
                </>
              ) : (
                <>You were editing this API</>
              )}
              {location.lastEditedAt && <> · {formatRelativeAge(new Date(location.lastEditedAt), now)}</>}
            </p>
          </div>
          <span className="flex shrink-0 items-center gap-1.5 rounded-md border border-ring px-3 py-1.5 text-[13px] font-semibold">
            Resume
            <ArrowRight aria-hidden className="size-3.5" />
          </span>
        </Card>
      </Link>
    </section>
  )
}
