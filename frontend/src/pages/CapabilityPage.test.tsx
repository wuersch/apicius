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
  useEnablePaging: vi.fn(),
  useDisablePaging: vi.fn(),
  useAddQueryParameter: vi.fn(),
  useUpdateQueryParameter: vi.fn(),
  useRemoveQueryParameter: vi.fn(),
  useAddRequestHeader: vi.fn(),
  useUpdateRequestHeader: vi.fn(),
  useRemoveRequestHeader: vi.fn(),
  useAddResponseHeader: vi.fn(),
  useUpdateResponseHeader: vi.fn(),
  useRemoveResponseHeader: vi.fn(),
  getGetSpecQueryKey: (specId: string) => [`/api/v1/specs/${specId}`],
  getListSpecsQueryKey: () => ['/api/v1/specs'],
  getGetLastEditedLocationQueryKey: () => ['/api/v1/specs/last-edited'],
  getGetCapabilityContractQueryKey: (specId: string, schemaName: string, capability: string) => [
    `/api/v1/specs/${specId}/resources/${schemaName}/capabilities/${capability}`,
  ],
}))

import {
  useAddQueryParameter,
  useAddRequestHeader,
  useAddResponseHeader,
  useAdoptStandardErrors,
  useDisablePaging,
  useEnablePaging,
  useGetCapabilityContract,
  useGetSpec,
  useRemoveQueryParameter,
  useRemoveRequestHeader,
  useRemoveResponseHeader,
  useRemoveStandardErrors,
  useUpdateQueryParameter,
  useUpdateRequestHeader,
  useUpdateResponseHeader,
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
  queryParameters: [],
  headers: {
    derived: [{ name: 'Accept', value: 'application/json', derived: true }],
    authored: [],
  },
  answers: {
    successStatus: '200',
    successDescription: 'The product.',
    successHeaders: [],
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
  enableMutate = vi.fn(),
  disableMutate = vi.fn(),
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
  vi.mocked(useEnablePaging).mockReturnValue({
    mutate: enableMutate,
    isPending: false,
    isError: false,
  } as never)
  vi.mocked(useDisablePaging).mockReturnValue({
    mutate: disableMutate,
    isPending: false,
    isError: false,
  } as never)
  // One spy per declaration mutation, so a test can assert the location-specific endpoint
  // fired — every facet renders all nine hooks (rules of hooks), only the addressed one may.
  const declarations = {
    addQueryParameter: vi.fn(),
    updateQueryParameter: vi.fn(),
    removeQueryParameter: vi.fn(),
    addRequestHeader: vi.fn(),
    updateRequestHeader: vi.fn(),
    removeRequestHeader: vi.fn(),
    addResponseHeader: vi.fn(),
    updateResponseHeader: vi.fn(),
    removeResponseHeader: vi.fn(),
  }
  const hooks: [() => unknown, ReturnType<typeof vi.fn>][] = [
    [useAddQueryParameter, declarations.addQueryParameter],
    [useUpdateQueryParameter, declarations.updateQueryParameter],
    [useRemoveQueryParameter, declarations.removeQueryParameter],
    [useAddRequestHeader, declarations.addRequestHeader],
    [useUpdateRequestHeader, declarations.updateRequestHeader],
    [useRemoveRequestHeader, declarations.removeRequestHeader],
    [useAddResponseHeader, declarations.addResponseHeader],
    [useUpdateResponseHeader, declarations.updateResponseHeader],
    [useRemoveResponseHeader, declarations.removeResponseHeader],
  ]
  for (const [hook, mutate] of hooks) {
    vi.mocked(hook).mockReturnValue({ mutate, isPending: false, isError: false } as never)
  }
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
  return declarations
}

beforeEach(() => {
  vi.clearAllMocks()
})

// AC1: identity (plain-language label first, derived detail after), then the facets in the
// stable order — Headers, Answers, and the split-out Errors card here; Look up takes no
// body (AC2: Request absent).
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

  // The v10 split: Errors is its own card — the failure chips and the toggle live there.
  const errors = screen.getByRole('region', { name: 'Errors' })
  expect(within(errors).getByText('no product with this id')).toBeInTheDocument()
  expect(within(errors).getByRole('switch', { name: 'Standard errors' })).toBeChecked()
  expect(within(errors).getByText(/RFC 9457/)).toBeInTheDocument()
  expect(within(errors).queryByText(/without a shared shape/)).not.toBeInTheDocument()
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
      queryParameters: [],
      headers: {
        derived: [{ name: 'Accept', value: 'application/json', derived: true }],
        authored: [],
      },
      answers: {
        successStatus: '201',
        successDescription: 'The created product.',
        successHeaders: [],
        failures: [],
      },
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
      queryParameters: [],
      headers: {
        derived: [{ name: 'Accept', value: 'application/json', derived: true }],
        authored: [],
      },
      answers: {
        successStatus: '200',
        successDescription: 'The updated product.',
        successHeaders: [],
        failures: [],
      },
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

const browseContract: CapabilityContractResponse = {
  capability: { capability: 'BROWSE', label: 'Browse all products', method: 'GET', path: '/products' },
  singularNoun: 'product',
  queryParameters: [],
  paging: { on: true, conflicts: [] },
  headers: {
    derived: [{ name: 'Accept', value: 'application/json', derived: true }],
    authored: [],
  },
  answers: {
    successStatus: '200',
    successDescription: 'A paged list of products',
    successHeaders: [],
    failures: [
      { status: '400', present: true },
      { status: '401', present: true },
      { status: '422', present: true },
      { status: '429', present: true },
      { status: '500', present: true },
    ],
  },
}

const browseRoute = '/apis/spec-1/resources/Product/capabilities/BROWSE'

// FEAT-010 UC1: paging is stated as built-in behavior in plain language — the wrapped-list
// answer with the pagination member, the toggle on.
test('presents paging as built in on a paged list capability', () => {
  arrange(browseContract, browseRoute)

  const paging = screen.getByRole('region', { name: 'Paging' })
  expect(within(paging).getByText('Built in')).toBeInTheDocument()
  expect(within(paging).getByRole('switch', { name: 'Paging' })).toBeChecked()
  expect(within(paging).getByText(/Results come back/)).toBeInTheDocument()
  expect(within(paging).getByText(/pagination:/)).toBeInTheDocument()
  expect(within(paging).getByText(/…the products…/)).toBeInTheDocument()
})

// AC2 (FEAT-009) reused: paging doesn't apply to a non-list capability — the facet is
// absent, never an empty card, and no toggle exists to misfire.
test('omits the Paging facet where paging does not apply', () => {
  arrange(lookUpContract)

  expect(screen.queryByRole('region', { name: 'Paging' })).not.toBeInTheDocument()
})

// UC2: switching off fires the disable mutation — plain, no confirm.
test('switches paging off via disable', async () => {
  const user = userEvent.setup()
  const enableMutate = vi.fn()
  const disableMutate = vi.fn()
  arrange(browseContract, browseRoute, vi.fn(), vi.fn(), enableMutate, disableMutate)

  await user.click(screen.getByRole('switch', { name: 'Paging' }))

  expect(disableMutate).toHaveBeenCalledWith(
    { specId: 'spec-1', schemaName: 'Product', capability: 'BROWSE' },
    expect.anything(),
  )
  expect(enableMutate).not.toHaveBeenCalled()
})

// UC3/UC4: the opted-out (or pre-FEAT-010) card is dashed/neutral with the consequence
// stated and no "Built in" badge; switching on is the one-action adopt.
test('shows paging off with the consequence and switches on via enable', async () => {
  const user = userEvent.setup()
  const enableMutate = vi.fn()
  arrange(
    { ...browseContract, paging: { on: false, conflicts: [] } },
    browseRoute,
    vi.fn(),
    vi.fn(),
    enableMutate,
  )

  const paging = screen.getByRole('region', { name: 'Paging' })
  const toggle = within(paging).getByRole('switch', { name: 'Paging' })
  expect(toggle).not.toBeChecked()
  expect(within(paging).getByText(/whole list in one response/)).toBeInTheDocument()
  // Off removes the built-in badge entirely — never dimmed.
  expect(within(paging).queryByText('Built in')).not.toBeInTheDocument()
  await user.click(toggle)

  expect(enableMutate).toHaveBeenCalledWith(
    { specId: 'spec-1', schemaName: 'Product', capability: 'BROWSE' },
    expect.anything(),
  )
})

// UC5: a designer-authored page/limit parameter blocks enabling — the switch locks and the
// copy names the conflict.
test('locks the paging toggle while an authored parameter claims the name', () => {
  arrange({ ...browseContract, paging: { on: false, conflicts: ['page'] } }, browseRoute)

  const paging = screen.getByRole('region', { name: 'Paging' })
  expect(within(paging).getByRole('switch', { name: 'Paging' })).toBeDisabled()
  expect(within(paging).getByText(/Can't switch on/)).toBeInTheDocument()
  expect(within(paging).getByText('page')).toBeInTheDocument()
})

// ---- FEAT-011: query parameters & headers ----

// The empty Filters card recedes (dashed, one quiet resting line — the paging-off idiom)
// rather than disappearing: no card would mean no way to add the first filter.
test('recedes the empty Filters card and opens the editor from its heading', async () => {
  const user = userEvent.setup()
  arrange(lookUpContract)

  const filters = screen.getByRole('region', { name: 'Filters' })
  expect(within(filters).getByText('none — the list comes as-is')).toBeInTheDocument()

  await user.click(within(filters).getByRole('button', { name: 'Add a filter' }))

  expect(screen.getByRole('textbox', { name: 'Name' })).toBeInTheDocument()
})

// UC1: a filter saves through the query-parameter endpoint as the plain-language draft —
// the derived name previewed live (never typed), serialization the server's business.
test('adds a query parameter through the inline editor', async () => {
  const user = userEvent.setup()
  const declarations = arrange(browseContract, browseRoute)

  await user.click(screen.getByRole('button', { name: 'Add a filter' }))
  await user.type(screen.getByRole('textbox', { name: 'Name' }), 'Price max')
  expect(screen.getByText('parameter priceMax')).toBeInTheDocument()
  await user.click(screen.getByRole('button', { name: 'Save' }))

  expect(declarations.addQueryParameter).toHaveBeenCalledWith(
    {
      specId: 'spec-1',
      schemaName: 'Product',
      capability: 'BROWSE',
      data: expect.objectContaining({ name: 'Price max', coreType: 'TEXT' }),
    },
    expect.anything(),
  )
})

// AC7: picking "one of …" swaps the refinement slot for the comma-separated values input;
// duplicates block inline, a valid set travels as the value list.
test('edits a one-of filter with the comma-separated values input', async () => {
  const user = userEvent.setup()
  const declarations = arrange(browseContract, browseRoute)

  await user.click(screen.getByRole('button', { name: 'Add a filter' }))
  await user.type(screen.getByRole('textbox', { name: 'Name' }), 'Status')
  await user.click(screen.getByLabelText('Kind'))
  await user.click(await screen.findByRole('menuitemradio', { name: 'one of …' }))
  const values = screen.getByRole('textbox', { name: 'One of values' })
  await user.type(values, 'available, available')

  expect(screen.getByText(/must be distinct/)).toBeInTheDocument()
  await user.click(screen.getByRole('button', { name: 'Save' }))
  expect(declarations.addQueryParameter).not.toHaveBeenCalled()

  await user.clear(values)
  await user.type(values, 'available, pending, sold')
  await user.click(screen.getByRole('button', { name: 'Save' }))

  expect(declarations.addQueryParameter).toHaveBeenCalledWith(
    expect.objectContaining({
      data: expect.objectContaining({
        oneOfValues: ['available', 'pending', 'sold'],
        coreType: undefined,
      }),
    }),
    expect.anything(),
  )
})

// FEAT-010's pair, accounted for: while paging is on, the query string genuinely carries
// page & limit, so the Filters card lists them — locked and badged, never editable.
test('lists the paging pair as built-in locked rows while paging is on', () => {
  arrange(browseContract, browseRoute)

  const filters = screen.getByRole('region', { name: 'Filters' })
  expect(within(filters).getByText('page')).toBeInTheDocument()
  expect(within(filters).getByText('limit')).toBeInTheDocument()
  expect(within(filters).getAllByText('Built in · paging')).toHaveLength(2)
  expect(within(filters).queryByRole('button', { name: 'Edit page' })).not.toBeInTheDocument()
})

// With paging off the pair is gone from the document — nothing to account for, and an
// otherwise-empty card recedes again.
test('omits the paging pair while paging is off', () => {
  arrange({ ...browseContract, paging: { on: false, conflicts: [] } }, browseRoute)

  const filters = screen.getByRole('region', { name: 'Filters' })
  expect(within(filters).queryByText('page')).not.toBeInTheDocument()
  expect(within(filters).getByText('none — the list comes as-is')).toBeInTheDocument()
})

// UC5: while the capability pages, page/limit are paging's constructs — excluded from the
// filter projection, so the collision set can't catch them; the editor blocks them inline.
test('blocks a paging-owned filter name inline while paging is on', async () => {
  const user = userEvent.setup()
  const declarations = arrange(browseContract, browseRoute)

  await user.click(screen.getByRole('button', { name: 'Add a filter' }))
  await user.type(screen.getByRole('textbox', { name: 'Name' }), 'Page')

  expect(screen.getByText(/Paging owns page and limit/)).toBeInTheDocument()
  await user.click(screen.getByRole('button', { name: 'Save' }))
  expect(declarations.addQueryParameter).not.toHaveBeenCalled()
})

// UC5: a reserved header name blocks inline — nothing sent, nothing persisted.
test('blocks a reserved header name inline', async () => {
  const user = userEvent.setup()
  const declarations = arrange(lookUpContract)

  await user.click(screen.getByRole('button', { name: 'Add header' }))
  await user.type(screen.getByRole('textbox', { name: 'Name' }), 'accept')

  expect(screen.getByText(/managed by Apicius/)).toBeInTheDocument()
  await user.click(screen.getByRole('button', { name: 'Save' }))
  expect(declarations.addRequestHeader).not.toHaveBeenCalled()
})

// UC4: rows edit in place — the editor opens seeded with the declaration, saves through
// the location's update endpoint addressed by the current name.
test('edits an authored filter in place', async () => {
  const user = userEvent.setup()
  const declarations = arrange(
    {
      ...browseContract,
      queryParameters: [{ name: 'priceMax', coreType: 'DECIMAL_NUMBER', required: false }],
    },
    browseRoute,
  )

  await user.click(screen.getByRole('button', { name: 'Edit priceMax' }))
  const name = screen.getByRole('textbox', { name: 'Name' })
  expect(name).toHaveValue('priceMax')
  await user.clear(name)
  await user.type(name, 'Budget')
  await user.click(screen.getByRole('button', { name: 'Save' }))

  expect(declarations.updateQueryParameter).toHaveBeenCalledWith(
    expect.objectContaining({
      name: 'priceMax',
      data: expect.objectContaining({ name: 'Budget' }),
    }),
    expect.anything(),
  )
})

// UC4: a local row's Remove deletes outright — everything is capability-local here.
test('removes an authored request header from its row', async () => {
  const user = userEvent.setup()
  const declarations = arrange({
    ...lookUpContract,
    headers: {
      derived: [{ name: 'Accept', value: 'application/json', derived: true }],
      authored: [{ name: 'Request-Id', coreType: 'TEXT', required: false }],
    },
  })

  await user.click(screen.getByRole('button', { name: 'Remove Request-Id' }))

  expect(declarations.removeRequestHeader).toHaveBeenCalledWith(
    { specId: 'spec-1', schemaName: 'Product', capability: 'LOOK_UP', name: 'Request-Id' },
    expect.anything(),
  )
})

// UC3/AC3 surface: response headers live in the success answer's expansion — "+ N headers"
// hints while collapsed, the rows and add affordance sit inside, and optionality (an
// inputs-only concept) never appears.
test('expands the success answer to its own response headers', async () => {
  const user = userEvent.setup()
  const declarations = arrange(
    {
      ...browseContract,
      answers: {
        ...browseContract.answers!,
        successHeaders: [{ name: 'Sync-Token', coreType: 'TEXT', required: false }],
      },
    },
    browseRoute,
  )

  const answers = screen.getByRole('region', { name: 'Answers' })
  expect(within(answers).getByText('+ 1 header')).toBeInTheDocument()
  await user.click(within(answers).getByRole('button', { name: /A paged list of products/ }))
  expect(within(answers).getByText('Sync-Token')).toBeInTheDocument()

  await user.click(within(answers).getByRole('button', { name: 'Add header' }))
  expect(within(answers).queryByRole('switch', { name: 'Required' })).not.toBeInTheDocument()
  await user.type(within(answers).getByRole('textbox', { name: 'Name' }), 'Request id')
  expect(within(answers).getByText('header Request-Id')).toBeInTheDocument()
  await user.click(within(answers).getByRole('button', { name: 'Save' }))

  expect(declarations.addResponseHeader).toHaveBeenCalledWith(
    {
      specId: 'spec-1',
      schemaName: 'Product',
      capability: 'BROWSE',
      data: expect.objectContaining({ name: 'Request id', required: undefined }),
    },
    expect.anything(),
  )
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
