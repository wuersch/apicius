import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, expect, test, vi } from 'vitest'
import { Button } from '@/components/ui/button'
import { NewResourceDialog } from './NewResourceDialog'

vi.mock('@/api/endpoints/specs/specs', () => ({
  useAddResource: vi.fn(),
  getGetSpecQueryKey: (specId: string) => [`/api/v1/specs/${specId}`],
  getListSpecsQueryKey: () => ['/api/v1/specs'],
  getGetLastEditedLocationQueryKey: () => ['/api/v1/specs/last-edited'],
}))

import { useAddResource } from '@/api/endpoints/specs/specs'

const mutateSpy = vi.fn()

function arrange(existingNames: string[] = []) {
  vi.mocked(useAddResource).mockReturnValue({ mutate: mutateSpy, isPending: false } as never)
  const queryClient = new QueryClient()
  const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')
  render(
    <QueryClientProvider client={queryClient}>
      <NewResourceDialog
        specId="spec-1"
        existingNames={existingNames}
        trigger={<Button>New resource</Button>}
      />
    </QueryClientProvider>,
  )
  return { invalidateSpy }
}

async function openDialog(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getByRole('button', { name: 'New resource' }))
  return screen.findByRole('dialog')
}

beforeEach(() => {
  vi.clearAllMocks()
})

// Mockup fidelity (view 2b): heading, explainer, the at-least-one hint, the identity rule
// row (not a checkbox), and the footer promise.
test('renders the settled dialog copy', async () => {
  const user = userEvent.setup()
  arrange()
  await openDialog(user)

  expect(screen.getByRole('heading', { name: 'New resource' })).toBeInTheDocument()
  expect(screen.getByText(/the schema and paths are derived for you/)).toBeInTheDocument()
  expect(screen.getByText('at least one')).toBeInTheDocument()
  expect(screen.getByText(/gets an identifier/)).toBeInTheDocument()
  expect(screen.getAllByRole('checkbox')).toHaveLength(5)
  expect(screen.getByText(/Creates/)).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Create resource →' })).toBeInTheDocument()
})

// PRIN-002: the derivation is shown live as the noun is typed — schema, path, and the
// per-capability labels all follow.
test('shows the live derivation while typing', async () => {
  const user = userEvent.setup()
  arrange()
  await openDialog(user)

  await user.type(screen.getByLabelText('Name'), 'Order item')

  expect(screen.getByText('schema OrderItem · collection /order-items')).toBeInTheDocument()
  expect(screen.getByText('Browse all order items')).toBeInTheDocument()
  expect(screen.getByText('GET /order-items')).toBeInTheDocument()
  expect(screen.getByText('Add an order item')).toBeInTheDocument()
  expect(screen.getByText('DELETE /order-items/{id}')).toBeInTheDocument()
  expect(screen.getByText(/Every order item gets an identifier/)).toBeInTheDocument()
  expect(screen.getByPlaceholderText('What is an order item?')).toBeInTheDocument()
})

// AC1/UC1: all five pre-selected (PRIN-006); confirming sends exactly them, invalidates the
// projections, closes — and never navigates (the editor re-renders from the projection).
test('creates with all five capabilities pre-selected', async () => {
  const user = userEvent.setup()
  const { invalidateSpy } = arrange()
  mutateSpy.mockImplementation((_variables, options) =>
    options.onSuccess({ status: 201, data: { name: 'Review' }, headers: new Headers() }),
  )

  await openDialog(user)
  await user.type(screen.getByLabelText('Name'), 'Review')
  await user.type(screen.getByLabelText(/Description/), 'What a customer says.')
  await user.click(screen.getByRole('button', { name: 'Create resource →' }))

  expect(mutateSpy).toHaveBeenCalledWith(
    {
      specId: 'spec-1',
      data: {
        name: 'Review',
        description: 'What a customer says.',
        capabilities: ['BROWSE', 'LOOK_UP', 'ADD', 'UPDATE', 'REMOVE'],
      },
    },
    expect.anything(),
  )
  expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['/api/v1/specs/spec-1'] })
  expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['/api/v1/specs'] })
  expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['/api/v1/specs/last-edited'] })
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
})

