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

// The brand mark (docs/design/artwork/custom-olive.svg), tinted via the olive token (brand
// notes: icon color = olive). Strokes are widened from the source's 20 so the mark stays
// legible at masthead size; the olive's highlight punches out in the page background.
function OliveBranchIcon() {
  return (
    <svg aria-hidden viewBox="0 0 752 748" className="size-5 self-center text-olive">
      <g fill="none" stroke="currentColor" strokeWidth="34" strokeLinecap="round" strokeLinejoin="round">
        <path d="M702,321.786L702,537C702,626.962 628.962,700 539,700L413,700" />
        <path d="M77.553,374C110.334,369.009 139.857,358.853 169.633,349.098C245.377,324.28 323.753,292.049 454.521,381" />
        <path d="M337,700L173,700C83.038,700 10,626.962 10,537L10,211C10,121.038 83.038,48 173,48L212.777,48" />
        <path d="M288.777,48L539,48C628.962,48 702,117.824 702,207.786L702,245.786" />
        <circle cx="702" cy="283.786" r="38" />
        <circle cx="375" cy="700" r="38" />
        <circle cx="250.777" cy="48" r="38" />
      </g>
      <g fill="currentColor" stroke="currentColor" strokeWidth="20" strokeLinejoin="round">
        <path d="M534.041,127.893C358.205,177.747 375.465,302.382 375.465,302.382C375.465,302.382 518.698,332.387 534.041,127.893Z" />
        <path d="M182.505,594.392C382.559,485.34 326.828,356.811 326.828,356.811C326.828,356.811 144.19,369.738 182.505,594.392Z" />
        <path d="M275.18,160.495C256.492,270.344 330.864,294.652 330.864,294.652C330.864,294.652 385.632,224.301 275.18,160.495Z" />
        <path d="M478.721,400.134C516.326,386.073 564.063,420.676 585.259,477.359C606.454,534.042 593.132,591.477 555.527,605.538C517.923,619.6 470.185,584.996 448.99,528.313C427.795,471.63 441.117,414.195 478.721,400.134Z" />
      </g>
      <path
        d="M458.951,494.694C462.47,513.963 468.976,540.444 495.914,567.464"
        fill="none"
        strokeWidth="26"
        strokeLinecap="round"
        className="stroke-background"
      />
    </svg>
  )
}
