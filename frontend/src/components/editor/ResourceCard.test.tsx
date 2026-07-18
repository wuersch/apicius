import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, expect, test, vi } from 'vitest'
import type { ResourceResponse } from '@/api/model'
import { ResourceCard } from './ResourceCard'

vi.mock('@/api/endpoints/specs/specs', () => ({
  useAddField: vi.fn(),
  useUpdateField: vi.fn(),
  useRemoveField: vi.fn(),
  useUpdateResourceDescription: vi.fn(),
  getGetSpecQueryKey: (specId: string) => [`/api/v1/specs/${specId}`],
  getListSpecsQueryKey: () => ['/api/v1/specs'],
  getGetLastEditedLocationQueryKey: () => ['/api/v1/specs/last-edited'],
}))

import {
  useAddField,
  useRemoveField,
  useUpdateField,
  useUpdateResourceDescription,
} from '@/api/endpoints/specs/specs'

const updateResourceDescriptionSpy = vi.fn().mockResolvedValue({})

const product: ResourceResponse = {
  name: 'Product',
  description: 'Something you sell.',
  capabilities: [
    { capability: 'BROWSE', label: 'See all products', method: 'GET', path: '/products' },
    { capability: 'ADD', label: 'Add a product', method: 'POST', path: '/products' },
  ],
  fields: [{ name: 'id', coreType: 'TEXT', list: false, required: true, visibility: 'AUTO' }],
}

function arrange(defaultOpen?: boolean, resource: ResourceResponse = product) {
  vi.mocked(useAddField).mockReturnValue({ mutate: vi.fn(), isPending: false } as never)
  vi.mocked(useUpdateField).mockReturnValue({ mutate: vi.fn(), isPending: false } as never)
  vi.mocked(useRemoveField).mockReturnValue({ mutate: vi.fn(), isPending: false } as never)
  vi.mocked(useUpdateResourceDescription).mockReturnValue({
    mutateAsync: updateResourceDescriptionSpy,
    isPending: false,
  } as never)
  return render(
    <MemoryRouter>
      <QueryClientProvider client={new QueryClient()}>
        <ResourceCard specId="spec-1" resource={resource} defaultOpen={defaultOpen} />
      </QueryClientProvider>
    </MemoryRouter>,
  )
}

function methodDots(container: HTMLElement) {
  return container.querySelectorAll('button .size-2.rounded-full')
}

beforeEach(() => {
  vi.clearAllMocks()
})

test('shows pluralized field and capability counts in the header', () => {
  arrange()

  expect(screen.getByText('1 field · 2 capabilities')).toBeInTheDocument()
})

test('uses singular forms when there is one of each', () => {
  arrange(false, {
    name: 'Product',
    capabilities: [product.capabilities![0]],
    fields: product.fields,
  })

  expect(screen.getByText('1 field · 1 capability')).toBeInTheDocument()
})

test('starts open when defaultOpen and shows the body', () => {
  arrange(true)

  expect(screen.getByRole('button', { name: /Product/ })).toHaveAttribute('aria-expanded', 'true')
  expect(screen.getByText('See all products')).toBeInTheDocument()
  expect(screen.getByRole('region', { name: 'Shape of Product' })).toBeInTheDocument()
})

test('starts collapsed by default, hiding the body', () => {
  arrange()

  expect(screen.getByRole('button', { name: /Product/ })).toHaveAttribute('aria-expanded', 'false')
  expect(screen.queryByText('See all products')).not.toBeInTheDocument()
  expect(screen.queryByRole('region', { name: 'Shape of Product' })).not.toBeInTheDocument()
})

// The collapsed header keeps a glanceable summary: one method dot per capability (mockup 2a).
test('shows method dots only while collapsed', () => {
  const { container } = arrange()

  expect(methodDots(container)).toHaveLength(2)
})

test('hides the method dots while open', () => {
  const { container } = arrange(true)

  expect(methodDots(container)).toHaveLength(0)
})

// FEAT-009 UC1: each capability row is the entry point to its contract view.
test('capability rows link to the contract view', () => {
  arrange(true)

  expect(screen.getByRole('link', { name: /See all products/ })).toHaveAttribute(
    'href',
    '/apis/spec-1/resources/Product/capabilities/BROWSE',
  )
  expect(screen.getByRole('link', { name: /Add a product/ })).toHaveAttribute(
    'href',
    '/apis/spec-1/resources/Product/capabilities/ADD',
  )
})

test('clicking the header toggles the body', async () => {
  const user = userEvent.setup()
  arrange()

  const header = screen.getByRole('button', { name: /Product/ })
  await user.click(header)
  expect(header).toHaveAttribute('aria-expanded', 'true')
  expect(screen.getByText('See all products')).toBeInTheDocument()

  await user.click(header)
  expect(header).toHaveAttribute('aria-expanded', 'false')
  expect(screen.queryByText('See all products')).not.toBeInTheDocument()
})

// FEAT-012 UC3: closed, the note is plain body copy; open, it edits in place through the
// resource description endpoint — blank being the clear gesture.
test('shows the description read-only while closed', () => {
  arrange()

  expect(screen.getByText('Something you sell.')).toBeInTheDocument()
  expect(
    screen.queryByRole('button', { name: 'Edit resource description' }),
  ).not.toBeInTheDocument()
})

test('edits the resource description while open', async () => {
  const user = userEvent.setup()
  arrange(true)

  await user.click(screen.getByRole('button', { name: 'Edit resource description' }))
  const editor = screen.getByRole('textbox', { name: 'resource description' })
  expect(editor).toHaveValue('Something you sell.')
  await user.clear(editor)
  await user.type(editor, 'A sellable item.{Enter}')

  expect(updateResourceDescriptionSpy).toHaveBeenCalledWith({
    specId: 'spec-1',
    schemaName: 'Product',
    data: { description: 'A sellable item.' },
  })
})

test('invites a note on an undescribed open resource', () => {
  arrange(true, { ...product, description: undefined })

  expect(screen.getByRole('button', { name: 'Add resource description' })).toHaveTextContent(
    'add a note for readers…',
  )
})
