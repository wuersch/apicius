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
  useAutoSignin: () => useAutoSignin(),
}))

beforeEach(() => {
  mockAuth.isAuthenticated = false
  mockAuth.error = undefined
  mockAuth.signinRedirect.mockClear()
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
  expect(mockAuth.signinRedirect).toHaveBeenCalled()
})

// FEAT-001 AC3/UC4: an expired session (failed silent renew leaves error set while
// isAuthenticated is stale-true) must redirect to the IdP, not dead-end.
test('redirects to the IdP when the session ends after sign-in', () => {
  mockAuth.isAuthenticated = true
  mockAuth.error = new Error('silent renew failed')

  render(
    <AuthGate>
      <p>secret content</p>
    </AuthGate>,
  )

  expect(screen.queryByText('secret content')).not.toBeInTheDocument()
  expect(screen.getByText(/redirecting to sign-in/i)).toBeInTheDocument()
  expect(mockAuth.signinRedirect).toHaveBeenCalled()
})
