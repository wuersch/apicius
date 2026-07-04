import { InMemoryWebStorage, WebStorageStateStore } from 'oidc-client-ts'
import type { AuthProviderProps } from 'react-oidc-context'

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
  // sessionStorage, so this override is load-bearing.
  userStore: new WebStorageStateStore({ store: new InMemoryWebStorage() }),
  // Strip code/state from the URL after the IdP redirects back.
  onSigninCallback: () => {
    window.history.replaceState({}, document.title, window.location.pathname)
  },
}
