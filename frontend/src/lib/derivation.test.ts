// The TS half of the shared-vector contract: every vector in the backend-owned
// derivation-vectors.json asserted against this mirror. The Java half is
// backend/src/test/java/dev/apicius/document/derivation/DerivationVectorsTest.java —
// together they keep the backend writer and this presentation mirror from drifting.
// (operationIds in the vectors are backend-only presentation; the Java suite asserts them.)

/// <reference types="node" />
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import type { Capability } from '@/api/model'
import {
  ALL_CAPABILITIES,
  countDerived,
  deriveCapability,
  deriveNaming,
  NAME_PATTERN,
} from './derivation'

type Vector = {
  name: string
  schemaName: string
  collectionPath: string
  itemPath: string
  labels: Record<Capability, string>
}

// Vitest runs with cwd = frontend/, so the backend's canonical file is one level up.
const vectors: { valid: Vector[]; invalid: string[] } = JSON.parse(
  readFileSync(
    resolve(process.cwd(), '../backend/src/test/resources/derivation/derivation-vectors.json'),
    'utf-8',
  ),
)

describe('derivation vectors (shared with the backend)', () => {
  it.each(vectors.valid)('derives "$name" exactly', (vector) => {
    expect(NAME_PATTERN.test(vector.name)).toBe(true)
    expect(deriveNaming(vector.name)).toEqual({
      schemaName: vector.schemaName,
      collectionPath: vector.collectionPath,
      itemPath: vector.itemPath,
    })
    for (const capability of ALL_CAPABILITIES) {
      expect(deriveCapability(vector.name, capability).label).toBe(vector.labels[capability])
    }
  })

  it.each(vectors.invalid.map((name) => [name]))('rejects %j', (name) => {
    expect(NAME_PATTERN.test(name)).toBe(false)
    expect(deriveNaming(name)).toBeNull()
  })
})

describe('deriveCapability verb/path', () => {
  it('maps each capability to its ADR-0010 operation', () => {
    expect(deriveCapability('Product', 'BROWSE')).toEqual({
      label: 'Browse all products',
      method: 'GET',
      path: '/products',
    })
    expect(deriveCapability('Product', 'LOOK_UP')).toMatchObject({
      method: 'GET',
      path: '/products/{id}',
    })
    expect(deriveCapability('Product', 'ADD')).toMatchObject({ method: 'POST', path: '/products' })
    expect(deriveCapability('Product', 'UPDATE')).toMatchObject({
      method: 'PATCH',
      path: '/products/{id}',
    })
    expect(deriveCapability('Product', 'REMOVE')).toMatchObject({
      method: 'DELETE',
      path: '/products/{id}',
    })
  })
})

describe('countDerived (the dialog footer)', () => {
  it('counts operations and distinct paths', () => {
    expect(countDerived(ALL_CAPABILITIES)).toEqual({ operations: 5, paths: 2 })
    expect(countDerived(['BROWSE', 'LOOK_UP', 'ADD', 'REMOVE'])).toEqual({
      operations: 4,
      paths: 2,
    })
    expect(countDerived(['BROWSE', 'ADD'])).toEqual({ operations: 2, paths: 1 })
    expect(countDerived(['LOOK_UP'])).toEqual({ operations: 1, paths: 1 })
    expect(countDerived([])).toEqual({ operations: 0, paths: 0 })
  })
})
