import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, expect, test, vi } from 'vitest'
import type { FieldResponse } from '@/api/model'
import { FieldEditor } from './FieldEditor'

vi.mock('@/api/endpoints/specs/specs', () => ({
  useAddField: vi.fn(),
  useUpdateField: vi.fn(),
  useRemoveField: vi.fn(),
  getGetSpecQueryKey: (specId: string) => [`/api/v1/specs/${specId}`],
  getListSpecsQueryKey: () => ['/api/v1/specs'],
  getGetLastEditedLocationQueryKey: () => ['/api/v1/specs/last-edited'],
}))

import { useAddField, useRemoveField, useUpdateField } from '@/api/endpoints/specs/specs'

const addSpy = vi.fn()
const updateSpy = vi.fn()
const removeSpy = vi.fn()
const closeSpy = vi.fn()

function arrange({
  existingNames = ['id'],
  field,
}: { existingNames?: string[]; field?: FieldResponse } = {}) {
  vi.mocked(useAddField).mockReturnValue({ mutate: addSpy, isPending: false } as never)
  vi.mocked(useUpdateField).mockReturnValue({ mutate: updateSpy, isPending: false } as never)
  vi.mocked(useRemoveField).mockReturnValue({ mutate: removeSpy, isPending: false } as never)
  const queryClient = new QueryClient()
  const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')
  render(
    <QueryClientProvider client={queryClient}>
      <ul>
        <FieldEditor
          specId="spec-1"
          resourceName="Product"
          existingNames={existingNames}
          field={field}
          onClose={closeSpy}
        />
      </ul>
    </QueryClientProvider>,
  )
  return { invalidateSpy }
}

beforeEach(() => {
  vi.clearAllMocks()
})

// Mockup fidelity (view 2c): header, the kind sentence's three slots, the collapsed
// Advanced disclosure, the derived-serialization footer — and no Remove field in add mode.
test('renders the settled add-mode anatomy', () => {
  arrange()

  expect(screen.getByRole('heading', { name: 'New field' })).toBeInTheDocument()
  expect(screen.getByText(/the schema property is derived from the name/)).toBeInTheDocument()
  expect(screen.getByLabelText('Name')).toBeInTheDocument()
  expect(screen.getByLabelText('Required')).toBeInTheDocument()
  expect(screen.getByLabelText('Core type')).toHaveTextContent('Text')
  expect(screen.getByLabelText('Refinement')).toHaveTextContent('plain')
  expect(screen.getByLabelText('A list of these')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: /Advanced/ })).toHaveTextContent('· normal')
  expect(screen.getByText('→ string')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Add field' })).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: /Remove field/ })).not.toBeInTheDocument()
})

// PRIN-002 / ADR-0011: the property name is derived live, never typed.
test('derives the property name while typing', async () => {
  const user = userEvent.setup()
  arrange()

  await user.type(screen.getByLabelText('Name'), 'First name')

  expect(screen.getByText('property firstName')).toBeInTheDocument()
})

// UC1/AC1: confirming sends the plain-language intent — the serialized constructs stay
// server-side — then re-renders from the invalidated projection (never echoes the form).
test('adds a field and invalidates the projections', async () => {
  const user = userEvent.setup()
  addSpy.mockImplementation((_variables, options) => options.onSuccess({ status: 201 }))
  const { invalidateSpy } = arrange()

  await user.type(screen.getByLabelText('Name'), 'First name')
  await user.click(screen.getByLabelText('Required'))
  await user.click(screen.getByRole('button', { name: 'Add field' }))

  expect(addSpy).toHaveBeenCalledWith(
    {
      specId: 'spec-1',
      schemaName: 'Product',
      data: {
        name: 'First name',
        coreType: 'TEXT',
        refinement: undefined,
        list: false,
        required: true,
        // Untouched visibility is omitted: the server's house rule stays authoritative (AC5).
        visibility: undefined,
        description: undefined,
      },
    },
    expect.anything(),
  )
  expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['/api/v1/specs/spec-1'] })
  expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['/api/v1/specs'] })
  expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['/api/v1/specs/last-edited'] })
  expect(closeSpy).toHaveBeenCalled()
})

