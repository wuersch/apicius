import { useAuth } from 'react-oidc-context'
import { AppChrome } from '@/components/AppChrome'
import { getFirstName } from '@/auth/initials'
import { useGetCurrentUser } from '@/api/endpoints/users/users'

function App() {
  const auth = useAuth()
  // The first authenticated request: it provisions the app_user server-side
  // (FEAT-001 AC1) and returns the canonical display name. Gated on the token so
  // it can't fire before auth is wired.
  const { data: response } = useGetCurrentUser({
    query: { enabled: Boolean(auth.user?.access_token) },
  })
  const user = response?.status === 200 ? response.data : undefined
  // Greet by first name (the mockups' convention); displayName stays for the chrome.
  const firstName = getFirstName(auth.user?.profile, user?.displayName)

  return (
    <div className="flex min-h-svh flex-col">
      <AppChrome />
      <main className="flex flex-1 items-center justify-center">
        <h1 className="text-2xl font-semibold">{firstName ? `Welcome, ${firstName}` : 'Welcome'}</h1>
      </main>
    </div>
  )
}

export default App
