import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import type { CreateSpecRequestSpecVersion } from '@/api/model'

export type SpecVersionOption = CreateSpecRequestSpecVersion

// FEAT-003 / PRIN-002: each option is a one-line capability tradeoff, never a changelog —
// the designer reads consequences, not the encoding. Copy is settled by the v4 mockup.
const OPTIONS: ReadonlyArray<{
  value: SpecVersionOption
  label: string
  chip?: { text: string; className: string }
  tradeoff: string
}> = [
  {
    value: '3.0',
    label: 'OpenAPI 3.0',
    tradeoff: 'Widest tooling support. No webhooks; simpler (non-JSON-Schema) data types.',
  },
  {
    value: '3.1',
    label: 'OpenAPI 3.1',
    chip: { text: 'Recommended', className: 'bg-olive-chip text-olive-chip-foreground' },
    tradeoff: 'Full JSON Schema, webhooks, example variations. The safe modern default.',
  },
  {
    value: '3.2',
    label: 'OpenAPI 3.2',
    chip: { text: 'Newest', className: 'bg-ochre-chip text-ochre' },
    tradeoff: 'QUERY search, streaming responses, hierarchical tags. Tooling still catching up.',
  },
]

export function SpecVersionPicker({
  value,
  onChange,
}: {
  value: SpecVersionOption
  onChange: (value: SpecVersionOption) => void
}) {
  return (
    <RadioGroup
      aria-label="OpenAPI version"
      value={value}
      onValueChange={(next) => onChange(next as SpecVersionOption)}
    >
      {OPTIONS.map((option) => (
        <RadioGroupItem
          key={option.value}
          value={option.value}
          // Selection = olive border + pale olive tint (mockup: #F4F2E6 / #37381F in dark) —
          // olive means good/safe everywhere, never decoration.
          className="rounded-lg border border-border bg-card px-4 py-3 text-left transition-colors data-[state=checked]:border-olive data-[state=checked]:bg-[#F4F2E6] dark:data-[state=checked]:bg-[#37381F]"
        >
          <div className="flex items-center gap-2">
            <span className="text-sm font-bold">{option.label}</span>
            {option.chip && (
              <span
                className={`rounded-sm px-2 py-0.5 text-[10.5px] font-bold ${option.chip.className}`}
              >
                {option.chip.text}
              </span>
            )}
          </div>
          <p className="mt-0.5 text-[12.5px] leading-normal text-text-tertiary">
            {option.tradeoff}
          </p>
        </RadioGroupItem>
      ))}
    </RadioGroup>
  )
}
