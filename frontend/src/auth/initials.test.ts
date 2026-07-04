import { describe, expect, test } from 'vitest'
import { getFirstName, getInitials } from './initials'

// FEAT-001 AC6: initials from first + last name claims, with graceful fallbacks.
describe('getInitials', () => {
  test('uses given_name and family_name when both are present', () => {
    expect(getInitials({ given_name: 'Ada', family_name: 'Lovelace' })).toBe('AL')
  })

  test('uses first and last word of the name claim when given/family are missing', () => {
    expect(getInitials({ name: 'Grace Brewster Hopper' })).toBe('GH')
  })

  test('uses a single letter for a one-word name', () => {
    expect(getInitials({ name: 'Ada' })).toBe('A')
  })

  test('falls back to preferred_username, then email', () => {
    expect(getInitials({ preferred_username: 'ghopper' })).toBe('G')
    expect(getInitials({ email: 'ada@example.com' })).toBe('A')
  })

  test('falls back to a placeholder when no usable claim exists', () => {
    expect(getInitials({})).toBe('?')
    expect(getInitials(undefined)).toBe('?')
  })

  test('ignores blank claims', () => {
    expect(getInitials({ given_name: '  ', name: 'Ada Lovelace' })).toBe('AL')
  })
})

// The mockups address the current user by first name.
describe('getFirstName', () => {
  test('prefers the given_name claim', () => {
    expect(getFirstName({ given_name: 'Ada', family_name: 'Lovelace' })).toBe('Ada')
  })

  test('falls back to the first word of the name claim', () => {
    expect(getFirstName({ name: 'Grace Hopper' })).toBe('Grace')
  })

  test('falls back to the first word of the provided full name', () => {
    expect(getFirstName(undefined, 'Mallory Might')).toBe('Mallory')
  })

  test('is undefined when no usable name is available', () => {
    expect(getFirstName({ email: 'ada@example.com' })).toBeUndefined()
    expect(getFirstName(undefined)).toBeUndefined()
  })
})
