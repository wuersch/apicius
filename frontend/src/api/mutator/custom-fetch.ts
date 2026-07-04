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

export const customFetch = async <T>(url: string, options: RequestInit): Promise<T> => {
  const headers = new Headers(options.headers)
  const token = apiAuth.getAccessToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetch(url, { ...options, headers })

  if (response.status === 401) {
    // Expired or missing session (FEAT-001 AC3): send the user back to the IdP.
    apiAuth.onUnauthorized()
  }

  // The generated client types responses as a { data, status, headers } envelope.
  const data = response.status === 204 ? undefined : await parseBody(response)
  return { data, status: response.status, headers: response.headers } as T
}

async function parseBody(response: Response): Promise<unknown> {
  const contentType = response.headers.get('content-type')
  return contentType?.includes('application/json') ? response.json() : response.text()
}
