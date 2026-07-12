import { beforeEach, expect, test, vi } from 'vitest'
import { downloadDocument } from './downloadDocument'

vi.mock('@/api/endpoints/specs/specs', () => ({
  exportSpecDocument: vi.fn(),
}))
vi.mock('@/lib/download', () => ({
  filenameFromContentDisposition: vi.fn(),
  triggerDownload: vi.fn(),
}))

import { exportSpecDocument } from '@/api/endpoints/specs/specs'
import { filenameFromContentDisposition, triggerDownload } from '@/lib/download'

const spec = {
  id: 'b1000000-0000-4000-8000-000000000001',
  title: 'Payments API',
}

beforeEach(() => {
  vi.clearAllMocks()
})

// FEAT-008 AC3: the server names the file — the Content-Disposition filename wins.
test('downloads the export under the server-chosen filename', async () => {
  const headers = new Headers({
    'content-disposition': 'attachment; filename="Payments API.yaml"',
    'content-type': 'application/yaml',
  })
  vi.mocked(exportSpecDocument).mockResolvedValue({
    status: 200,
    data: 'openapi: "3.1.1"\n',
    headers,
  } as never)
  vi.mocked(filenameFromContentDisposition).mockReturnValue('Payments API.yaml')

  expect(await downloadDocument(spec, 'yaml')).toBe(true)

  expect(exportSpecDocument).toHaveBeenCalledWith(spec.id, { format: 'yaml' })
  expect(filenameFromContentDisposition).toHaveBeenCalledWith(
    'attachment; filename="Payments API.yaml"',
  )
  expect(triggerDownload).toHaveBeenCalledWith(
    'openapi: "3.1.1"\n',
    'Payments API.yaml',
    'application/yaml',
  )
})

// Stripped headers (an odd proxy) still download — filename from the title, a neutral mime.
test('falls back to a title-derived filename and a neutral mime', async () => {
  vi.mocked(exportSpecDocument).mockResolvedValue({
    status: 200,
    data: '{"openapi":"3.1.1"}',
    headers: new Headers(),
  } as never)
  vi.mocked(filenameFromContentDisposition).mockReturnValue(undefined)

  expect(await downloadDocument(spec, 'json')).toBe(true)

  expect(triggerDownload).toHaveBeenCalledWith(
    '{"openapi":"3.1.1"}',
    'Payments API.json',
    'application/octet-stream',
  )
})

// The caller owns the failure UX — report it, download nothing.
test('reports failure without downloading when the export errors', async () => {
  vi.mocked(exportSpecDocument).mockRejectedValue({ status: 500, data: undefined })

  expect(await downloadDocument(spec, 'yaml')).toBe(false)

  expect(triggerDownload).not.toHaveBeenCalled()
})
