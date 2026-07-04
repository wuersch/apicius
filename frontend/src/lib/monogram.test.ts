import { describe, expect, test } from 'vitest'
import { monogram } from './monogram'

// FEAT-002 AC2: each card carries a single-letter monogram tile (mockup: "S" for Storefront API).
describe('monogram', () => {
  test('takes the first letter of the title, uppercased', () => {
    expect(monogram('Storefront API')).toBe('S')
    expect(monogram('billing API')).toBe('B')
  })

  test('skips leading non-alphanumeric characters', () => {
    expect(monogram('  (internal) Tools')).toBe('I')
  })

  test('falls back to "?" when there is nothing to show', () => {
    expect(monogram('')).toBe('?')
    expect(monogram('···')).toBe('?')
  })
})
