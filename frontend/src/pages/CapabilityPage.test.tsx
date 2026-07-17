import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, expect, test, vi } from 'vitest'
import type { CapabilityContractResponse } from '@/api/model'
import { CapabilityPage } from './CapabilityPage'

vi.mock('react-oidc-context', () => ({
  useAuth: () => ({ user: { access_token: 'token' } }),
}))

vi.mock('@/api/endpoints/specs/specs', () => ({
  useGetSpec: vi.fn(),
  useGetCapabilityContract: vi.fn(),
  useAdoptStandardErrors: vi.fn(),
  useRemoveStandardErrors: vi.fn(),
  getGetSpecQueryKey: (specId: string) => [`/api/v1/specs/${specId}`],
  getListSpecsQueryKey: () => ['/api/v1/specs'],
  getGetLastEditedLocationQueryKey: () => ['/api/v1/specs/last-edited'],
  getGetCapabilityContractQueryKey: (specId: string, schemaName: string, capability: string) => [
    `/api/v1/specs/${specId}/resources/${schemaName}/capabilities/${capability}`,
  ],
}))

import {
  useAdoptStandardErrors,
  useGetCapabilityContract,
  useGetSpec,
  useRemoveStandardErrors,
} from '@/api/endpoints/specs/specs'

const spec = {
  status: 200,
  data: {
    id: 'spec-1',
    title: 'Storefront API',
    apiVersion: '1.0.0',
    resources: [
      {
        name: 'Product',
        capabilities: [
          { capability: 'BROWSE', label: 'Browse all products', method: 'GET', path: '/products' },
          { capability: 'ADD', label: 'Add a product', method: 'POST', path: '/products' },
          {
            capability: 'LOOK_UP',
            label: 'Look up one product',
            method: 'GET',
            path: '/products/{id}',
          },
        ],
        fields: [
          { name: 'id', coreType: 'TEXT', list: false, required: true, visibility: 'AUTO' },
        ],
      },
    ],
  },
}

const lookUpContract: CapabilityContractResponse = {
  capability: {
    capability: 'LOOK_UP',
    label: 'Look up one product',
    method: 'GET',
    path: '/products/{id}',
  },
  singularNoun: 'product',
  headers: [{ name: 'Accept', value: 'application/json', derived: true }],
  answers: {
    successStatus: '200',
    successDescription: 'The product.',
    failures: [
      { status: '400', present: true },
      { status: '401', present: true },
      { status: '404', present: true },
      { status: '429', present: true },
      { status: '500', present: true },
    ],
  },
}

function arrange(
  contract: CapabilityContractResponse | { notFound: true },
  route = '/apis/spec-1/resources/Product/capabilities/LOOK_UP',
  adoptMutate = vi.fn(),
  removeMutate = vi.fn(),
) {
  vi.mocked(useGetSpec).mockReturnValue({
    data: spec,
    error: null,
    isPending: false,
  } as never)
  vi.mocked(useGetCapabilityContract).mockReturnValue(
    'notFound' in contract
      ? ({ data: undefined, error: { status: 404 }, isPending: false } as never)
      : ({ data: { status: 200, data: contract }, error: null, isPending: false } as never),
  )
  vi.mocked(useAdoptStandardErrors).mockReturnValue({
    mutate: adoptMutate,
    isPending: false,
    isError: false,
  } as never)
  vi.mocked(useRemoveStandardErrors).mockReturnValue({
    mutate: removeMutate,
    isPending: false,
    isError: false,
  } as never)
  render(
    <MemoryRouter initialEntries={[route]}>
      <QueryClientProvider client={new QueryClient()}>
        <Routes>
          <Route
            path="/apis/:id/resources/:schemaName/capabilities/:capability"
            element={<CapabilityPage />}
          />
        </Routes>
      </QueryClientProvider>
    </MemoryRouter>,
  )
}

beforeEach(() => {
  vi.clearAllMocks()
})

// AC1: identity (plain-language label first, derived detail after), then the facets in the
// stable order — Headers and Answers here; Look up takes no body (AC2: Request absent).
test('renders the facets in stable order with derived detail de-emphasized', () => {
  arrange(lookUpContract)

  expect(screen.getByRole('heading', { name: 'Look up one product' })).toBeInTheDocument()
  expect(screen.getByText('GET /products/{id}')).toBeInTheDocument()
  expect(screen.queryByRole('region', { name: 'Request' })).not.toBeInTheDocument()

  const headers = screen.getByRole('region', { name: 'Headers' })
  expect(within(headers).getByText('Accept')).toBeInTheDocument()
  expect(within(headers).getByText('application/json')).toBeInTheDocument()
  expect(within(headers).getByText('derived')).toBeInTheDocument()

  const answers = screen.getByRole('region', { name: 'Answers' })
  expect(within(answers).getByText('200')).toBeInTheDocument()
  expect(within(answers).getByText('The product.')).toBeInTheDocument()
  expect(within(answers).getByText('no product with this id')).toBeInTheDocument()
  expect(within(answers).getByRole('switch', { name: 'Standard errors' })).toBeChecked()
  expect(within(answers).getByText(/RFC 9457/)).toBeInTheDocument()
  expect(
    within(answers).queryByText(/without a shared shape/),
  ).not.toBeInTheDocument()
})

