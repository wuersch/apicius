import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
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
  useCreateSpec: () => ({ mutate: vi.fn(), isPending: false }),
  getListSpecsQueryKey: () => ['/api/v1/specs'],
  getGetLastEditedLocationQueryKey: () => ['/api/v1/specs/last-edited'],
}))

import { useListSpecs } from '@/api/endpoints/specs/specs'

function renderApp() {
  return render(
    <MemoryRouter>
      <QueryClientProvider client={new QueryClient()}>
        <App />
      </QueryClientProvider>
    </MemoryRouter>,
  )
}

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
  renderApp()
  expect(screen.getByText('Storefront API')).toBeInTheDocument()
  expect(screen.getByText('AL')).toBeInTheDocument()
})

// FEAT-002 AC3: opening a card navigates to the editor route for that API.
test('opening a card enters the editor', async () => {
  const user = userEvent.setup()
  renderApp()
  await user.click(screen.getByRole('link', { name: /Storefront API/ }))
  expect(screen.getByRole('heading', { name: 'Editor' })).toBeInTheDocument()
  expect(screen.getByText('b-1')).toBeInTheDocument()
})
