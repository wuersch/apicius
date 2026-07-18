import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, expect, test, vi } from 'vitest'
import { EditorPage } from './EditorPage'

vi.mock('react-oidc-context', () => ({
  useAuth: () => ({ user: { access_token: 'token' } }),
}))

vi.mock('@/api/endpoints/specs/specs', () => ({
  useGetSpec: vi.fn(),
  useAddResource: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useUpdateResourceDescription: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false })),
  useUpdateApiDescription: vi.fn(),
  getGetSpecQueryKey: (specId: string) => [`/api/v1/specs/${specId}`],
  getListSpecsQueryKey: () => ['/api/v1/specs'],
  getGetLastEditedLocationQueryKey: () => ['/api/v1/specs/last-edited'],
}))

import { useGetSpec, useUpdateApiDescription } from '@/api/endpoints/specs/specs'

const updateApiDescriptionSpy = vi.fn().mockResolvedValue({ status: 200 })

function arrange(query: {
  data?: unknown
  error?: unknown
  isPending?: boolean
}) {
  vi.mocked(useGetSpec).mockReturnValue({
    data: query.data,
    error: query.error ?? null,
    isPending: query.isPending ?? false,
  } as never)
  vi.mocked(useUpdateApiDescription).mockReturnValue({
    mutateAsync: updateApiDescriptionSpy,
    isPending: false,
  } as never)
  render(
    <MemoryRouter initialEntries={['/apis/spec-1']}>
      <QueryClientProvider client={new QueryClient()}>
        <Routes>
          <Route path="/apis/:id" element={<EditorPage />} />
        </Routes>
      </QueryClientProvider>
    </MemoryRouter>,
  )
}

const emptySpec = {
  status: 200,
  data: {
    id: 'spec-1',
    title: 'Storefront API',
    description: 'Sell products online.',
    apiVersion: '1.0.0',
    resourceCount: 0,
    operationCount: 0,
    resources: [],
  },
}

beforeEach(() => {
  vi.clearAllMocks()
})

// AC8, empty half: the API's identity, the plain statement, and creation offered.
test('states an empty API plainly and offers resource creation', () => {
  arrange({ data: emptySpec })

  expect(screen.getByRole('heading', { name: 'Storefront API' })).toBeInTheDocument()
  expect(screen.getByText('v1.0.0')).toBeInTheDocument()
  expect(screen.getByText('Sell products online.')).toBeInTheDocument()
  expect(screen.getByRole('heading', { name: 'This API has no resources yet' })).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'New resource' })).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Create your first resource' })).toBeInTheDocument()
})

// FEAT-012's grammar on FEAT-007's member: the note edits in place through the dedicated
// description endpoint — only the description travels, title and version can't be clobbered.
test('edits the API description in place', async () => {
  const user = userEvent.setup()
  arrange({ data: emptySpec })

  await user.click(screen.getByRole('button', { name: 'Edit API description' }))
  const editor = screen.getByRole('textbox', { name: 'API description' })
  expect(editor).toHaveValue('Sell products online.')
  await user.clear(editor)
  await user.type(editor, 'Everything the shop offers.{Enter}')

  expect(updateApiDescriptionSpy).toHaveBeenCalledWith({
    specId: 'spec-1',
    data: { description: 'Everything the shop offers.' },
  })
})

// AC8, populated half: resources with their capabilities in plain language first, the
// derived verb/path as de-emphasized detail (PRIN-002).
test('lists resources with plain-language capabilities and derived detail', () => {
  arrange({
    data: {
      status: 200,
      data: {
        ...emptySpec.data,
        resourceCount: 1,
        operationCount: 2,
        resources: [
          {
            name: 'OrderItem',
            description: 'A line of an order.',
            capabilities: [
              { capability: 'BROWSE', label: 'Browse all order items', method: 'GET', path: '/order-items' },
              { capability: 'REMOVE', label: 'Remove an order item', method: 'DELETE', path: '/order-items/{id}' },
            ],
          },
        ],
      },
    },
  })

  expect(screen.getByRole('heading', { name: 'OrderItem' })).toBeInTheDocument()
  expect(screen.getByText('A line of an order.')).toBeInTheDocument()
  expect(screen.getByText('Browse all order items')).toBeInTheDocument()
  expect(screen.getByText('GET /order-items')).toBeInTheDocument()
  expect(screen.getByText('Remove an order item')).toBeInTheDocument()
  expect(screen.getByText('DELETE /order-items/{id}')).toBeInTheDocument()
  expect(screen.queryByText('This API has no resources yet')).not.toBeInTheDocument()
})

// An unknown id is stated honestly, with the way back.
test('renders the not-found state on a 404', () => {
  arrange({ error: { status: 404 } })

  expect(screen.getByRole('heading', { name: "This API doesn't exist" })).toBeInTheDocument()
  expect(screen.getByRole('link', { name: 'Back to all APIs' })).toBeInTheDocument()
})

// While loading, nothing is asserted about the API — no flash of the empty state.
test('renders nothing while the projection loads', () => {
  arrange({ isPending: true })

  expect(screen.queryByRole('heading')).not.toBeInTheDocument()
})