// The rail: the noun's "can do" list travels with the capability, current one marked.
test('lists sibling capabilities in the rail with the open one marked', () => {
  arrange(lookUpContract)

  const rail = screen.getByRole('navigation', { name: 'Capabilities of Product' })
  const active = within(rail).getByRole('link', { name: /Look up one product/ })
  expect(active).toHaveAttribute('aria-current', 'page')
  expect(within(rail).getByRole('link', { name: /Browse all products/ })).toHaveAttribute(
    'href',
    '/apis/spec-1/resources/Product/capabilities/BROWSE',
  )
})

// AC5, Add: the Request facet derives the shape — the identity row reading server-assigned.
test('derives the Request facet for Add from the shape', () => {
  arrange(
    {
      capability: { capability: 'ADD', label: 'Add a product', method: 'POST', path: '/products' },
      singularNoun: 'product',
      request: {
        mergePatch: false,
        fields: [
          { name: 'id', coreType: 'TEXT', list: false, required: true, visibility: 'AUTO' },
        ],
      },
      headers: [{ name: 'Accept', value: 'application/json', derived: true }],
      answers: { successStatus: '201', successDescription: 'The created product.', failures: [] },
    },
    '/apis/spec-1/resources/Product/capabilities/ADD',
  )

  const request = screen.getByRole('region', { name: 'Request' })
  expect(within(request).getByText(/identifier is assigned by the server/)).toBeInTheDocument()
  expect(within(request).getByText('identifier — the server sets it')).toBeInTheDocument()
})

// AC5, Update: merge-patch semantics stated, nothing enumerated.
test('states merge-patch semantics for Update', () => {
  arrange(
    {
      capability: {
        capability: 'UPDATE',
        label: 'Update a product',
        method: 'PATCH',
        path: '/products/{id}',
      },
      singularNoun: 'product',
      request: { mergePatch: true, fields: [] },
      headers: [{ name: 'Accept', value: 'application/json', derived: true }],
      answers: { successStatus: '200', successDescription: 'The updated product.', failures: [] },
    },
    '/apis/spec-1/resources/Product/capabilities/UPDATE',
  )

  const request = screen.getByRole('region', { name: 'Request' })
  expect(within(request).getByText(/Send only the fields you change/)).toBeInTheDocument()
  expect(within(request).queryByText('identifier — the server sets it')).not.toBeInTheDocument()
})

// UC3/UC5/AC4: absent standard answers rest as dashed chips with the consequence stated and
// the toggle off; switching on fires the adopt mutation.
test('shows absent answers off and switches them on via adopt', async () => {
  const user = userEvent.setup()
  const adoptMutate = vi.fn()
  arrange(
    {
      ...lookUpContract,
      answers: {
        successStatus: '200',
        successDescription: 'The product.',
        failures: [
          { status: '400', present: false },
          { status: '401', present: false },
          { status: '404', present: false },
          { status: '429', present: false },
          { status: '500', present: false },
        ],
      },
    },
    '/apis/spec-1/resources/Product/capabilities/LOOK_UP',
    adoptMutate,
  )

  const toggle = screen.getByRole('switch', { name: 'Standard errors' })
  expect(toggle).not.toBeChecked()
  expect(screen.getByText(/without a shared shape/)).toBeInTheDocument()
  // Off removes the guarantee's badge entirely — never dimmed.
  expect(screen.queryByText(/RFC 9457/)).not.toBeInTheDocument()
  await user.click(toggle)

  expect(adoptMutate).toHaveBeenCalledWith(
    { specId: 'spec-1', schemaName: 'Product', capability: 'LOOK_UP' },
    expect.anything(),
  )
})

// UC5: switching present answers off fires the remove mutation — plain, no confirm.
test('switches present answers off via remove', async () => {
  const user = userEvent.setup()
  const adoptMutate = vi.fn()
  const removeMutate = vi.fn()
  arrange(lookUpContract, '/apis/spec-1/resources/Product/capabilities/LOOK_UP', adoptMutate, removeMutate)

  await user.click(screen.getByRole('switch', { name: 'Standard errors' }))

  expect(removeMutate).toHaveBeenCalledWith(
    { specId: 'spec-1', schemaName: 'Product', capability: 'LOOK_UP' },
    expect.anything(),
  )
  expect(adoptMutate).not.toHaveBeenCalled()
})

// An unknown capability is stated honestly, with the way back to the API.
test('renders the not-found state on a 404', () => {
  arrange({ notFound: true })

  expect(
    screen.getByRole('heading', { name: "This capability doesn't exist" }),
  ).toBeInTheDocument()
  expect(screen.getByRole('link', { name: 'Back to the API' })).toHaveAttribute(
    'href',
    '/apis/spec-1',
  )
})
