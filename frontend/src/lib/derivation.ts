// FEAT-005: ADR-0010's presentation half, mirrored for the dialog's live preview — schema
// name, collection path, plain-language labels, verb/path. The backend (CanonicalDerivation)
// is the only writer; this module never produces document content. Kept honest by the shared
// vectors in backend/src/test/resources/derivation/derivation-vectors.json, asserted by both
// test suites (derivation.test.ts here, DerivationVectorsTest there).

import { Capability } from '@/api/model'

/** The "cleanly derivable" rule: starts with a letter; letter/digit words, single spaces. */
export const NAME_PATTERN = /^[A-Za-z][A-Za-z0-9]*( [A-Za-z0-9]+)*$/

/** Dialog row order — also the backend's derivation order. */
export const ALL_CAPABILITIES: Capability[] = [
  Capability.BROWSE,
  Capability.LOOK_UP,
  Capability.ADD,
  Capability.UPDATE,
  Capability.REMOVE,
]

// Singular → plural; a word that already is one of these plurals returns itself (People and
// Person both derive /people — the backend rejects the collision). No f→ves rule, matching
// the backend's wrong-but-consistent stance.
const IRREGULARS: Record<string, string> = {
  person: 'people',
  child: 'children',
  man: 'men',
  woman: 'women',
  foot: 'feet',
  tooth: 'teeth',
  mouse: 'mice',
  goose: 'geese',
}
const IRREGULAR_PLURALS = new Set(Object.values(IRREGULARS))

export function pluralize(word: string): string {
  const lower = word.toLowerCase()
  if (IRREGULAR_PLURALS.has(lower)) return word
  const irregular = IRREGULARS[lower]
  if (irregular) return matchFirstLetterCase(irregular, word)
  if (/(s|x|z|ch|sh)$/.test(lower)) return `${word}es`
  if (lower.length > 1 && lower.endsWith('y') && !isVowel(lower[lower.length - 2])) {
    return `${word.slice(0, -1)}ies`
  }
  return `${word}s`
}

export type DerivedNaming = {
  schemaName: string
  collectionPath: string
  itemPath: string
}

/** Naming for the mono preview line; null while the name isn't cleanly derivable yet. */
export function deriveNaming(trimmedName: string): DerivedNaming | null {
  if (!NAME_PATTERN.test(trimmedName)) return null
  const words = trimmedName.split(' ')
  const collectionPath = `/${pluralizeLast(words.map((word) => word.toLowerCase())).join('-')}`
  return {
    schemaName: words.map(pascalWord).join(''),
    collectionPath,
    itemPath: `${collectionPath}/{id}`,
  }
}

export type DerivedCapability = {
  label: string
  method: string
  path: string
}

/** One capability row's label and derived verb/path for the given noun. */
export function deriveCapability(trimmedName: string, capability: Capability): DerivedCapability {
  const naming = deriveNaming(trimmedName)
  if (!naming) throw new Error(`underivable name: ${trimmedName}`)
  const words = trimmedName.split(' ')
  const singular = displayNoun(words, false)
  const plural = displayNoun(words, true)
  const article = articleFor(singular)
  switch (capability) {
    case Capability.BROWSE:
      return { label: `Browse all ${plural}`, method: 'GET', path: naming.collectionPath }
    case Capability.LOOK_UP:
      return { label: `Look up one ${singular}`, method: 'GET', path: naming.itemPath }
    case Capability.ADD:
      return { label: `Add ${article} ${singular}`, method: 'POST', path: naming.collectionPath }
    case Capability.UPDATE:
      return { label: `Update ${article} ${singular}`, method: 'PATCH', path: naming.itemPath }
    case Capability.REMOVE:
      return { label: `Remove ${article} ${singular}`, method: 'DELETE', path: naming.itemPath }
  }
}

/** The footer's promise: how many operations on how many distinct paths a selection creates. */
export function countDerived(selected: Capability[]): { operations: number; paths: number } {
  const onCollection = selected.some(
    (capability) => capability === Capability.BROWSE || capability === Capability.ADD,
  )
  const onItem = selected.some(
    (capability) => capability !== Capability.BROWSE && capability !== Capability.ADD,
  )
  return { operations: selected.length, paths: Number(onCollection) + Number(onItem) }
}

/** The noun as labels display it ("order item"), for copy outside the label templates. */
export function displayNounOf(trimmedName: string): string | null {
  if (!NAME_PATTERN.test(trimmedName)) return null
  return displayNoun(trimmedName.split(' '), false)
}

/** By first letter, not by sound — deterministic, matching the backend's label rule. */
export function articleFor(noun: string): 'a' | 'an' {
  return isVowel(noun[0]) ? 'an' : 'a'
}

/** PascalCase preserving inner case, so acronyms survive ("API key" → "APIKey"). */
function pascalWord(word: string): string {
  return word[0].toUpperCase() + word.slice(1)
}

/** Labels keep a word's casing unless it is ordinary — "Browse all API keys", not "api keys". */
function displayNoun(words: string[], plural: boolean): string {
  const display = words.map((word) => {
    const rest = word.slice(1)
    return rest === rest.toLowerCase() ? word[0].toLowerCase() + rest : word
  })
  return (plural ? pluralizeLast(display) : display).join(' ')
}

function pluralizeLast(words: string[]): string[] {
  return words.map((word, index) => (index === words.length - 1 ? pluralize(word) : word))
}

function matchFirstLetterCase(plural: string, original: string): string {
  return original[0] === original[0].toUpperCase()
    ? plural[0].toUpperCase() + plural.slice(1)
    : plural
}

function isVowel(character: string): boolean {
  return 'aeiou'.includes(character.toLowerCase())
}
