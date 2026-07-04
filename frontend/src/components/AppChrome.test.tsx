import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { expect, test, vi } from 'vitest'
import { AppChrome } from './AppChrome'

const signoutRedirect = vi.fn()

vi.mock('react-oidc-context', () => ({
  useAuth: () => ({
    user: { profile: { given_name: 'Ada', family_name: 'Lovelace' } },
    signoutRedirect,
  }),
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
