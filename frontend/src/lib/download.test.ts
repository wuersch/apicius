import { afterEach, describe, expect, it, vi } from 'vitest'
import { filenameFromContentDisposition, triggerDownload } from './download'

describe('filenameFromContentDisposition', () => {
  // FEAT-008 AC3: the server names the file; filename* carries the unicode title.
  it('prefers the RFC 5987 filename*', () => {
    expect(
      filenameFromContentDisposition(
        'attachment; filename="Payments API v2.yaml";' +
          " filename*=UTF-8''Payments%20API%20v2%20%E2%9C%A8.yaml",
      ),
    ).toBe('Payments API v2 ✨.yaml')
  })

  it('falls back to the quoted filename', () => {
    expect(filenameFromContentDisposition('attachment; filename="Payments API.yaml"')).toBe(
      'Payments API.yaml',
    )
  })

  it('is undefined when the header is absent or nameless', () => {
    expect(filenameFromContentDisposition(null)).toBeUndefined()
    expect(filenameFromContentDisposition('attachment')).toBeUndefined()
  })

  // A malformed percent-encoding must not throw mid-download — fall back instead.
  it('falls back to the quoted filename when filename* is malformed', () => {
    expect(
      filenameFromContentDisposition(
        'attachment; filename="a.yaml"; filename*=UTF-8\'\'%E2%28.yaml',
      ),
    ).toBe('a.yaml')
  })
})

describe('triggerDownload', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('clicks a temporary object-URL anchor and revokes it', () => {
    // jsdom has no object URLs — provide them.
    const createSpy = vi.fn((_blob: Blob) => 'blob:mock')
    const revokeSpy = vi.fn()
    Object.assign(URL, { createObjectURL: createSpy, revokeObjectURL: revokeSpy })
    let clicked: { href: string; download: string } | undefined
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
      this: HTMLAnchorElement,
    ) {
      clicked = { href: this.href, download: this.download }
    })

    triggerDownload('openapi: "3.1.1"\n', 'Payments API.yaml', 'application/yaml')

    expect(createSpy).toHaveBeenCalledWith(expect.any(Blob))
    expect(createSpy.mock.calls[0]?.[0].type).toBe('application/yaml')
    expect(clicked?.href).toContain('blob:mock')
    expect(clicked?.download).toBe('Payments API.yaml')
    expect(revokeSpy).toHaveBeenCalledWith('blob:mock')
  })
})
