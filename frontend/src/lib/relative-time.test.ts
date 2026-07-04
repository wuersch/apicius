import { describe, expect, test } from 'vitest'
import { formatRelativeAge } from './relative-time'

// FEAT-002 AC2: relative last-edited in the spec's "2d ago" shape; pure function, fixed now.
describe('formatRelativeAge', () => {
  const now = new Date('2026-06-27T12:00:00Z')

  test.each([
    ['2026-06-27T11:59:30Z', 'just now'],
    ['2026-06-27T11:55:00Z', '5m ago'],
    ['2026-06-27T09:00:00Z', '3h ago'],
    ['2026-06-25T12:00:00Z', '2d ago'],
    ['2026-06-13T12:00:00Z', '2w ago'],
    ['2026-03-27T12:00:00Z', '3mo ago'],
    ['2024-06-27T12:00:00Z', '2y ago'],
  ])('%s → %s', (from, expected) => {
    expect(formatRelativeAge(new Date(from), now)).toBe(expected)
  })

  test('a future timestamp (clock skew) clamps to "just now"', () => {
    expect(formatRelativeAge(new Date('2026-06-27T12:05:00Z'), now)).toBe('just now')
  })
})
