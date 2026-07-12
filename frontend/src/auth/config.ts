import { InMemoryWebStorage, WebStorageStateStore } from 'oidc-client-ts'
import type { User } from 'oidc-client-ts'
import type { AuthProviderProps } from 'react-oidc-context'

/**
 * The URL to return to after an IdP round-trip — every sign-in passes this as OIDC state
 * so reloads and deep links land back where they started, not on the root page. The
 * payload never reaches the IdP: oidc-client-ts stores it locally and sends only an
 * opaque handle, so this works with Keycloak, Entra ID, or any compliant IdP.
 */
export function returnTo(): string {
  return window.location.pathname + window.location.search
}

// OIDC authorization-code flow with PKCE (ADR-0005). The IdP is pure configuration —
// Keycloak in dev (see backend dev-realm.json), Entra ID or any OIDC IdP in prod (AC5).
export const oidcConfig: AuthProviderProps = {
  authority: import.meta.env.VITE_OIDC_AUTHORITY,
  client_id: import.meta.env.VITE_OIDC_CLIENT_ID,
  redirect_uri: `${window.location.origin}/`,
  post_logout_redirect_uri: `${window.location.origin}/`,
  scope: 'openid profile email',
  automaticSilentRenew: true,
  // ADR-0005: the token lives in memory only. oidc-client-ts defaults to
  // sessionStorage, so this override is load-bearing. (The transient signin state —
  // PKCE verifier plus the returnTo payload — is a separate store and stays on the
  // sessionStorage default: it must survive the full-page redirect.)
  userStore: new WebStorageStateStore({ store: new InMemoryWebStorage() }),
  // Restore the pre-auth URL and strip code/state from the callback URL. Runs before
  // AuthGate unblocks the router, so replaceState alone repositions the whole app.
  // Only a same-origin path is honored — state rides through the IdP, so an absolute
  // (or protocol-relative) value is never trusted as a navigation target.
  onSigninCallback: (user?: User) => {
    const state = user?.state
    const target = typeof state === 'string' && state.startsWith('/') && !state.startsWith('//')
      ? state
      : window.location.pathname
    window.history.replaceState({}, document.title, target)
  },
}
