// The TS half of the shared-vector contract for FEAT-011: every vector in the backend-owned
// declaration-vectors.json asserted against this mirror. The Java half is
// backend/src/test/java/dev/apicius/document/derivation/DeclarationVectorsTest.java —
// together they keep the backend writer and this presentation mirror from drifting.

/// <reference types="node" />
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import {
  deriveHeaderName,
  isReservedHeaderName,
  normalizeOneOfValues,
  oneOfValuesProblem,
} from './headerDerivation'

type Vectors = {
  headerNames: { valid: { name: string; headerName: string }[]; deriveToEmpty: string[] }
  reservedHeaderNames: { reserved: string[]; free: string[] }
  oneOf: {
    serialization: { values: string[]; schema: { type: string; enum: string[] } }[]
    invalid: { values: string[]; why: string }[]
  }
}

// Vitest runs with cwd = frontend/, so the backend's canonical file is one level up.
const vectors: Vectors = JSON.parse(
  readFileSync(
    resolve(process.cwd(), '../backend/src/test/resources/derivation/declaration-vectors.json'),
    'utf-8',
  ),
)

describe('declaration vectors (shared with the backend)', () => {
  it.each(vectors.headerNames.valid)('derives "$name" → "$headerName"', (vector) => {
    expect(deriveHeaderName(vector.name)).toBe(vector.headerName)
  })

  it.each(vectors.headerNames.deriveToEmpty.map((name) => [name]))(
    'derives %j to nothing',
    (name) => {
      expect(deriveHeaderName(name)).toBe('')
    },
  )

  it.each(vectors.reservedHeaderNames.reserved.map((name) => [name]))(
    'reserves %j',
    (name) => {
      expect(isReservedHeaderName(name)).toBe(true)
    },
  )

  it.each(vectors.reservedHeaderNames.free.map((name) => [name]))(
    'leaves %j free',
    (name) => {
      expect(isReservedHeaderName(name)).toBe(false)
    },
  )

  it.each(vectors.oneOf.serialization)('normalizes $values to the enum', (vector) => {
    expect(oneOfValuesProblem(vector.values)).toBeNull()
    expect(normalizeOneOfValues(vector.values)).toEqual(vector.schema.enum)
    expect(vector.schema.type).toBe('string')
  })

  it.each(vectors.oneOf.invalid)('rejects $values ($why)', (vector) => {
    expect(oneOfValuesProblem(vector.values)).not.toBeNull()
  })
})
