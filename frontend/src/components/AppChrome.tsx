import { Link } from 'react-router'
import { useAuth } from 'react-oidc-context'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { getInitials } from '@/auth/initials'

// The masthead: brand lockup left, identity right (FEAT-001 AC6 — initials avatar with
// Sign out reachable from it; sign out is a full RP-initiated IdP logout). Search, bell
// and the rail arrive with the features that give them behavior.
export function AppChrome() {
  const auth = useAuth()

  return (
    <header className="flex items-center justify-between px-11 pt-7 pb-5">
      <Link to="/" className="flex items-baseline gap-[7px] outline-none focus-visible:ring-2 focus-visible:ring-ring">
        <OliveBranchIcon />
        {/* STUDIO rides the cap line — a deliberate superscript qualifier (brand notes). */}
        <span className="text-lg leading-none font-bold">apicius</span>
        <span className="self-start text-[11.5px] leading-none font-semibold tracking-[.14em] text-text-tertiary">
          STUDIO
        </span>
      </Link>
      <DropdownMenu>
        <DropdownMenuTrigger
          aria-label="Account"
          className="rounded-full outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          <Avatar className="size-8">
            <AvatarFallback>{getInitials(auth.user?.profile)}</AvatarFallback>
          </Avatar>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem onSelect={() => void auth.signoutRedirect()}>Sign out</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </header>
  )
}

function OliveBranchIcon() {
  return (
    <svg
      aria-hidden
      viewBox="0 0 20 20"
      className="size-5 self-center text-olive"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M10 17c0-6 2-10 6-13" />
      <path d="M12.5 8.5c2 .3 3.8-.6 4.5-2.5-2-.3-3.8.6-4.5 2.5Z" />
      <path d="M11 12c-1.8-.8-3.8-.4-5 1 1.8.8 3.8.4 5-1Z" />
      <path d="M13.8 5.2c.2-1.6-.5-3.1-2-4-.2 1.6.5 3.1 2 4Z" />
    </svg>
  )
}
