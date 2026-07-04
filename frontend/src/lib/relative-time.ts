// FEAT-002 AC2: relative last-edited ("2d ago"). Hand-rolled buckets rather than
// Intl.RelativeTimeFormat, whose narrow style varies across ICU builds and wouldn't
// reliably produce the spec's exact copy.

const MINUTE = 60
const HOUR = 60 * MINUTE
const DAY = 24 * HOUR
const WEEK = 7 * DAY
const MONTH = 30 * DAY
const YEAR = 365 * DAY

export function formatRelativeAge(from: Date, now: Date): string {
  const seconds = Math.floor((now.getTime() - from.getTime()) / 1000)
  if (seconds < MINUTE) return 'just now'
  if (seconds < HOUR) return `${Math.floor(seconds / MINUTE)}m ago`
  if (seconds < DAY) return `${Math.floor(seconds / HOUR)}h ago`
  if (seconds < WEEK) return `${Math.floor(seconds / DAY)}d ago`
  if (seconds < MONTH) return `${Math.floor(seconds / WEEK)}w ago`
  if (seconds < YEAR) return `${Math.floor(seconds / MONTH)}mo ago`
  return `${Math.floor(seconds / YEAR)}y ago`
}
