import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, expect, test, vi } from 'vitest'
import { ApiCardActions } from './ApiCardActions'

vi.mock('@/api/endpoints/specs/specs', () => ({
  useUpdateSpecDetails: vi.fn(),
  useDuplicateSpec: vi.fn(),
  useDeleteSpec: vi.fn(),
  getListSpecsQueryKey: () => ['/api/v1/specs'],
  getGetLastEditedLocationQueryKey: () => ['/api/v1/specs/last-edited'],
}))
vi.mock('./downloadDocument', () => ({
  downloadDocument: vi.fn(),
}))

import {
  useDeleteSpec,
  useDuplicateSpec,
  useUpdateSpecDetails,
} from '@/api/endpoints/specs/specs'
import { downloadDocument } from './downloadDocument'

const updateSpy = vi.fn()
const duplicateSpy = vi.fn()
const deleteSpy = vi.fn()

const spec = {
  id: 'b1000000-0000-4000-8000-000000000001',
  title: 'Payments API',
  description: 'Charges, refunds and payout schedules.',
  apiVersion: '2.3.0',
  specVersion: '3.1.1',
  resourceCount: 9,
  operationCount: 31,
  updatedAt: '2026-07-10T12:00:00Z',
}

function arrange() {
  vi.mocked(useUpdateSpecDetails).mockReturnValue({ mutate: updateSpy, isPending: false } as never)
  vi.mocked(useDuplicateSpec).mockReturnValue({ mutate: duplicateSpy, isPending: false } as never)
  vi.mocked(useDeleteSpec).mockReturnValue({ mutate: deleteSpy, isPending: false } as never)
  const queryClient = new QueryClient()
  const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')
  render(
    <QueryClientProvider client={queryClient}>
      <ApiCardActions spec={spec} />
    </QueryClientProvider>,
  )
  return { invalidateSpy }
}

async function openMenu(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getByRole('button', { name: 'Actions for Payments API' }))
}

async function openEditDialog(user: ReturnType<typeof userEvent.setup>) {
  await openMenu(user)
  await user.click(await screen.findByRole('menuitem', { name: 'Edit details…' }))
  return screen.findByRole('dialog')
}

async function openDeleteDialog(user: ReturnType<typeof userEvent.setup>) {
  await openMenu(user)
  await user.click(await screen.findByRole('menuitem', { name: 'Delete…' }))
  return screen.findByRole('dialog')
}

beforeEach(() => {
  vi.clearAllMocks()
})

// ---- Edit details (1g·2) ----

// AC1 entry: the dialog pre-fills from the card's summary; the spec version renders locked —
// visible, explained, and not an input (AC2).
test('edit details pre-fills the current values and locks the spec version', async () => {
  const user = userEvent.setup()
  arrange()
  await openEditDialog(user)

  expect(screen.getByRole('heading', { name: 'Edit details' })).toBeInTheDocument()
  expect(
    screen.getByText('The name, version and description everyone sees — nothing inside the API changes.'),
  ).toBeInTheDocument()
  expect(screen.getByLabelText('Title')).toHaveValue('Payments API')
  expect(screen.getByLabelText('Version')).toHaveValue('2.3.0')
  expect(screen.getByLabelText(/Description/)).toHaveValue('Charges, refunds and payout schedules.')
  expect(screen.getByText('OpenAPI 3.1')).toBeInTheDocument()
  expect(screen.getByText('spec version — fixed after creation')).toBeInTheDocument()
  expect(screen.queryByLabelText(/OpenAPI/)).not.toBeInTheDocument()
  expect(screen.getByText(/Rewrites/)).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Save changes' })).toBeInTheDocument()
})

// AC1: saving sends the trimmed details and refreshes the home + jump-back-in (the pointer
// moves server-side), then closes.
test('saves the edited details and invalidates the home queries', async () => {
  const user = userEvent.setup()
  const { invalidateSpy } = arrange()
  updateSpy.mockImplementation((_variables, options) =>
    options.onSuccess({ status: 200, data: { ...spec, title: 'Payments API v2' }, headers: new Headers() }),
  )
  await openEditDialog(user)

  await user.clear(screen.getByLabelText('Title'))
  await user.type(screen.getByLabelText('Title'), 'Payments API v2')
  await user.clear(screen.getByLabelText('Version'))
  await user.type(screen.getByLabelText('Version'), '2.4.0')
  await user.click(screen.getByRole('button', { name: 'Save changes' }))

  expect(updateSpy).toHaveBeenCalledWith(
    {
      specId: spec.id,
      data: {
        title: 'Payments API v2',
        version: '2.4.0',
        description: 'Charges, refunds and payout schedules.',
      },
    },
    expect.anything(),
  )
  expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['/api/v1/specs'] })
  expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['/api/v1/specs/last-edited'] })
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
})

