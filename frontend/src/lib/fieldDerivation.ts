// FEAT-006: ADR-0011's presentation half, mirrored for the field editor's live preview —
// property name, kind vocabulary, serialization preview, the password house-rule default.
// The backend (FieldNameDerivation / FieldKind) is the only writer; this module never
// produces document content. Kept honest by the shared vectors in
// backend/src/test/resources/derivation/field-vectors.json, asserted by both test suites
// (fieldDerivation.test.ts here, FieldVectorsTest there).

import type { CoreType, FieldVisibility, Refinement } from '@/api/model'

/**
 * Freeform field name → the JSON property name that is the field's identity (ADR-0011):
 * spaces trigger camelCase joining, identifier characters pass through, everything else is
 * stripped visibly. Returns '' when nothing survives — the blocked AC9 state.
 */
export function derivePropertyName(rawName: string): string {
  const words = rawName
    .split(' ')
    // Unicode letters/digits, like the backend's Character.isLetterOrDigit.
    .map((word) => word.replace(/[^\p{L}\p{Nd}_]/gu, ''))
    .filter((word) => word.length > 0)
  if (words.length === 0) return ''
  const [first, ...rest] = words
  return leadingWord(first) + rest.map((word) => word[0].toUpperCase() + word.slice(1)).join('')
}

/** An all-caps leading word is an acronym starting the name — lowercased whole ("API key"). */
function leadingWord(word: string): string {
  const allCaps = word === word.toUpperCase() && word !== word.toLowerCase()
  return allCaps ? word.toLowerCase() : word[0].toLowerCase() + word.slice(1)
}

/** The three-slot kind (ADR-0011) as the editor holds it. */
export type FieldKind = {
  coreType: CoreType
  refinement: Refinement | null
  list: boolean
}

/**
 * The core-type vocabulary in dropdown order (ADR-0011's row order), each entry carrying its
 * label and its serialization — one table read for both, like the backend's enum constants.
 */
export const CORE_TYPES: { value: CoreType; label: string; type: string; format: string | null }[] = [
  { value: 'TEXT', label: 'Text', type: 'string', format: null },
  { value: 'WHOLE_NUMBER', label: 'Whole number', type: 'integer', format: null },
  { value: 'DECIMAL_NUMBER', label: 'Decimal number', type: 'number', format: null },
  { value: 'YES_NO', label: 'Yes / no', type: 'boolean', format: null },
  { value: 'DATE', label: 'Date', type: 'string', format: 'date' },
  { value: 'DATE_TIME', label: 'Date & time', type: 'string', format: 'date-time' },
]

/**
 * Refinements per core type — the row binding that makes an incompatible pair impossible to
 * construct through the UI (the server still validates for other clients). An empty list
 * means the core type refines to nothing (its dropdown stays on "plain").
 */
export const REFINEMENTS_BY_CORE_TYPE: Record<
  CoreType,
  { value: Refinement; label: string; format: string }[]
> = {
  TEXT: [
    { value: 'EMAIL', label: 'email', format: 'email' },
    { value: 'UUID', label: 'UUID', format: 'uuid' },
    { value: 'URL', label: 'URL', format: 'uri' },
    { value: 'PASSWORD', label: 'password', format: 'password' },
  ],
  WHOLE_NUMBER: [
    { value: 'INT32', label: '32-bit', format: 'int32' },
    { value: 'INT64', label: '64-bit', format: 'int64' },
  ],
  DECIMAL_NUMBER: [
    { value: 'FLOAT', label: 'float', format: 'float' },
    { value: 'DOUBLE', label: 'double', format: 'double' },
  ],
  YES_NO: [],
  DATE: [],
  DATE_TIME: [],
}

/** The scalar half of ADR-0011's serialization (lists wrap it as array + items). */
export function serializedScalar(kind: FieldKind): { type: string; format: string | null } {
  const core = CORE_TYPES.find((entry) => entry.value === kind.coreType)!
  const refinement = REFINEMENTS_BY_CORE_TYPE[kind.coreType].find(
    (entry) => entry.value === kind.refinement,
  )
  return { type: core.type, format: refinement ? refinement.format : core.format }
}

/**
 * The footer's derived-serialization line (mockup 2c/2d): "→ string · in required",
 * "→ array of string · uuid", "→ string · password · write-only".
 */
export function serializationPreview(
  kind: FieldKind,
  required: boolean,
  visibility: FieldVisibility,
): string {
  const { type, format } = serializedScalar(kind)
  const parts = [kind.list ? `array of ${type}` : type]
  if (format) parts.push(format)
  if (visibility === 'AUTO') parts.push('read-only')
  if (visibility === 'WRITE_ONLY') parts.push('write-only')
  if (required) parts.push('in required')
  return `→ ${parts.join(' · ')}`
}

/** The field row's plain-language kind (AC11), mockup-style: "text", "text · email". */
export function describeKind(kind: FieldKind): string {
  const core = CORE_TYPES.find((entry) => entry.value === kind.coreType)?.label.toLowerCase() ?? ''
  const refinement = REFINEMENTS_BY_CORE_TYPE[kind.coreType]
      .find((entry) => entry.value === kind.refinement)?.label
  const scalar = refinement ? `${core} · ${refinement}` : core
  return kind.list ? `list of ${scalar}` : scalar
}

/**
 * The house rule (ADR-0011, AC5): Text as password defaults to write-only. Mirrors the
 * server's default for display; the request omits visibility unless the designer chose one,
 * so the server's own rule stays authoritative.
 */
export function defaultVisibility(refinement: Refinement | null): FieldVisibility {
  return refinement === 'PASSWORD' ? 'WRITE_ONLY' : 'NORMAL'
}
