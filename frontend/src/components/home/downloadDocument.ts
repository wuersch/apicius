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
    // The server owns the media type too (ExportFormat.mediaType()) — read it, don't re-derive.
    const mimeType = response.headers.get('content-type') ?? 'application/octet-stream'
    triggerDownload(response.data, filename, mimeType)
    return true
  } catch {
    return false
  }
}
