import { render, screen } from '@testing-library/react'
import { expect, test } from 'vitest'
import App from './App'

test('renders the app shell with a call-to-action button', () => {
  render(<App />)
  expect(screen.getByRole('button', { name: /get started/i })).toBeInTheDocument()
})
