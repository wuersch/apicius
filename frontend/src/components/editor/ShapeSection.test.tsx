import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, expect, test, vi } from 'vitest'
import type { ResourceResponse } from '@/api/model'
import { ShapeSection } from './ShapeSection'

vi.mock('@/api/endpoints/specs/specs', () => ({
  useAddField: vi.fn(),
  useUpdateField: vi.fn(),
  useRemoveField: vi.fn(),
  getGetSpecQueryKey: (specId: string) => [`/api/v1/specs/${specId}`],
  getListSpecsQueryKey: () => ['/api/v1/specs'],
  getGetLastEditedLocationQueryKey: () => ['/api/v1/specs/last-edited'],
}))

import { useAddField, useRemoveField, useUpdateField } from '@/api/endpoints/specs/specs'

const product: ResourceResponse = {
  name: 'Product',
  capabilities: [],
  fields: [
    { name: 'id', coreType: 'TEXT', list: false, required: true, visibility: 'AUTO' },
    { name: 'name', coreType: 'TEXT', list: false, required: true, visibility: 'NORMAL' },
    {
      name: 'contact',
      coreType: 'TEXT',
      refinement: 'EMAIL',
      list: false,
      required: false,
      visibility: 'NORMAL',
    },
  ],
}

function arrange(resource: ResourceResponse = product) {
  vi.mocked(useAddField).mockReturnValue({ mutate: vi.fn(), isPending: false } as never)
  vi.mocked(useUpdateField).mockReturnValue({ mutate: vi.fn(), isPending: false } as never)
  vi.mocked(useRemoveField).mockReturnValue({ mutate: vi.fn(), isPending: false } as never)
  render(
    <QueryClientProvider client={new QueryClient()}>
      <ShapeSection specId="spec-1" resource={resource} />
    </QueryClientProvider>,
  )
}

beforeEach(() => {
  vi.clearAllMocks()
})

// AC11: every field shows its plain-language kind and attributes; id is visibly present as
// the read-only identity row.
test('shows the shape with id locked first', () => {
  arrange()

  const section = screen.getByRole('region', { name: 'Shape of Product' })
  expect(section).toBeInTheDocument()
  expect(screen.getByText('identifier — the server sets it')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Edit field name' })).toBeInTheDocument()
  expect(screen.getByText('text · email')).toBeInTheDocument()
  expect(screen.getAllByText('required')).toHaveLength(1)
})

// AC7, the UI half: the id row offers no edit affordance at all.
test('gives the id row no edit affordance', () => {
  arrange()

  expect(screen.queryByRole('button', { name: 'Edit field id' })).not.toBeInTheDocument()
})

// The inline editor docks where the row is (never a dialog); one open at a time —
// everything else goes inert.
test('opens one editor at a time', async () => {
  const user = userEvent.setup()
  arrange()

  await user.click(screen.getByRole('button', { name: 'Add a field…' }))

  expect(screen.getByRole('heading', { name: 'New field' })).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Edit field name' })).toBeDisabled()

  await user.click(screen.getByRole('button', { name: 'Cancel' }))
  expect(screen.queryByRole('heading', { name: 'New field' })).not.toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Edit field name' })).toBeEnabled()
})

// The add-mode marker must never collide with a real property name — underscores pass
// through derivation, so a field literally named __new__ is legal.
test('edits a field named __new__ without opening the add editor', async () => {
  const user = userEvent.setup()
  arrange({
    name: 'Product',
    capabilities: [],
    fields: [
      { name: 'id', coreType: 'TEXT', list: false, required: true, visibility: 'AUTO' },
      { name: '__new__', coreType: 'TEXT', list: false, required: false, visibility: 'NORMAL' },
    ],
  })

  await user.click(screen.getByRole('button', { name: 'Edit field __new__' }))

  expect(screen.getAllByRole('heading', { name: /field/i })).toHaveLength(1)
  expect(screen.getByRole('heading', { name: /Edit field/ })).toBeInTheDocument()
  expect(screen.queryByRole('heading', { name: 'New field' })).not.toBeInTheDocument()
})

// UC3: clicking a row opens the editor for that field, prefilled from the projection.
test('opens the edit editor docked at the row', async () => {
  const user = userEvent.setup()
  arrange()

  await user.click(screen.getByRole('button', { name: 'Edit field contact' }))

  expect(screen.getByRole('heading', { name: /Edit field/ })).toBeInTheDocument()
  expect(screen.getByLabelText('Name')).toHaveValue('contact')
  expect(screen.getByRole('button', { name: /Remove field/ })).toBeInTheDocument()
  // The row being edited stays visible, highlighted and inert — not replaced (mockup 2d·6).
  expect(screen.getByText('editing')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Edit field contact' })).not.toBeInTheDocument()
})
