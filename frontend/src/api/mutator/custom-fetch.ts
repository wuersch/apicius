// Fetch wrapper orval routes every generated call through (see orval.config.ts).
// It runs outside the React tree, so AuthGate wires the token accessor and the
// 401 handler in at runtime instead of via hooks (ADR-0005: token in memory only).

type ApiAuth = {
  getAccessToken: () => string | undefined
  onUnauthorized: () => void
}

let apiAuth: ApiAuth = {
  getAccessToken: () => undefined,
  onUnauthorized: () => {},
}

export function configureApiAuth(auth: ApiAuth): void {
  apiAuth = auth
}

type ResponseEnvelope = { data: unknown; status: number; headers: Headers }

export const customFetch = async <T>(url: string, options: RequestInit): Promise<T> => {
  const headers = new Headers(options.headers)
  // Only attach the Bearer to first-party requests — never leak the token to a
  // third-party host if an absolute URL ever reaches the generated client.
  const token = isSameOrigin(url) ? apiAuth.getAccessToken() : undefined
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetch(url, { ...options, headers })

  const data = response.status === 204 ? undefined : await parseBody(response)
  const envelope: ResponseEnvelope = { data, status: response.status, headers: response.headers }

  if (!response.ok) {
    // A 401 on a request that carried a token means the session is truly
    // unusable (expired/rejected) — send the user back to the IdP (AC3/UC4).
    // A token-less 401 is not that; it must not trigger a redirect.
    if (response.status === 401 && token) apiAuth.onUnauthorized()
    // Throw so TanStack Query surfaces it as an error (isError / retry).
    throw envelope
  }

  return envelope as T
}

function isSameOrigin(url: string): boolean {
  try {
    return new URL(url, window.location.origin).origin === window.location.origin
  } catch {
    return false
  }
}

async function parseBody(response: Response): Promise<unknown> {
  const contentType = response.headers.get('content-type')
  // 'json' not 'application/json': structured suffixes like application/problem+json
  // (RFC 9457 errors) must parse as JSON too.
  return contentType?.includes('json') ? response.json() : response.text()
}
