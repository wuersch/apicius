import { useEffect, useRef, type ReactNode } from 'react'
import { useAuth, useAutoSignin } from 'react-oidc-context'
import { configureApiAuth } from '@/api/mutator/custom-fetch'
import { returnTo } from '@/auth/config'
import { Button } from '@/components/ui/button'

// FEAT-001: the app is fully gated — no anonymous surface. Unauthenticated visits
// redirect to the IdP; useAutoSignin dedupes the redirect under StrictMode. Every
// sign-in carries the current URL as OIDC state (returnTo), so the round-trip lands
// back on the reloaded or deep-linked page — onSigninCallback restores it.
export function AuthGate({ children }: { children: ReactNode }) {
  const auth = useAuth()
  useAutoSignin({ signinArgs: { state: returnTo() } })

  // At most one IdP redirect at a time — token-less 401s, session-expiry, and
  // re-renders must not stack up concurrent full-page navigations.
  const redirecting = useRef(false)
  const redirectToSignin = () => {
    if (redirecting.current) return
    redirecting.current = true
    void auth.signinRedirect({ state: returnTo() })
  }

  // Install the API token accessor during render, before any child effect runs
  // (React renders parent before child) — otherwise the first /users/me query
  // fires before the accessor is wired and goes out without the Bearer token.
  configureApiAuth({
    getAccessToken: () => auth.user?.access_token,
    onUnauthorized: redirectToSignin,
  })

  // An error while a session exists means the session ended (e.g. silent renew
  // failed); isAuthenticated stays stale-true then, so useAutoSignin won't retry —
  // redirect to the IdP ourselves (AC3/UC4).
  const sessionEnded = Boolean(auth.error) && auth.isAuthenticated
  useEffect(() => {
    if (sessionEnded && !redirecting.current) {
      redirecting.current = true
      void auth.signinRedirect({ state: returnTo() })
    }
  }, [sessionEnded, auth])

  if (auth.error && !auth.isAuthenticated) {
    return (
      <main className="flex min-h-svh flex-col items-center justify-center gap-4">
        <p role="alert">Sign-in failed: {auth.error.message}</p>
        <Button onClick={redirectToSignin}>Sign in again</Button>
      </main>
    )
  }

  if (!auth.isAuthenticated || sessionEnded) {
    return (
      <main className="flex min-h-svh items-center justify-center">
        <p className="text-muted-foreground">Redirecting to sign-in…</p>
      </main>
    )
  }

  return <>{children}</>
}
