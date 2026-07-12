import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { expect, test, vi } from 'vitest'
import { ApiCard } from './ApiCard'

vi.mock('@/api/endpoints/specs/specs', () => ({
  useUpdateSpecDetails: () => ({ mutate: vi.fn(), isPending: false }),
  useDuplicateSpec: () => ({ mutate: vi.fn(), isPending: false }),
  useDeleteSpec: () => ({ mutate: vi.fn(), isPending: false }),
  getListSpecsQueryKey: () => ['/api/v1/specs'],
  getGetLastEditedLocationQueryKey: () => ['/api/v1/specs/last-edited'],
}))

const now = new Date('2026-06-27T12:00:00Z')
const spec = {
  id: 'b1000000-0000-4000-8000-000000000001',
  title: 'Storefront API',
  description: 'Sell products online.',
  apiVersion: '1.0',
  specVersion: '3.1.1',
  resourceCount: 5,
  operationCount: 21,
  updatedAt: '2026-06-25T12:00:00Z',
}

function renderCard() {
  return render(
    <MemoryRouter>
      <QueryClientProvider client={new QueryClient()}>
        <ApiCard spec={spec} now={now} />
      </QueryClientProvider>
    </MemoryRouter>,
  )
}

// FEAT-002 AC2: monogram, title, description, counts in domain vocabulary, version, relative age.
test('renders the card fields in domain vocabulary', () => {
  renderCard()
  expect(screen.getByText('S')).toBeInTheDocument()
  expect(screen.getByText('Storefront API')).toBeInTheDocument()
  expect(screen.getByText('Sell products online.')).toBeInTheDocument()
  expect(screen.getByText('5 resources · 21 ops')).toBeInTheDocument()
  expect(screen.getByText('v1.0')).toBeInTheDocument()
  expect(screen.getByText('2d ago')).toBeInTheDocument()
})

// FEAT-002 AC3: the whole card is a link into the editor for that API.
test('links to the editor route for the API', () => {
  renderCard()
  expect(screen.getByRole('link')).toHaveAttribute('href', `/apis/${spec.id}`)
})

// FEAT-007: the ⋯ actions live on the card but outside the anchor — a button nested in a
// link is invalid HTML and would navigate instead of opening the menu.
test('offers the card actions outside the link', () => {
  renderCard()
  const trigger = screen.getByRole('button', { name: 'Actions for Storefront API' })
  expect(screen.getByRole('link')).not.toContainElement(trigger)
})

// FEAT-007 scope: exactly the three management actions — the Download items are FEAT-008.
test('the overflow menu shows the three management actions and nothing else', async () => {
  const user = userEvent.setup()
  renderCard()

  await user.click(screen.getByRole('button', { name: 'Actions for Storefront API' }))

  const items = await screen.findAllByRole('menuitem')
  expect(items.map((item) => item.textContent)).toEqual(['Edit details…', 'Duplicate', 'Delete…'])
  expect(screen.queryByText(/Download/)).not.toBeInTheDocument()
})
