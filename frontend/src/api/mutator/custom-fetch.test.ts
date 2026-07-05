import { afterEach, describe, expect, it, vi } from 'vitest'
import { customFetch } from './custom-fetch'

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
})
