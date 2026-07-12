import { afterEach, describe, expect, it, vi } from 'vitest'
import { customFetch, retryUnlessClientError } from './custom-fetch'

function stubFetch(response: Response) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response))
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('customFetch body parsing', () => {
  it('parses application/json bodies as JSON', async () => {
    stubFetch(
      new Response(JSON.stringify({ items: [] }), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      }),
    )

    const envelope = await customFetch<{ data: unknown; status: number }>('/api/v1/specs', {})
    expect(envelope.data).toEqual({ items: [] })
  })

  // RFC 9457 responses are structured suffixes (+json), not the literal application/json
  // type — the violations contract (FEAT-003 AC5) is unreadable if this parses as text.
  it('parses application/problem+json bodies as JSON', async () => {
    const problem = {
      title: 'Validation failed',
      status: 400,
      violations: [{ field: 'title', message: 'must not be blank' }],
    }
    stubFetch(
      new Response(JSON.stringify(problem), {
        status: 400,
        headers: { 'content-type': 'application/problem+json' },
      }),
    )

    const thrown = await customFetch('/api/v1/specs', { method: 'POST' }).then(
      () => {
        throw new Error('expected customFetch to throw on 400')
      },
      (envelope: { data: unknown; status: number }) => envelope,
    )
    expect(thrown.status).toBe(400)
    expect(thrown.data).toEqual(problem)
  })

  // FEAT-008 AC1: an attachment is a file for the user, never structured data. Parsing the
  // JSON export and re-serializing it would reorder integer-like keys ("404" before "200"),
  // so the downloaded file must pass through as the exact text the server sent.
  it('returns attachment bodies as raw text even when they are JSON', async () => {
    const document = '{"paths":{"/b":{},"/a":{}},"responses":{"404":{},"200":{}}}'
    stubFetch(
      new Response(document, {
        status: 200,
        headers: {
          'content-type': 'application/json',
          'content-disposition': 'attachment; filename="Payments API.json"',
        },
      }),
    )

    const envelope = await customFetch<{ data: unknown }>(
      '/api/v1/specs/x/document?format=json',
      {},
    )
    expect(envelope.data).toBe(document)
  })

  it('returns yaml attachment bodies as text', async () => {
    const document = 'openapi: "3.1.1"\n'
    stubFetch(
      new Response(document, {
        status: 200,
        headers: {
          'content-type': 'application/yaml',
          'content-disposition': 'attachment; filename="Payments API.yaml"',
        },
      }),
    )

    const envelope = await customFetch<{ data: unknown }>(
      '/api/v1/specs/x/document?format=yaml',
      {},
    )
    expect(envelope.data).toBe(document)
  })
})

describe('retryUnlessClientError', () => {
  // A 4xx is deterministic — the request is wrong (or the thing is gone); asking again
  // only delays the page's honest answer (e.g. ~7s of blank before "doesn't exist").
  it('never retries client errors', () => {
    for (const status of [400, 401, 403, 404, 409]) {
      expect(retryUnlessClientError(0, { status })).toBe(false)
    }
  })

  it('retries server errors and network failures up to three times', () => {
    expect(retryUnlessClientError(0, { status: 500 })).toBe(true)
    expect(retryUnlessClientError(2, { status: 503 })).toBe(true)
    expect(retryUnlessClientError(3, { status: 500 })).toBe(false)
    // fetch rejections (network down) carry no status envelope.
    expect(retryUnlessClientError(0, new TypeError('Failed to fetch'))).toBe(true)
  })
})