// UC2/AC4: a refinement and the list wrapper travel as intent; the footer speaks the
// derived serialization (mockup 2d·2).
test('sends refinement and list choices', async () => {
  const user = userEvent.setup()
  arrange()

  await user.type(screen.getByLabelText('Name'), 'Related products')
  await user.click(screen.getByLabelText('Refinement'))
  await user.click(await screen.findByRole('menuitemradio', { name: 'UUID' }))
  await user.click(screen.getByLabelText('A list of these'))

  expect(screen.getByText('→ array of string · uuid')).toBeInTheDocument()

  await user.click(screen.getByRole('button', { name: 'Add field' }))
  expect(addSpy).toHaveBeenCalledWith(
    expect.objectContaining({
      data: expect.objectContaining({ refinement: 'UUID', list: true }),
    }),
    expect.anything(),
  )
})

// AC5 (mockup 2d·3): Text as password opens Advanced itself — write-only applied with the
// house-rule tag, explained in a banner, overridable; the request still omits visibility so
// the server's own rule decides.
test('applies the password house rule visibly and overridably', async () => {
  const user = userEvent.setup()
  arrange()

  await user.type(screen.getByLabelText('Name'), 'Password')
  await user.click(screen.getByLabelText('Refinement'))
  await user.click(await screen.findByRole('menuitemradio', { name: 'password' }))

  expect(screen.getByRole('radiogroup', { name: 'Visibility' })).toBeInTheDocument()
  expect(screen.getByText('house rule')).toBeInTheDocument()
  expect(screen.getByText(/never returned, so write-only was switched on/)).toBeInTheDocument()
  expect(screen.getByText('→ string · password · write-only')).toBeInTheDocument()

  await user.click(screen.getByRole('button', { name: 'Add field' }))
  expect(addSpy).toHaveBeenCalledWith(
    expect.objectContaining({
      data: expect.objectContaining({ refinement: 'PASSWORD', visibility: undefined }),
    }),
    expect.anything(),
  )

  // The override is a deliberate act — sent explicitly, banner gone (AC5's second half).
  await user.click(screen.getByRole('button', { name: 'Override' }))
  expect(screen.queryByText(/switched on for you/)).not.toBeInTheDocument()
  await user.click(screen.getByRole('button', { name: 'Add field' }))
  expect(addSpy).toHaveBeenLastCalledWith(
    expect.objectContaining({
      data: expect.objectContaining({ refinement: 'PASSWORD', visibility: 'NORMAL' }),
    }),
    expect.anything(),
  )
})

// AC10 as UI fact: visibility is one radio group of three — no combination of controls can
// express auto and write-only together.
test('offers visibility as a single choice of three', async () => {
  const user = userEvent.setup()
  arrange()

  await user.click(screen.getByRole('button', { name: /Advanced/ }))

  const radios = screen.getAllByRole('radio')
  expect(radios).toHaveLength(3)
  await user.click(screen.getByRole('radio', { name: /Auto/ }))
  expect(screen.getByText('→ string · read-only')).toBeInTheDocument()
})

// UC5/AC9 (mockup 2d·4): a colliding name — id counts too — blocks inline with the confirm
// disabled; nothing is sent.
test('blocks a colliding name inline', async () => {
  const user = userEvent.setup()
  arrange({ existingNames: ['id', 'name'] })

  await user.type(screen.getByLabelText('Name'), 'Name')

  expect(screen.getByRole('alert')).toHaveTextContent(/already has a field/)
  expect(screen.getByRole('button', { name: 'Add field' })).toBeDisabled()

  await user.clear(screen.getByLabelText('Name'))
  await user.type(screen.getByLabelText('Name'), 'Id')
  expect(screen.getByRole('alert')).toBeInTheDocument()
  expect(addSpy).not.toHaveBeenCalled()
})

// UC5/AC9: a name that derives to nothing cannot be confirmed; nothing is sent.
test('blocks a name that derives to nothing', async () => {
  const user = userEvent.setup()
  arrange()

  await user.type(screen.getByLabelText('Name'), '!!!')
  await user.click(screen.getByRole('button', { name: 'Add field' }))

  expect(screen.getByRole('alert')).toHaveTextContent(/letters or digits/)
  expect(addSpy).not.toHaveBeenCalled()
})

