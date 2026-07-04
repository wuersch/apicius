import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { expect, test } from 'vitest'
import { ApiCard } from './ApiCard'

const now = new Date('2026-06-27T12:00:00Z')
const spec = {
  id: 'b1000000-0000-4000-8000-000000000001',
  title: 'Storefront API',
  description: 'Sell products online.',
  apiVersion: '1.0',
  resourceCount: 5,
  operationCount: 21,
  updatedAt: '2026-06-25T12:00:00Z',
}

function renderCard() {
  return render(
    <MemoryRouter>
      <ApiCard spec={spec} now={now} />
    </MemoryRouter>,
  )
}

// FEAT-002 AC2: monogram, title, description, counts in domain vocabulary, version, relative age.
test('renders the card fields in domain vocabulary', () => {
  renderCard()
  expect(screen.getByText('S')).toBeInTheDocument()
  expect(screen.getByText('Storefront API')).toBeInTheDocument()
  expect(screen.getByText('Sell products online.')).toBeInTheDocument()
  expect(screen.getByText('5 resources · 21 ops')).toBeInTheDocument()
  expect(screen.getByText('v1.0')).toBeInTheDocument()
  expect(screen.getByText('2d ago')).toBeInTheDocument()
})

// FEAT-002 AC3: the whole card is a link into the editor for that API.
test('links to the editor route for the API', () => {
  renderCard()
  expect(screen.getByRole('link')).toHaveAttribute('href', `/apis/${spec.id}`)
})