// AC1: a blanked description is omitted from the payload — the backend removes the member.
test('sends a cleared description as undefined', async () => {
  const user = userEvent.setup()
  arrange()
  await openEditDialog(user)

  await user.clear(screen.getByLabelText(/Description/))
  await user.click(screen.getByRole('button', { name: 'Save changes' }))

  expect(updateSpy).toHaveBeenCalledWith(
    { specId: spec.id, data: { title: 'Payments API', version: '2.3.0', description: undefined } },
    expect.anything(),
  )
})

// AC6: an emptied title is blocked inline — no request, canonical copy, button stays clickable.
test('blocks an empty title inline without calling the backend', async () => {
  const user = userEvent.setup()
  arrange()
  await openEditDialog(user)

  await user.clear(screen.getByLabelText('Title'))
  await user.click(screen.getByRole('button', { name: 'Save changes' }))

  expect(updateSpy).not.toHaveBeenCalled()
  expect(screen.getByRole('alert')).toHaveTextContent(
    "A title is required — it's the name everyone will see.",
  )
  expect(screen.getByRole('button', { name: 'Save changes' })).toBeEnabled()
})

// AC6's sibling: the version is the document's required info.version — an empty one is blocked
// the same way.
test('blocks an empty version inline without calling the backend', async () => {
  const user = userEvent.setup()
  arrange()
  await openEditDialog(user)

  await user.clear(screen.getByLabelText('Version'))
  await user.click(screen.getByRole('button', { name: 'Save changes' }))

  expect(updateSpy).not.toHaveBeenCalled()
  expect(screen.getByRole('alert')).toHaveTextContent(
    "A version is required — it's your API's own version number.",
  )
})

// AC6 defense-in-depth: a backend field violation renders the same canonical copy.
test('renders the title error when the backend rejects the title', async () => {
  const user = userEvent.setup()
  arrange()
  updateSpy.mockImplementation((_variables, options) =>
    options.onError({
      status: 400,
      data: {
        title: 'Validation failed',
        status: 400,
        violations: [{ field: 'title', message: 'must not be blank' }],
      },
      headers: new Headers(),
    }),
  )
  await openEditDialog(user)

  await user.click(screen.getByRole('button', { name: 'Save changes' }))

  expect(screen.getByRole('alert')).toHaveTextContent(
    "A title is required — it's the name everyone will see.",
  )
})

// ---- Duplicate ----

// AC3 + the settled handoff: duplicating refreshes the list and opens Edit details pre-filled
// for the copy, so "<title> (copy)" can be renamed immediately.
test('duplicate opens edit details pre-filled for the new copy', async () => {
  const user = userEvent.setup()
  const { invalidateSpy } = arrange()
  duplicateSpy.mockImplementation((_variables, options) =>
    options.onSuccess({
      status: 201,
      data: { ...spec, id: 'c2000000-0000-4000-8000-000000000002', title: 'Payments API (copy)' },
      headers: new Headers(),
    }),
  )

  await openMenu(user)
  await user.click(await screen.findByRole('menuitem', { name: 'Duplicate' }))

  expect(duplicateSpy).toHaveBeenCalledWith({ specId: spec.id }, expect.anything())
  expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['/api/v1/specs'] })
  expect(await screen.findByRole('dialog')).toBeInTheDocument()
  expect(screen.getByLabelText('Title')).toHaveValue('Payments API (copy)')
})

// ---- Download (FEAT-008) ----

// UC1/UC2: two direct items, one per format — no intermediate dialog; the extension hint
// names what lands on disk.
test('downloads the document in the chosen format from the menu', async () => {
  const user = userEvent.setup()
  arrange()
  vi.mocked(downloadDocument).mockResolvedValue(true)

  await openMenu(user)
  const yamlItem = await screen.findByRole('menuitem', { name: /Download as YAML/ })
  expect(yamlItem).toHaveTextContent('.yaml')
  await user.click(yamlItem)
  expect(downloadDocument).toHaveBeenCalledWith(spec, 'yaml')

  await openMenu(user)
  const jsonItem = await screen.findByRole('menuitem', { name: /Download as JSON/ })
  expect(jsonItem).toHaveTextContent('.json')
  await user.click(jsonItem)
  expect(downloadDocument).toHaveBeenCalledWith(spec, 'json')
})

