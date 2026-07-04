import { render, renderHook, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, expect, test, vi, type Mock } from 'vitest'
import type { ReactNode } from 'react'
import { AuthGate } from './AuthGate'
import App from '@/App'
import { configureApiAuth } from '@/api/mutator/custom-fetch'
import { useGetCurrentUser } from '@/api/endpoints/users/users'

// This is the seam the unit tests mock away: real AuthGate → App under a real
// QueryClientProvider, real generated hook, real customFetch, only fetch + the OIDC
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

beforeEach(() => {
  vi.stubGlobal('fetch', vi.fn())
  signinRedirect.mockClear()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

// The core regression: the very first /users/me must carry the Bearer token, and it
// must happen exactly once — no token-less 401, no redirect bounce.
test('first /users/me carries the Bearer token and fires once', async () => {
  ;(fetch as unknown as Mock).mockResolvedValue(
    new Response(
      JSON.stringify({ id: 'id-1', displayName: 'Ada Lovelace', email: 'ada@example.com' }),
      { status: 200, headers: { 'content-type': 'application/json' } },
    ),
  )

  render(
    <QueryClientProvider client={newClient()}>
      <AuthGate>
        <App />
      </AuthGate>
    </QueryClientProvider>,
  )

  await screen.findByText('Welcome, Ada')

  expect(fetch).toHaveBeenCalledTimes(1)
  const [url, init] = (fetch as unknown as Mock).mock.calls[0]
  expect(String(url)).toContain('/api/v1/users/me')
  expect(new Headers(init.headers).get('authorization')).toBe(`Bearer ${ACCESS_TOKEN}`)
  expect(signinRedirect).not.toHaveBeenCalled()
})

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
