import { render, screen } from '@testing-library/react'
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

test('surfaces sign-in errors', () => {
  mockAuth.error = new Error('IdP unreachable')

  render(
    <AuthGate>
      <p>secret content</p>
    </AuthGate>,
  )

  expect(screen.getByRole('alert')).toHaveTextContent(/idp unreachable/i)
  expect(screen.queryByText('secret content')).not.toBeInTheDocument()
})
