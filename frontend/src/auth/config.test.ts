import type { User } from 'oidc-client-ts'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { oidcConfig } from './config'

// The callback runs before the router mounts, so replaceState alone repositions the app.
function signinCallback(state: unknown) {
  oidcConfig.onSigninCallback?.({ state } as User)
}

describe('onSigninCallback', () => {
  const replaceState = vi.spyOn(window.history, 'replaceState')

  afterEach(() => {
    replaceState.mockClear()
    window.history.replaceState({}, '', '/')
  })

  // Reloads and deep links: the pre-auth URL round-trips as OIDC state and is restored.
  it('restores the pre-auth URL from the OIDC state', () => {
    signinCallback('/apis/abc?tab=shape')

    expect(replaceState).toHaveBeenCalledWith({}, document.title, '/apis/abc?tab=shape')
  })

  it('strips the callback params when no state was carried', () => {
    window.history.replaceState({}, '', '/?code=x&state=y')

    signinCallback(undefined)

    expect(replaceState).toHaveBeenLastCalledWith({}, document.title, '/')
  })

  // State rides through the IdP — never trust it as an absolute or protocol-relative
  // navigation target.
  it('ignores state that is not a same-origin path', () => {
    for (const hostile of ['https://evil.example/', '//evil.example/', { path: '/x' }, 42]) {
      signinCallback(hostile)
      expect(replaceState).toHaveBeenLastCalledWith({}, document.title, '/')
    }
  })
})