const priceField: FieldResponse = {
  name: 'price',
  coreType: 'DECIMAL_NUMBER',
  list: false,
  required: true,
  visibility: 'NORMAL',
  description: 'Unit price in USD.',
}

// UC3 (mockup 2d·6): edit mode — prefilled from the projection, one atomic save through the
// update endpoint, addressed by the current property name.
test('edits a field in place', async () => {
  const user = userEvent.setup()
  updateSpy.mockImplementation((_variables, options) => options.onSuccess({ status: 200 }))
  arrange({ existingNames: ['id', 'price'], field: priceField })

  expect(screen.getByRole('heading', { name: /Edit field/ })).toBeInTheDocument()
  expect(screen.getByLabelText('Name')).toHaveValue('price')
  expect(screen.getByLabelText('Core type')).toHaveTextContent('Decimal number')
  expect(screen.getByText('→ number · in required')).toBeInTheDocument()

  await user.clear(screen.getByLabelText('Name'))
  await user.type(screen.getByLabelText('Name'), 'Unit price')
  await user.click(screen.getByRole('button', { name: 'Save changes' }))

  expect(updateSpy).toHaveBeenCalledWith(
    expect.objectContaining({
      specId: 'spec-1',
      schemaName: 'Product',
      propertyName: 'price',
      data: expect.objectContaining({ name: 'Unit price', coreType: 'DECIMAL_NUMBER' }),
    }),
    expect.anything(),
  )
  expect(closeSpy).toHaveBeenCalled()
})

// AC5/UC2 covers changing a field too: retyping an existing plain field to Text-as-password
// applies the house rule — visibly, overridably — not just in add mode.
test('applies the password house rule when retyping an existing field', async () => {
  const user = userEvent.setup()
  const nickname: FieldResponse = {
    name: 'nickname',
    coreType: 'TEXT',
    list: false,
    required: false,
    visibility: 'NORMAL',
  }
  arrange({ existingNames: ['id', 'nickname'], field: nickname })

  await user.click(screen.getByLabelText('Refinement'))
  await user.click(await screen.findByRole('menuitemradio', { name: 'password' }))

  expect(screen.getByText(/switched on for you/)).toBeInTheDocument()
  expect(screen.getByText('house rule')).toBeInTheDocument()
  expect(screen.getByText('→ string · password · write-only')).toBeInTheDocument()

  // Retracting the kind change retracts the unpinned rule — back to the session's start.
  await user.click(screen.getByLabelText('Refinement'))
  await user.click(await screen.findByRole('menuitemradio', { name: 'plain' }))
  expect(screen.queryByText(/switched on for you/)).not.toBeInTheDocument()
  expect(screen.getByText('→ string')).toBeInTheDocument()
})

// AC9's self-exemption: a field never collides with itself, so an unchanged name saves.
test('lets a field keep its own name', () => {
  arrange({ existingNames: ['id', 'price'], field: priceField })

  expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Save changes' })).toBeEnabled()
})

// UC4 (mockup 2d·6): remove lives where edit lives — danger action, left of the footer.
test('removes the field being edited', async () => {
  const user = userEvent.setup()
  removeSpy.mockImplementation((_variables, options) => options.onSuccess({ status: 204 }))
  const { invalidateSpy } = arrange({ existingNames: ['id', 'price'], field: priceField })

  await user.click(screen.getByRole('button', { name: /Remove field/ }))

  expect(removeSpy).toHaveBeenCalledWith(
    { specId: 'spec-1', schemaName: 'Product', propertyName: 'price' },
    expect.anything(),
  )
  expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['/api/v1/specs/spec-1'] })
  expect(closeSpy).toHaveBeenCalled()
})

// Defense in depth: the server's 409 maps back to the same inline conflict state (AC9).
test('maps a server conflict onto the name field', async () => {
  const user = userEvent.setup()
  addSpy.mockImplementation((_variables, options) =>
    options.onError({ status: 409, data: { violations: [{ field: 'name' }] } }),
  )
  arrange()

  await user.type(screen.getByLabelText('Name'), 'Sku')
  await user.click(screen.getByRole('button', { name: 'Add field' }))

  expect(screen.getByRole('alert')).toHaveTextContent(/already has a field/)
})
