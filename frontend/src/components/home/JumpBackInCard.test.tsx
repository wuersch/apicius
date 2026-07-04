import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { expect, test } from 'vitest'
import { JumpBackInCard } from './JumpBackInCard'

const now = new Date('2026-06-27T12:00:00Z')
const location = {
  specId: 'b1000000-0000-4000-8000-000000000001',
  specTitle: 'Storefront API',
  apiVersion: '1.0',
  capabilityName: 'Add a product',
  lastEditedAt: '2026-06-27T10:00:00Z',
}

// FEAT-002 AC1: the card names the API and the last-edited capability when recorded.
test('names the API and the capability', () => {
  render(
    <MemoryRouter>
      <JumpBackInCard location={location} now={now} />
    </MemoryRouter>,
  )
  expect(screen.getByText('Storefront API')).toBeInTheDocument()
  expect(screen.getByText('Add a product')).toBeInTheDocument()
  expect(screen.getByText(/2h ago/)).toBeInTheDocument()
})

// FEAT-002 AC1: with no capability recorded, the card resolves to API-level.
test('resolves to API-level when no capability is recorded', () => {
  render(
    <MemoryRouter>
      <JumpBackInCard location={{ ...location, capabilityName: undefined }} now={now} />
    </MemoryRouter>,
  )
  expect(screen.getByText(/You were editing this API/)).toBeInTheDocument()
  expect(screen.queryByText('Add a product')).not.toBeInTheDocument()
})

// FEAT-002 AC1/AC3: opening the card enters the editor on that API.
test('links to the editor route for the API', () => {
  render(
    <MemoryRouter>
      <JumpBackInCard location={location} now={now} />
    </MemoryRouter>,
  )
  expect(screen.getByRole('link')).toHaveAttribute('href', `/apis/${location.specId}`)
})
