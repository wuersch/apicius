import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import { HomePage } from './HomePage'

vi.mock('react-oidc-context', () => ({
  useAuth: () => ({
    user: {
      access_token: 'test-token',
      profile: { given_name: 'Ada', family_name: 'Lovelace' },
    },
  }),
}))

vi.mock('@/api/endpoints/users/users', () => ({
  useGetCurrentUser: vi.fn(),
}))

vi.mock('@/api/endpoints/specs/specs', () => ({
  useListSpecs: vi.fn(),
  useGetLastEditedLocation: vi.fn(),
  useCreateSpec: vi.fn(),
  getListSpecsQueryKey: () => ['/api/v1/specs'],
  getGetLastEditedLocationQueryKey: () => ['/api/v1/specs/last-edited'],
}))

import { useGetCurrentUser } from '@/api/endpoints/users/users'
import { useCreateSpec, useGetLastEditedLocation, useListSpecs } from '@/api/endpoints/specs/specs'

const specs = [
  { id: 'b-2', title: 'billing API', description: 'Invoices.', apiVersion: '2.3', resourceCount: 4, operationCount: 18, updatedAt: '2026-06-24T12:00:00Z' },
  { id: 'b-3', title: 'Fleet API', description: 'Vehicles.', apiVersion: '0.4', resourceCount: 3, operationCount: 12, updatedAt: '2026-06-20T12:00:00Z' },
  { id: 'b-1', title: 'Storefront API', description: 'Sell products online.', apiVersion: '1.0', resourceCount: 5, operationCount: 21, updatedAt: '2026-06-25T12:00:00Z' },
]

function arrange({
  items = specs,
  lastEdited,
}: {
  items?: typeof specs
  lastEdited?: { specId: string; specTitle: string; apiVersion: string; capabilityName?: string; lastEditedAt: string }
} = {}) {
  vi.mocked(useGetCurrentUser).mockReturnValue({
    data: { status: 200, data: { id: 'u-1', displayName: 'Ada Lovelace' }, headers: new Headers() },
  } as never)
  vi.mocked(useListSpecs).mockReturnValue({
    data: { status: 200, data: { items, total: items.length }, headers: new Headers() },
  } as never)
  vi.mocked(useGetLastEditedLocation).mockReturnValue({
    data: lastEdited
      ? { status: 200, data: lastEdited, headers: new Headers() }
      : { status: 204, data: undefined, headers: new Headers() },
  } as never)
  vi.mocked(useCreateSpec).mockReturnValue({ mutate: vi.fn(), isPending: false } as never)

  return render(
    <MemoryRouter>
      <QueryClientProvider client={new QueryClient()}>
        <HomePage />
      </QueryClientProvider>
    </MemoryRouter>,
  )
}

beforeEach(() => {
  // FEAT-002 AC6 needs a deterministic clock: a Saturday morning.
  vi.useFakeTimers({ shouldAdvanceTime: true })
  vi.setSystemTime(new Date(2026, 5, 27, 9, 0, 0))
})

afterEach(() => {
  vi.useRealTimers()
})

// FEAT-002 AC6: greets by name with the time of day and the current date.
test('greets the designer with time of day and date', () => {
  arrange()
  expect(screen.getByRole('heading', { name: 'Good morning, Ada' })).toBeInTheDocument()
  expect(screen.getByText('Saturday, June 27')).toBeInTheDocument()
})

// FEAT-002 AC2: one card per API, in the order the backend sent (alphabetical), not re-sorted.
test('renders one card per API in the order given by the backend', () => {
  arrange()
  const grid = screen.getByRole('region', { name: 'All APIs' })
  const links = within(grid).getAllByRole('link')
  expect(links.map((link) => link.getAttribute('href'))).toEqual(['/apis/b-2', '/apis/b-3', '/apis/b-1'])
  expect(within(grid).getByText('billing API')).toBeInTheDocument()
  expect(within(grid).getByText('3')).toBeInTheDocument()
})

// FEAT-002 AC1: a recorded location renders the jump-back-in card naming the capability.
test('shows the jump-back-in card when a location is recorded', () => {
  arrange({
    lastEdited: {
      specId: 'b-1',
      specTitle: 'Storefront API',
      apiVersion: '1.0',
      capabilityName: 'Add a product',
      lastEditedAt: '2026-06-27T07:00:00Z',
    },
  })
  const section = screen.getByRole('region', { name: 'Jump back in' })
  expect(within(section).getByText('Add a product')).toBeInTheDocument()
  expect(within(section).getByRole('link')).toHaveAttribute('href', '/apis/b-1')
})

// FEAT-002 AC1: no recorded location (204) → no jump-back-in card at all.
test('renders no jump-back-in card without a recorded location', () => {
  arrange()
  expect(screen.queryByRole('region', { name: 'Jump back in' })).not.toBeInTheDocument()
})

// FEAT-002 AC4: no accessible APIs → the empty state with Create and Import entry points.
test('renders the empty state when there are no APIs', () => {
  arrange({ items: [] })
  expect(screen.getByText('Start from scratch')).toBeInTheDocument()
  expect(screen.getByText('Import a spec')).toBeInTheDocument()
  expect(screen.queryByRole('region', { name: 'All APIs' })).not.toBeInTheDocument()
})

// FEAT-003: the New API CTA is live and opens the Create API dialog.
test('the New API button opens the create dialog', async () => {
  const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
  arrange()
  const button = screen.getByRole('button', { name: 'New API' })
  expect(button).toBeEnabled()

  await user.click(button)
  expect(await screen.findByRole('dialog')).toBeInTheDocument()
  expect(screen.getByRole('heading', { name: 'Create a new API' })).toBeInTheDocument()
})
