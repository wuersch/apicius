import { useEffect, type ReactNode } from 'react'
import { useAuth, useAutoSignin } from 'react-oidc-context'
import { configureApiAuth } from '@/api/mutator/custom-fetch'

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

  if (auth.error) {
    return (
      <main className="flex min-h-svh items-center justify-center">
        <p role="alert">Sign-in failed: {auth.error.message}</p>
      </main>
    )
  }

  if (!auth.isAuthenticated) {
    return (
      <main className="flex min-h-svh items-center justify-center">
        <p className="text-muted-foreground">Redirecting to sign-in…</p>
      </main>
    )
  }

  return <>{children}</>
}
