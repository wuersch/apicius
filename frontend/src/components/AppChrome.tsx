import { useAuth } from 'react-oidc-context'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { getInitials } from '@/auth/initials'

// FEAT-001 AC6: the identity indicator — an initials avatar with Sign out reachable
// from it. Sign out is a full RP-initiated IdP logout (UC3/AC4).
export function AppChrome() {
  const auth = useAuth()

  return (
    <header className="flex h-12 items-center justify-between border-b px-4">
      <span className="text-sm font-semibold">Apicius</span>
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
