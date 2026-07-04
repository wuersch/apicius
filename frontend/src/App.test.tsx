import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, expect, test, vi } from 'vitest'
import App from './App'

vi.mock('react-oidc-context', () => ({
  useAuth: () => ({
    user: {
      access_token: 'test-token',
      profile: { given_name: 'Ada', family_name: 'Lovelace' },
    },
    signoutRedirect: vi.fn(),
  }),
}))

vi.mock('@/api/endpoints/users/users', () => ({
  useGetCurrentUser: () => ({
    data: {
      status: 200,
      data: { id: 'u-1', displayName: 'Ada Lovelace', email: 'ada@example.com' },
      headers: new Headers(),
    },
  }),
}))

vi.mock('@/api/endpoints/specs/specs', () => ({
  useListSpecs: vi.fn(),
  useGetLastEditedLocation: () => ({ data: { status: 204, data: undefined, headers: new Headers() } }),
}))

import { useListSpecs } from '@/api/endpoints/specs/specs'

beforeEach(() => {
  vi.mocked(useListSpecs).mockReturnValue({
    data: {
      status: 200,
      data: {
        items: [
          { id: 'b-1', title: 'Storefront API', description: 'Sell products online.', apiVersion: '1.0', resourceCount: 5, operationCount: 21, updatedAt: '2026-06-25T12:00:00Z' },
        ],
        total: 1,
      },
      headers: new Headers(),
    },
  } as never)
})

// The home route renders the FEAT-002 landing page inside the chrome (initials avatar, FEAT-001 AC6).
test('renders the home page and chrome at the root route', () => {
  render(
    <MemoryRouter>
      <App />
    </MemoryRouter>,
  )
  expect(screen.getByText('Storefront API')).toBeInTheDocument()
  expect(screen.getByText('AL')).toBeInTheDocument()
})

// FEAT-002 AC3: opening a card navigates to the editor route for that API.
test('opening a card enters the editor', async () => {
  const user = userEvent.setup()
  render(
    <MemoryRouter>
      <App />
    </MemoryRouter>,
  )
  await user.click(screen.getByRole('link', { name: /Storefront API/ }))
  expect(screen.getByRole('heading', { name: 'Editor' })).toBeInTheDocument()
  expect(screen.getByText('b-1')).toBeInTheDocument()
})
