// FEAT-002 AC6: greeting copy follows the time of day; the date reads like the mockup's
// "Saturday, June 27". Pure functions — callers pass the clock in.

export function timeOfDayGreeting(now: Date): string {
  const hour = now.getHours()
  if (hour >= 5 && hour < 12) return 'Good morning'
  if (hour >= 12 && hour < 18) return 'Good afternoon'
  if (hour >= 18 && hour < 22) return 'Good evening'
  return 'Good night'
}

export function formatFriendlyDate(now: Date, locale?: string): string {
  return new Intl.DateTimeFormat(locale, {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
  }).format(now)
}
