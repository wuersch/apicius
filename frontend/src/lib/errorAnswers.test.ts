import { expect, test } from 'vitest'
import { failureName } from './errorAnswers'

// FEAT-009: the plain-language failure vocabulary — the 404 phrased from the noun.
test('names every standard failure in plain language', () => {
  expect(failureName('400', 'product')).toBe("we couldn't read the request")
  expect(failureName('401', 'product')).toBe('not signed in')
  expect(failureName('404', 'order item')).toBe('no order item with this id')
  expect(failureName('422', 'product')).toBe('bad input')
  expect(failureName('429', 'product')).toBe('too many requests')
  expect(failureName('500', 'product')).toBe('our fault')
})

test('yields nothing for a status outside the standard set', () => {
  expect(failureName('418', 'product')).toBe('')
})
