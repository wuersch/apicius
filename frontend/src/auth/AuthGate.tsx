import { useEffect, type ReactNode } from 'react'
import { useAuth, useAutoSignin } from 'react-oidc-context'
import { configureApiAuth } from '@/api/mutator/custom-fetch'
import { Button } from '@/components/ui/button'

// FEAT-001: the app is fully gated — no anonymous surface. Unauthenticated visits
// redirect to the IdP; useAutoSignin dedupes the redirect under StrictMode.
export function AuthGate({ children }: { children: ReactNode }) {
  const auth = useAuth()
  useAutoSignin()

  useEffect(() => {
    configureApiAuth({
      getAccessToken: () => auth.user?.access_token,
      onUnauthorized: () => {
        void auth.signinRedirect()
      },
    })
  }, [auth])

  // An error while a session exists means the session ended (e.g. silent renew
  // failed); isAuthenticated stays stale-true then, so useAutoSignin won't retry —
  // redirect to the IdP ourselves (AC3/UC4).
  const sessionEnded = Boolean(auth.error) && auth.isAuthenticated
  useEffect(() => {
    if (sessionEnded) {
      void auth.signinRedirect()
    }
  }, [sessionEnded, auth])

  if (auth.error && !auth.isAuthenticated) {
    return (
      <main className="flex min-h-svh flex-col items-center justify-center gap-4">
        <p role="alert">Sign-in failed: {auth.error.message}</p>
        <Button onClick={() => void auth.signinRedirect()}>Sign in again</Button>
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
