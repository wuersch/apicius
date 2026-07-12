import { Link } from 'react-router'
import { Card } from '@/components/ui/card'
import { ApiCardActions } from '@/components/home/ApiCardActions'
import { monogram } from '@/lib/monogram'
import { formatRelativeAge } from '@/lib/relative-time'
import type { SpecSummaryResponse } from '@/api/model'

// The monogram tiles cycle a small warm palette (the mockup hand-picks per-API colors;
// a deterministic pick keeps the variety without storing a color).
const TILE_STYLES = [
  'bg-olive-chip text-olive-chip-foreground',
  'bg-ochre-chip text-ochre',
  'bg-muted text-text-secondary',
]

function tileStyle(title: string): string {
  return TILE_STYLES[title.length % TILE_STYLES.length]
}

// FEAT-002 AC2: one card per API, in domain vocabulary — resources and ops, never paths.
// FEAT-007: the ⋯ actions sit above a stretched-link overlay — a button nested inside the
// old whole-card anchor would be invalid HTML and would navigate instead of opening the menu.
export function ApiCard({ spec, now }: { spec: SpecSummaryResponse; now: Date }) {
  const title = spec.title ?? ''
  return (
    <Card className="relative h-full gap-3 rounded-lg px-4 shadow-xs ring-border transition-shadow hover:shadow-md has-[a:focus-visible]:ring-2 has-[a:focus-visible]:ring-ring">
      <Link
        to={`/apis/${spec.id}`}
        aria-label={title}
        className="absolute inset-0 z-0 rounded-lg outline-none"
      />
      <div className="flex items-start gap-3">
        <div
          aria-hidden
          className={`flex size-9 shrink-0 items-center justify-center rounded-md text-sm font-bold ${tileStyle(title)}`}
        >
          {monogram(title)}
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <span className="truncate text-[15px] font-bold">{title}</span>
            <span className="rounded-sm bg-secondary px-1.5 py-0.5 font-mono text-[10.5px] text-text-secondary">
              v{spec.apiVersion}
            </span>
          </div>
          <p className="mt-0.5 line-clamp-2 text-[13px] text-text-tertiary">{spec.description}</p>
        </div>
        <div className="relative z-10 -mt-1 -mr-2 shrink-0">
          <ApiCardActions spec={spec} />
        </div>
      </div>
      <div className="flex items-center font-mono text-[11.5px] text-text-faint">
        <span>
          {spec.resourceCount} resources · {spec.operationCount} ops
        </span>
        {spec.updatedAt && <span className="ml-auto">{formatRelativeAge(new Date(spec.updatedAt), now)}</span>}
      </div>
    </Card>
  )
}
