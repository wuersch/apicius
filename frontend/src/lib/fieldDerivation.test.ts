// The TS half of the shared-vector contract for FEAT-006: every vector in the backend-owned
// field-vectors.json asserted against this mirror. The Java half is
// backend/src/test/java/dev/apicius/document/derivation/FieldVectorsTest.java — together they
// keep the backend writer and this presentation mirror from drifting.

/// <reference types="node" />
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import type { CoreType, FieldVisibility, Refinement } from '@/api/model'
import {
  defaultVisibility,
  derivePropertyName,
  describeKind,
  REFINEMENTS_BY_CORE_TYPE,
  serializationPreview,
  serializedScalar,
} from './fieldDerivation'

type KindVector = {
  coreType: CoreType
  refinement: Refinement | null
  list: boolean
  type: string
  format: string | null
}

type Vectors = {
  propertyNames: { valid: { name: string; propertyName: string }[]; deriveToEmpty: string[] }
  kinds: KindVector[]
  incompatible: { coreType: CoreType; refinement: Refinement }[]
  visibilityDefaults: { refinement: Refinement | null; default: FieldVisibility }[]
}

// Vitest runs with cwd = frontend/, so the backend's canonical file is one level up.
const vectors: Vectors = JSON.parse(
  readFileSync(
    resolve(process.cwd(), '../backend/src/test/resources/derivation/field-vectors.json'),
    'utf-8',
  ),
)

describe('field vectors (shared with the backend)', () => {
  it.each(vectors.propertyNames.valid)('derives "$name" → "$propertyName"', (vector) => {
    expect(derivePropertyName(vector.name)).toBe(vector.propertyName)
  })

  it.each(vectors.propertyNames.deriveToEmpty.map((name) => [name]))(
    'derives %j to nothing',
    (name) => {
      expect(derivePropertyName(name)).toBe('')
    },
  )

  it.each(vectors.kinds)('serializes $coreType as $refinement (list: $list)', (vector) => {
    expect(
      serializedScalar({ coreType: vector.coreType, refinement: vector.refinement, list: vector.list }),
    ).toEqual({ type: vector.type, format: vector.format })
  })

  it.each(vectors.incompatible)(
    'never offers $refinement for $coreType',
    ({ coreType, refinement }) => {
      expect(REFINEMENTS_BY_CORE_TYPE[coreType].map((entry) => entry.value)).not.toContain(
        refinement,
      )
    },
  )

  it.each(vectors.visibilityDefaults)(
    'defaults visibility to $default for $refinement',
    ({ refinement, default: expected }) => {
      expect(defaultVisibility(refinement)).toBe(expected)
    },
  )
})

describe('serializationPreview', () => {
  // The footer lines of mockup views 2d·1, 2d·2, 2d·3, 2d·5.
  it('renders the mockup footer lines', () => {
    expect(serializationPreview({ coreType: 'WHOLE_NUMBER', refinement: null, list: false }, true, 'NORMAL'))
      .toBe('→ integer · in required')
    expect(serializationPreview({ coreType: 'TEXT', refinement: 'UUID', list: true }, false, 'NORMAL'))
      .toBe('→ array of string · uuid')
    expect(serializationPreview({ coreType: 'TEXT', refinement: 'PASSWORD', list: false }, false, 'WRITE_ONLY'))
      .toBe('→ string · password · write-only')
    expect(serializationPreview({ coreType: 'TEXT', refinement: null, list: false }, false, 'AUTO'))
      .toBe('→ string · read-only')
  })
})

describe('describeKind', () => {
  it('speaks the row vocabulary', () => {
    expect(describeKind({ coreType: 'TEXT', refinement: null, list: false })).toBe('text')
    expect(describeKind({ coreType: 'YES_NO', refinement: null, list: false })).toBe('yes / no')
    expect(describeKind({ coreType: 'TEXT', refinement: 'EMAIL', list: false })).toBe('text · email')
    expect(describeKind({ coreType: 'TEXT', refinement: 'UUID', list: true })).toBe('list of text · UUID')
    expect(describeKind({ coreType: 'DATE_TIME', refinement: null, list: false })).toBe('date & time')
  })
})
