import { ChevronDown } from 'lucide-react'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { cn } from '@/lib/utils'

// FEAT-006: one quiet slot of the kind sentence — a closed dropdown costs one value of
// visual weight, not the whole vocabulary (mockup 2d·1). Generic over the option value so
// the core-type and refinement slots share it; the slots differ only in text treatment,
// passed via className.
export function KindDropdown<T extends string | null>({
  value,
  options,
  onChange,
  ariaLabel,
  disabled,
  className,
}: {
  value: T
  options: { value: T; label: string }[]
  onChange: (value: T) => void
  ariaLabel: string
  disabled?: boolean
  className?: string
}) {
  const selected = options.find((option) => option.value === value)
  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        aria-label={ariaLabel}
        disabled={disabled}
        className={cn(
          'flex h-10 shrink-0 items-center gap-2 rounded-[7px] border-[1.5px] border-control-border bg-control py-0 pr-[11px] pl-[13px] text-sm outline-none transition-colors hover:border-ring focus-visible:border-olive focus-visible:ring-3 focus-visible:ring-olive/15 disabled:cursor-not-allowed disabled:opacity-40',
          className,
        )}
      >
        {selected?.label}
        <ChevronDown aria-hidden className="size-[13px] shrink-0 text-mono-derived" strokeWidth={2.2} />
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-auto min-w-40">
        <DropdownMenuRadioGroup
          value={value ?? ''}
          onValueChange={(next) => onChange((next === '' ? null : next) as T)}
        >
          {options.map((option) => (
            <DropdownMenuRadioItem key={option.value ?? 'plain'} value={option.value ?? ''}>
              {option.label}
            </DropdownMenuRadioItem>
          ))}
        </DropdownMenuRadioGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
