import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test, vi } from 'vitest'
import { SpecVersionPicker } from './SpecVersionPicker'

// AC3 / PRIN-002: three options, each a plain-language capability tradeoff — never a changelog.
test('renders the three versions with their tradeoffs and chips', () => {
  render(<SpecVersionPicker value="3.1" onChange={vi.fn()} />)

  const group = screen.getByRole('radiogroup', { name: 'OpenAPI version' })
  expect(group).toBeInTheDocument()

  const radios = screen.getAllByRole('radio')
  expect(radios).toHaveLength(3)

  expect(screen.getByText('OpenAPI 3.0')).toBeInTheDocument()
  expect(screen.getByText('OpenAPI 3.1')).toBeInTheDocument()
  expect(screen.getByText('OpenAPI 3.2')).toBeInTheDocument()
  expect(screen.getByText('Recommended')).toBeInTheDocument()
  expect(screen.getByText('Newest')).toBeInTheDocument()
  expect(
    screen.getByText('Full JSON Schema, webhooks, example variations. The safe modern default.'),
  ).toBeInTheDocument()
  expect(
    screen.getByText('Widest tooling support. No webhooks; simpler (non-JSON-Schema) data types.'),
  ).toBeInTheDocument()
  expect(
    screen.getByText('QUERY search, streaming responses, hierarchical tags. Tooling still catching up.'),
  ).toBeInTheDocument()
})

test('marks the current value as checked', () => {
  render(<SpecVersionPicker value="3.1" onChange={vi.fn()} />)

  expect(screen.getByRole('radio', { name: /OpenAPI 3\.1/ })).toHaveAttribute('aria-checked', 'true')
  expect(screen.getByRole('radio', { name: /OpenAPI 3\.0/ })).toHaveAttribute('aria-checked', 'false')
})

test('reports the chosen minor', async () => {
  const user = userEvent.setup()
  const onChange = vi.fn()
  render(<SpecVersionPicker value="3.1" onChange={onChange} />)

  await user.click(screen.getByRole('radio', { name: /OpenAPI 3\.2/ }))
  expect(onChange).toHaveBeenCalledWith('3.2')
})
