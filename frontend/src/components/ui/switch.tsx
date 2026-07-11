"use client"

import * as React from "react"
import { Switch as SwitchPrimitive } from "radix-ui"

import { cn } from "@/lib/utils"

/** The mockups' 34×20 pill toggle (launcher-hybrid-v7, the field editor's Required). */
function Switch({
  className,
  ...props
}: React.ComponentProps<typeof SwitchPrimitive.Root>) {
  return (
    <SwitchPrimitive.Root
      data-slot="switch"
      className={cn(
        "relative h-5 w-[34px] shrink-0 rounded-full bg-control-border outline-none transition-colors focus-visible:ring-3 focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50 data-[state=checked]:bg-primary",
        className,
      )}
      {...props}
    >
      <SwitchPrimitive.Thumb
        data-slot="switch-thumb"
        className="block size-4 translate-x-0.5 rounded-full bg-[#FBF4E9] shadow-[0_1px_2px_rgba(43,33,24,.2)] transition-transform data-[state=checked]:translate-x-[14px]"
      />
    </SwitchPrimitive.Root>
  )
}

export { Switch }
