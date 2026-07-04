import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import { AuthProvider } from 'react-oidc-context'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import './index.css'
import App from './App.tsx'
import { oidcConfig } from './auth/config.ts'
import { AuthGate } from './auth/AuthGate.tsx'

// TanStack Query is the only client-side data layer; it holds view state only —
// the domain model lives server-side (ADR-0002, ADR-0006).
const queryClient = new QueryClient()

// The app is fully gated (FEAT-001): AuthProvider runs the OIDC code+PKCE flow,
// AuthGate keeps everything below it authenticated-only.
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider {...oidcConfig}>
      <QueryClientProvider client={queryClient}>
        <AuthGate>
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </AuthGate>
        <ReactQueryDevtools initialIsOpen={false} />
      </QueryClientProvider>
    </AuthProvider>
  </StrictMode>,
)