// AC4/UC2: deselecting is the deliberate override — the row says what won't exist, the
// footer counts what will, and only the kept capabilities are sent.
test('deselecting a capability updates the row, the footer, and the request', async () => {
  const user = userEvent.setup()
  arrange()

  await openDialog(user)
  await user.type(screen.getByLabelText('Name'), 'Review')
  expect(screen.getByText(/Creates/).textContent).toContain('5 operations')
  expect(screen.getByText(/Creates/).textContent).toContain('2 paths')

  await user.click(screen.getByRole('checkbox', { name: 'Update a review' }))

  expect(screen.getByText("won't be created")).toBeInTheDocument()
  expect(screen.getByText(/Creates/).textContent).toContain('4 operations')
  expect(screen.getByText(/Creates/).textContent).toContain('2 paths')

  await user.click(screen.getByRole('button', { name: 'Create resource →' }))
  expect(mutateSpy).toHaveBeenCalledWith(
    expect.objectContaining({
      data: expect.objectContaining({ capabilities: ['BROWSE', 'LOOK_UP', 'ADD', 'REMOVE'] }),
    }),
    expect.anything(),
  )
})

// AC5/UC3: empty name → inline error, nothing sent, button stays clickable (PRIN-006).
test('blocks an empty name inline without calling the backend', async () => {
  const user = userEvent.setup()
  arrange()

  await openDialog(user)
  await user.click(screen.getByRole('button', { name: 'Create resource →' }))

  expect(mutateSpy).not.toHaveBeenCalled()
  expect(screen.getByRole('alert')).toHaveTextContent(
    "A name is required — it's the noun your API is about.",
  )
  expect(screen.getByRole('button', { name: 'Create resource →' })).toBeEnabled()
})

// The "cleanly derivable" rule, explained in plain language.
test('blocks an underivable name inline', async () => {
  const user = userEvent.setup()
  arrange()

  await openDialog(user)
  await user.type(screen.getByLabelText('Name'), '1product')
  await user.click(screen.getByRole('button', { name: 'Create resource →' }))

  expect(mutateSpy).not.toHaveBeenCalled()
  expect(screen.getByRole('alert')).toHaveTextContent(
    'Use letters, digits and single spaces, starting with a letter.',
  )
})

// AC6/UC3: a name already used in this API is blocked before the request — case-insensitively
// on the derived schema name ("order item" collides with OrderItem).
test('blocks a duplicate name against the loaded projection', async () => {
  const user = userEvent.setup()
  arrange(['OrderItem'])

  await openDialog(user)
  await user.type(screen.getByLabelText('Name'), 'order item')
  await user.click(screen.getByRole('button', { name: 'Create resource →' }))

  expect(mutateSpy).not.toHaveBeenCalled()
  expect(screen.getByRole('alert')).toHaveTextContent(
    'That name is already used in this API — pick a different noun.',
  )
})

// AC6 defense-in-depth: the server's 409 (it stays authoritative — e.g. the Person/People
// path collision the client can't see) renders the same canonical conflict copy.
test('renders the conflict copy when the backend answers 409', async () => {
  const user = userEvent.setup()
  arrange()
  mutateSpy.mockImplementation((_variables, options) =>
    options.onError({
      status: 409,
      data: { title: 'Name conflict', status: 409, violations: [{ field: 'name', message: 'taken' }] },
      headers: new Headers(),
    }),
  )

  await openDialog(user)
  await user.type(screen.getByLabelText('Name'), 'People')
  await user.click(screen.getByRole('button', { name: 'Create resource →' }))

  expect(screen.getByRole('alert')).toHaveTextContent(
    'That name is already used in this API — pick a different noun.',
  )
})

// AC7/UC4: all five deselected → the at-least-one rule, explained; nothing sent.
test('blocks confirming with no capability selected', async () => {
  const user = userEvent.setup()
  arrange()

  await openDialog(user)
  await user.type(screen.getByLabelText('Name'), 'Review')
  for (const label of [
    'Browse all reviews',
    'Look up one review',
    'Add a review',
    'Update a review',
    'Remove a review',
  ]) {
    await user.click(screen.getByRole('checkbox', { name: label }))
  }
  expect(screen.getByText(/Creates/).textContent).toContain('0 operations')

  await user.click(screen.getByRole('button', { name: 'Create resource →' }))

  expect(mutateSpy).not.toHaveBeenCalled()
  expect(screen.getByRole('alert')).toHaveTextContent(
    'A resource is something people can act on — keep at least one capability.',
  )
  expect(screen.getByRole('button', { name: 'Create resource →' })).toBeEnabled()
})

// Cancel abandons cleanly: nothing sent, reopening starts fresh (all five re-selected).
test('cancel closes without creating and resets the form', async () => {
  const user = userEvent.setup()
  arrange()

  await openDialog(user)
  await user.type(screen.getByLabelText('Name'), 'Abandoned')
  await user.click(screen.getByRole('checkbox', { name: 'Update an abandoned' }))
  await user.click(screen.getByRole('button', { name: 'Cancel' }))
  expect(mutateSpy).not.toHaveBeenCalled()
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

  await openDialog(user)
  expect(screen.getByLabelText('Name')).toHaveValue('')
  expect(screen.getAllByRole('checkbox', { checked: true })).toHaveLength(5)
})
