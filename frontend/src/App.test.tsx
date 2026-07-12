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
  useUpdateSpecDetails: () => ({ mutate: vi.fn(), isPending: false }),
  useDuplicateSpec: () => ({ mutate: vi.fn(), isPending: false }),
  useDeleteSpec: () => ({ mutate: vi.fn(), isPending: false }),
  useAddResource: () => ({ mutate: vi.fn(), isPending: false }),
  useGetSpec: () => ({
    data: {
      status: 200,
      data: { id: 'b-1', title: 'Storefront API', apiVersion: '1.0', resourceCount: 0, operationCount: 0, resources: [] },
      headers: new Headers(),
    },
    error: null,
    isPending: false,
  }),
  getListSpecsQueryKey: () => ['/api/v1/specs'],
  getGetSpecQueryKey: (specId: string) => [`/api/v1/specs/${specId}`],
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

// FEAT-002 AC3: opening a card navigates to the editor route for that API — since FEAT-005,
// the real editor (AC8's minimal honest display), no longer a placeholder.
test('opening a card enters the editor', async () => {
  const user = userEvent.setup()
  renderApp()
  await user.click(screen.getByRole('link', { name: /Storefront API/ }))
  expect(screen.getByRole('heading', { name: 'Storefront API' })).toBeInTheDocument()
  expect(screen.getByRole('heading', { name: 'This API has no resources yet' })).toBeInTheDocument()
})
