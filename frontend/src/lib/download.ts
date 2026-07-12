// FEAT-008: hand a fetched export to the browser as a file download.

/** The filename the server chose, from a Content-Disposition: attachment header (AC3). */
export function filenameFromContentDisposition(header: string | null): string | undefined {
  if (!header) return undefined
  // Prefer the RFC 5987 filename* — it carries the unicode title the plain filename can't.
  const extended = /filename\*=UTF-8''([^;]+)/i.exec(header)
  if (extended) {
    try {
      return decodeURIComponent(extended[1])
    } catch {
      // Malformed percent-encoding — fall through to the plain filename.
    }
  }
  return /filename="((?:[^"\\]|\\.)*)"/.exec(header)?.[1].replace(/\\(.)/g, '$1')
}

export function triggerDownload(content: string, filename: string, mimeType: string): void {
  const url = URL.createObjectURL(new Blob([content], { type: mimeType }))
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.click()
  URL.revokeObjectURL(url)
}
