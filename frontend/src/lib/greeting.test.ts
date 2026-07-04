import { describe, expect, test } from 'vitest'
import { formatFriendlyDate, timeOfDayGreeting } from './greeting'

// FEAT-002 AC6: the home greets with the time of day; boundaries are fixed dates, no fake timers.
describe('timeOfDayGreeting', () => {
  test.each([
    [0, 'Good night'],
    [4, 'Good night'],
    [5, 'Good morning'],
    [11, 'Good morning'],
    [12, 'Good afternoon'],
    [17, 'Good afternoon'],
    [18, 'Good evening'],
    [21, 'Good evening'],
    [22, 'Good night'],
    [23, 'Good night'],
  ])('at %i:00 → %s', (hour, expected) => {
    expect(timeOfDayGreeting(new Date(2026, 5, 27, hour, 0))).toBe(expected)
  })
})

// FEAT-002 AC6: the current date, in the mockup's "Saturday, June 27" shape.
describe('formatFriendlyDate', () => {
  test('formats weekday, month and day without a year', () => {
    expect(formatFriendlyDate(new Date(2026, 5, 27), 'en-US')).toBe('Saturday, June 27')
  })
})
