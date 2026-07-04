import { render, screen } from '@testing-library/react'
import { expect, test } from 'vitest'
import { EmptyState } from './EmptyState'

// FEAT-002 AC4: the empty state offers the Create and Import entry points…
test('shows the Create and Import ghost tiles', () => {
  render(<EmptyState />)
  expect(screen.getByText('Start from scratch')).toBeInTheDocument()
  expect(screen.getByText('Name a resource, we scaffold the rest.')).toBeInTheDocument()
  expect(screen.getByText('Import a spec')).toBeInTheDocument()
  expect(screen.getByText('Bring an existing OpenAPI file, losslessly.')).toBeInTheDocument()
})

// …but they stay inert until FEAT-003/004 give them behavior — no dead interactive controls.
test('the tiles are not interactive yet', () => {
  render(<EmptyState />)
  expect(screen.queryByRole('button')).not.toBeInTheDocument()
  expect(screen.queryByRole('link')).not.toBeInTheDocument()
})
