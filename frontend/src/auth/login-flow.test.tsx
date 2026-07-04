import { render, renderHook, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, expect, test, vi, type Mock } from 'vitest'
import type { ReactNode } from 'react'
import { AuthGate } from './AuthGate'
import App from '@/App'
import { configureApiAuth } from '@/api/mutator/custom-fetch'
import { useGetCurrentUser } from '@/api/endpoints/users/users'

// This is the seam the unit tests mock away: real AuthGate → App under a real
// QueryClientProvider, real generated hooks, real customFetch, only fetch + the OIDC
// hooks stubbed. It reproduces the token-wiring race a production build would hit.

const ACCESS_TOKEN = 'test-access-token'
const signinRedirect = vi.fn()

vi.mock('react-oidc-context', () => ({
  useAuth: () => ({
    isAuthenticated: true,
    error: undefined,
    user: { access_token: ACCESS_TOKEN, profile: { given_name: 'Ada', family_name: 'Lovelace' } },
    signinRedirect,
    signoutRedirect: vi.fn(),
  }),
  useAutoSignin: () => {},
}))

function newClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false } } })
}

// The home fires three queries; the stub routes each URL like the real backend would.
function stubBackend() {
  ;(fetch as unknown as Mock).mockImplementation((url: RequestInfo | URL) => {
    const path = String(url)
    if (path.includes('/api/v1/users/me')) {
      return Promise.resolve(json({ id: 'id-1', displayName: 'Ada Lovelace', email: 'ada@example.com' }))
    }
    if (path.includes('/api/v1/specs/last-edited')) {
      return Promise.resolve(new Response(null, { status: 204 }))
    }
    if (path.includes('/api/v1/specs')) {
      return Promise.resolve(json({ items: [], total: 0 }))
    }
    return Promise.resolve(new Response('not found', { status: 404 }))
  })
}

function json(body: unknown) {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'content-type': 'application/json' },
  })
}

beforeEach(() => {
  vi.stubGlobal('fetch', vi.fn())
  signinRedirect.mockClear()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

// The core regression: every first-render request must carry the Bearer token — the
// accessor is wired during AuthGate's render, before any child query fires. No
// token-less 401, no redirect bounce.
test('first-render requests carry the Bearer token', async () => {
  stubBackend()

  render(
    <QueryClientProvider client={newClient()}>
      <AuthGate>
        <MemoryRouter>
          <App />
        </MemoryRouter>
      </AuthGate>
    </QueryClientProvider>,
  )

  // FEAT-002's greeting proves /users/me resolved (name comes from the token profile).
  await screen.findByRole('heading', { name: /Ada/ })
  await waitFor(() => expect(fetchCalls('/api/v1/specs')).toHaveLength(1))

  expect(fetchCalls('/api/v1/users/me')).toHaveLength(1)
  for (const [, init] of (fetch as unknown as Mock).mock.calls) {
    expect(new Headers((init as RequestInit).headers).get('authorization')).toBe(`Bearer ${ACCESS_TOKEN}`)
  }
  expect(signinRedirect).not.toHaveBeenCalled()
})

function fetchCalls(path: string) {
  return (fetch as unknown as Mock).mock.calls.filter(([url]) => {
    const called = String(url)
    // exact-path match so "/specs" doesn't also count "/specs/last-edited"
    return called.endsWith(path) || called.includes(`${path}?`)
  })
}

// customFetch must throw on non-2xx so TanStack Query treats it as an error
// (previously it returned the 401 as a resolved value → isError never set).
test('a 401 surfaces as a query error', async () => {
  configureApiAuth({ getAccessToken: () => ACCESS_TOKEN, onUnauthorized: () => {} })
  ;(fetch as unknown as Mock).mockResolvedValue(new Response('', { status: 401 }))

  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={newClient()}>{children}</QueryClientProvider>
  )
  const { result } = renderHook(() => useGetCurrentUser(), { wrapper })

  await waitFor(() => expect(result.current.isError).toBe(true))
})
