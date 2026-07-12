import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, expect, test, vi } from 'vitest'
import { AuthGate } from './AuthGate'

const mockAuth = {
  isAuthenticated: false,
  error: undefined as Error | undefined,
  user: undefined,
  signinRedirect: vi.fn(),
}

const useAutoSignin = vi.fn()

vi.mock('react-oidc-context', () => ({
  useAuth: () => mockAuth,
  useAutoSignin: (options?: unknown) => useAutoSignin(options),
}))

beforeEach(() => {
  mockAuth.isAuthenticated = false
  mockAuth.error = undefined
  mockAuth.signinRedirect.mockClear()
  window.history.replaceState({}, '', '/')
})

// FEAT-001 AC3: no anonymous surface — unauthenticated visitors never see the app.
test('hides the app and auto-signs-in while unauthenticated', () => {
  render(
    <AuthGate>
      <p>secret content</p>
    </AuthGate>,
  )

  expect(screen.queryByText('secret content')).not.toBeInTheDocument()
  expect(useAutoSignin).toHaveBeenCalled()
})

// Reloads and deep links: the pre-auth URL rides the OIDC state channel through the IdP,
// so the redirect lands the user back where they started — not on the root page.
test('auto sign-in carries the current URL as OIDC state', () => {
  window.history.replaceState({}, '', '/apis/abc?tab=shape')

  render(
    <AuthGate>
      <p>secret content</p>
    </AuthGate>,
  )

  expect(useAutoSignin).toHaveBeenCalledWith({
    signinArgs: { state: '/apis/abc?tab=shape' },
  })
})

test('renders the app once authenticated', () => {
  mockAuth.isAuthenticated = true

  render(
    <AuthGate>
      <p>secret content</p>
    </AuthGate>,
  )

  expect(screen.getByText('secret content')).toBeInTheDocument()
})

test('surfaces sign-in errors with a retry action', async () => {
  mockAuth.error = new Error('IdP unreachable')

  render(
    <AuthGate>
      <p>secret content</p>
    </AuthGate>,
  )

  expect(screen.getByRole('alert')).toHaveTextContent(/idp unreachable/i)
  expect(screen.queryByText('secret content')).not.toBeInTheDocument()

  await userEvent.click(screen.getByRole('button', { name: /sign in again/i }))
  expect(mockAuth.signinRedirect).toHaveBeenCalledWith({ state: '/' })
})

// FEAT-001 AC3/UC4: an expired session (failed silent renew leaves error set while
// isAuthenticated is stale-true) must redirect to the IdP, not dead-end.
test('redirects to the IdP when the session ends after sign-in', () => {
  mockAuth.isAuthenticated = true
  mockAuth.error = new Error('silent renew failed')
  window.history.replaceState({}, '', '/apis/abc')

  render(
    <AuthGate>
      <p>secret content</p>
    </AuthGate>,
  )

  expect(screen.queryByText('secret content')).not.toBeInTheDocument()
  expect(screen.getByText(/redirecting to sign-in/i)).toBeInTheDocument()
  // The session-expiry round-trip also returns the user to where they were.
  expect(mockAuth.signinRedirect).toHaveBeenCalledWith({ state: '/apis/abc' })
})
