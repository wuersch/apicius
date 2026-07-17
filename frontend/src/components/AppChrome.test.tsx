import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { expect, test, vi } from 'vitest'
import { AppChrome } from './AppChrome'

const signoutRedirect = vi.fn()

vi.mock('react-oidc-context', () => ({
  useAuth: () => ({
    user: { access_token: 'token', profile: { given_name: 'Ada', family_name: 'Lovelace' } },
    signoutRedirect,
  }),
}))

vi.mock('@/api/endpoints/specs/specs', () => ({
  useGetSpec: vi.fn(() => ({
    data: { status: 200, data: { id: 'spec-1', title: 'Fleet API' } },
  })),
}))

// FEAT-001 AC4/AC6: Sign out is reachable from the avatar and performs the
// RP-initiated IdP logout.
test('signs out via the IdP from the avatar menu', async () => {
  render(
    <MemoryRouter>
      <AppChrome />
    </MemoryRouter>,
  )

  await userEvent.click(screen.getByRole('button', { name: /account/i }))
  await userEvent.click(await screen.findByRole('menuitem', { name: /sign out/i }))

  expect(signoutRedirect).toHaveBeenCalledOnce()
})

// FEAT-002: the masthead carries the brand lockup, linking home.
test('the brand lockup links to the home route', () => {
  render(
    <MemoryRouter>
      <AppChrome />
    </MemoryRouter>,
  )
  expect(screen.getByRole('link', { name: /apicius/i })).toHaveAttribute('href', '/')
})

// FEAT-009: inside a capability, the breadcrumb deepens into the brand's slot — the
// masthead persists, the way back stays one glance away (mockup View 3).
test('replaces the brand with the breadcrumb inside a capability', () => {
  render(
    <MemoryRouter initialEntries={['/apis/spec-1/resources/Vehicle/capabilities/BROWSE']}>
      <AppChrome />
    </MemoryRouter>,
  )

  expect(screen.queryByRole('link', { name: /apicius/i })).not.toBeInTheDocument()
  const breadcrumb = screen.getByRole('navigation', { name: 'Breadcrumb' })
  expect(screen.getByRole('link', { name: 'All APIs' })).toHaveAttribute('href', '/')
  expect(screen.getByRole('link', { name: 'Fleet API' })).toHaveAttribute('href', '/apis/spec-1')
  expect(breadcrumb).toHaveTextContent('Vehicle')
})
