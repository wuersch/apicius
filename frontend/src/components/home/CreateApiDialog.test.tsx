import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, expect, test, vi } from 'vitest'
import { Button } from '@/components/ui/button'
import { CreateApiDialog } from './CreateApiDialog'

const navigateSpy = vi.fn()

vi.mock('react-router', async (importOriginal) => ({
  ...(await importOriginal<typeof import('react-router')>()),
  useNavigate: () => navigateSpy,
}))

vi.mock('@/api/endpoints/specs/specs', () => ({
  useCreateSpec: vi.fn(),
  getListSpecsQueryKey: () => ['/api/v1/specs'],
  getGetLastEditedLocationQueryKey: () => ['/api/v1/specs/last-edited'],
}))

import { useCreateSpec } from '@/api/endpoints/specs/specs'

const mutateSpy = vi.fn()

function arrange() {
  vi.mocked(useCreateSpec).mockReturnValue({ mutate: mutateSpy, isPending: false } as never)
  const queryClient = new QueryClient()
  const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')
  render(
    <MemoryRouter>
      <QueryClientProvider client={queryClient}>
        <CreateApiDialog trigger={<Button>New API</Button>} />
      </QueryClientProvider>
    </MemoryRouter>,
  )
  return { invalidateSpy }
}

async function openDialog(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getByRole('button', { name: 'New API' }))
  return screen.findByRole('dialog')
}

beforeEach(() => {
  vi.clearAllMocks()
})

// Mockup fidelity: heading, explainer, placeholders, immutability note, seed note, actions.
test('renders the settled dialog copy', async () => {
  const user = userEvent.setup()
  arrange()
  await openDialog(user)

  expect(screen.getByRole('heading', { name: 'Create a new API' })).toBeInTheDocument()
  expect(screen.getByPlaceholderText('e.g. Storefront API')).toBeInTheDocument()
  expect(screen.getByPlaceholderText('What does this API let people do?')).toBeInTheDocument()
  expect(screen.getByText('fixed after creation')).toBeInTheDocument()
  expect(screen.getByText(/change it any time later/)).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Create API →' })).toBeInTheDocument()
})

// AC1: title + untouched picker → create with the 3.1 default, then navigate to the editor.
test('creates with the default version and navigates on success', async () => {
  const user = userEvent.setup()
  const { invalidateSpy } = arrange()
  mutateSpy.mockImplementation((_variables, options) =>
    options.onSuccess({ status: 201, data: { id: 'new-id' }, headers: new Headers() }),
  )

  await openDialog(user)
  await user.type(screen.getByLabelText('Title'), 'Storefront API')
  await user.click(screen.getByRole('button', { name: 'Create API →' }))

  expect(mutateSpy).toHaveBeenCalledWith(
    { data: { title: 'Storefront API', description: undefined, specVersion: '3.1' } },
    expect.anything(),
  )
  expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['/api/v1/specs'] })
  expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['/api/v1/specs/last-edited'] })
  expect(navigateSpy).toHaveBeenCalledWith('/apis/new-id')
})

// AC2: a typed description is sent; a blank one is omitted (asserted above as undefined).
test('includes the description when provided', async () => {
  const user = userEvent.setup()
  arrange()

  await openDialog(user)
  await user.type(screen.getByLabelText('Title'), 'Storefront API')
  await user.type(screen.getByLabelText(/Description/), 'Sell products online.')
  await user.click(screen.getByRole('button', { name: 'Create API →' }))

  expect(mutateSpy).toHaveBeenCalledWith(
    { data: { title: 'Storefront API', description: 'Sell products online.', specVersion: '3.1' } },
    expect.anything(),
  )
})

// AC3: choosing another version card sends that minor.
test('sends the chosen spec version', async () => {
  const user = userEvent.setup()
  arrange()

  await openDialog(user)
  await user.type(screen.getByLabelText('Title'), 'Fleet API')
  await user.click(screen.getByRole('radio', { name: /OpenAPI 3\.2/ }))
  await user.click(screen.getByRole('button', { name: 'Create API →' }))

  expect(mutateSpy).toHaveBeenCalledWith(
    { data: { title: 'Fleet API', description: undefined, specVersion: '3.2' } },
    expect.anything(),
  )
})

// AC5/UC3: blank title → inline error, no request, button stays clickable.
test('blocks an empty title inline without calling the backend', async () => {
  const user = userEvent.setup()
  arrange()

  await openDialog(user)
  await user.click(screen.getByRole('button', { name: 'Create API →' }))

  expect(mutateSpy).not.toHaveBeenCalled()
  expect(screen.getByRole('alert')).toHaveTextContent(
    "A title is required — it's the name everyone will see.",
  )
  expect(screen.getByRole('button', { name: 'Create API →' })).toBeEnabled()
})

// AC5 defense-in-depth: a backend title violation renders the same canonical copy.
test('renders the title error when the backend rejects the title', async () => {
  const user = userEvent.setup()
  arrange()
  mutateSpy.mockImplementation((_variables, options) =>
    options.onError({
      status: 400,
      data: { title: 'Validation failed', status: 400, violations: [{ field: 'title', message: 'must not be blank' }] },
      headers: new Headers(),
    }),
  )

  await openDialog(user)
  // Client validation passes (whitespace-padded title trims to something non-empty server-side
  // rejected here by the mocked response).
  await user.type(screen.getByLabelText('Title'), 'x')
  await user.click(screen.getByRole('button', { name: 'Create API →' }))

  expect(screen.getByRole('alert')).toHaveTextContent(
    "A title is required — it's the name everyone will see.",
  )
})

// Cancel abandons cleanly: nothing sent, and reopening starts fresh.
test('cancel closes without creating and resets the form', async () => {
  const user = userEvent.setup()
  arrange()

  await openDialog(user)
  await user.type(screen.getByLabelText('Title'), 'Abandoned API')
  await user.click(screen.getByRole('button', { name: 'Cancel' }))
  expect(mutateSpy).not.toHaveBeenCalled()
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

  await openDialog(user)
  expect(screen.getByLabelText('Title')).toHaveValue('')
})
