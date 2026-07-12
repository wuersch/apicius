import { exportSpecDocument } from '@/api/endpoints/specs/specs'
import type { ExportFormat, SpecSummaryResponse } from '@/api/model'
import { filenameFromContentDisposition, triggerDownload } from '@/lib/download'

// FEAT-008: fetch an export and hand it to the browser as a file. The server names the file
// (AC3) via Content-Disposition; the title is only the stripped-header fallback. Returns
// whether it worked — the caller owns what a failure looks like in its surface.
export async function downloadDocument(
  spec: SpecSummaryResponse,
  format: ExportFormat,
): Promise<boolean> {
  try {
    const response = await exportSpecDocument(spec.id ?? '', { format })
    if (response.status !== 200) return false
    const filename =
      filenameFromContentDisposition(response.headers.get('content-disposition')) ??
      `${spec.title || 'api'}.${format}`
    triggerDownload(
      response.data,
      filename,
      format === 'yaml' ? 'application/yaml' : 'application/json',
    )
    return true
  } catch {
    return false
  }
}