// Menu downloads fail silently (the Duplicate precedent — the menu is already closed and
// there is no toast surface); it must not crash or open anything.
test('a failed menu download changes nothing', async () => {
  const user = userEvent.setup()
  arrange()
  vi.mocked(downloadDocument).mockResolvedValue(false)

  await openMenu(user)
  await user.click(await screen.findByRole('menuitem', { name: /Download as YAML/ }))

  expect(downloadDocument).toHaveBeenCalledWith(spec, 'yaml')
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
})

// ---- Delete (1g·3) ----

// The escape hatch: a download is a full backup (PRIN-003), offered at the moment of
// destruction — it downloads without disturbing the confirmation ritual.
test('the delete dialog offers a YAML download escape hatch', async () => {
  const user = userEvent.setup()
  arrange()
  vi.mocked(downloadDocument).mockResolvedValue(true)
  await openDeleteDialog(user)

  expect(
    screen.getByText('Download a copy first if you might need it back.'),
  ).toBeInTheDocument()
  await user.click(screen.getByRole('button', { name: /Download as YAML/ }))

  expect(downloadDocument).toHaveBeenCalledWith(spec, 'yaml')
  expect(deleteSpy).not.toHaveBeenCalled()
  expect(screen.getByRole('dialog')).toBeInTheDocument()
  expect(screen.queryByRole('alert')).not.toBeInTheDocument()
})

// Silence is wrong at the moment of destruction — a failed backup download says so inline.
test('a failed escape-hatch download shows an inline alert', async () => {
  const user = userEvent.setup()
  arrange()
  vi.mocked(downloadDocument).mockResolvedValue(false)
  await openDeleteDialog(user)

  await user.click(screen.getByRole('button', { name: /Download as YAML/ }))

  expect(await screen.findByRole('alert')).toHaveTextContent(
    "Something went wrong and the download didn't start. Try again.",
  )
  expect(screen.getByRole('dialog')).toBeInTheDocument()
})

// AC5: the confirmation names the API, states the blast radius, and arms only on an exact match.
test('delete arms only when the exact title is typed', async () => {
  const user = userEvent.setup()
  arrange()
  await openDeleteDialog(user)

  expect(screen.getByRole('heading', { name: 'Delete Payments API?' })).toBeInTheDocument()
  expect(
    screen.getByText('9 resources, 31 capabilities and the full spec are deleted for everyone. There is no undo.'),
  ).toBeInTheDocument()
  const confirm = screen.getByRole('button', { name: 'Delete forever' })
  expect(confirm).toBeDisabled()

  await user.type(screen.getByPlaceholderText('Payments API'), 'payments api')
  expect(confirm).toBeDisabled()

  await user.clear(screen.getByPlaceholderText('Payments API'))
  await user.type(screen.getByPlaceholderText('Payments API'), 'Payments API')
  expect(confirm).toBeEnabled()

  await user.click(confirm)
  expect(deleteSpy).toHaveBeenCalledWith({ specId: spec.id }, expect.anything())
})

// AC5: a completed confirmation removes the API and refreshes the home + jump-back-in
// (deleted pointers are cleared server-side).
test('confirmed delete invalidates the home queries and closes', async () => {
  const user = userEvent.setup()
  const { invalidateSpy } = arrange()
  deleteSpy.mockImplementation((_variables, options) =>
    options.onSuccess({ status: 204, data: undefined, headers: new Headers() }),
  )
  await openDeleteDialog(user)

  await user.type(screen.getByPlaceholderText('Payments API'), 'Payments API')
  await user.click(screen.getByRole('button', { name: 'Delete forever' }))

  expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['/api/v1/specs'] })
  expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['/api/v1/specs/last-edited'] })
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
})

// AC5: abandoning changes nothing — and the ritual starts over on reopen.
test('cancel deletes nothing and reopening starts the confirmation fresh', async () => {
  const user = userEvent.setup()
  arrange()
  await openDeleteDialog(user)

  await user.type(screen.getByPlaceholderText('Payments API'), 'Payments API')
  await user.click(screen.getByRole('button', { name: 'Cancel' }))
  expect(deleteSpy).not.toHaveBeenCalled()
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

  await openDeleteDialog(user)
  expect(screen.getByPlaceholderText('Payments API')).toHaveValue('')
  expect(screen.getByRole('button', { name: 'Delete forever' })).toBeDisabled()
})
