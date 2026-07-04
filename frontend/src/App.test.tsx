import { render, screen } from '@testing-library/react'
import { expect, test, vi } from 'vitest'
import App from './App'

vi.mock('react-oidc-context', () => ({
  useAuth: () => ({
    user: { profile: { given_name: 'Ada', family_name: 'Lovelace' } },
    signoutRedirect: vi.fn(),
  }),
}))

vi.mock('@/api/endpoints/users/users', () => ({
  useGetCurrentUser: () => ({
    data: {
      status: 200,
      data: { id: '5f0f0aa9-6ba7-4bbd-a2f1-c07a1f1c1a11', displayName: 'Ada Lovelace', email: 'ada@example.com' },
      headers: new Headers(),
    },
  }),
}))

// FEAT-001 AC1: the home greets the authenticated designer by first name (the mockups' convention).
test('greets the authenticated user by first name', () => {
  render(<App />)
  expect(screen.getByRole('heading', { name: 'Welcome, Ada' })).toBeInTheDocument()
})

// FEAT-001 AC6: the app chrome shows the initials avatar.
test('shows the initials avatar in the app chrome', () => {
  render(<App />)
  expect(screen.getByText('AL')).toBeInTheDocument()
})
