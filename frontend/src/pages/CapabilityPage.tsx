import { Link, useParams } from 'react-router'
import { useAuth } from 'react-oidc-context'
import { useGetCapabilityContract, useGetSpec } from '@/api/endpoints/specs/specs'
import type { Capability } from '@/api/model'
import { CapabilityContract } from '@/components/capability/CapabilityContract'
import { CapabilityRail } from '@/components/capability/CapabilityRail'

// FEAT-009: the contract view — a capability reached from its resource (UC1), the noun's
// "can do" list riding along as the rail. The page renders two projections: the spec detail
// (rail, breadcrumb — already cached from the editor) and the capability contract.
export function CapabilityPage() {
  const { id, schemaName, capability } = useParams<{
    id: string
    schemaName: string
    capability: string
  }>()
  const auth = useAuth()
  // Gated on the token so no query fires before auth is wired (FEAT-001 pattern).
  const enabled = Boolean(auth.user?.access_token) && Boolean(id && schemaName && capability)
  const spec = useGetSpec(id ?? '', { query: { enabled } })
  const contract = useGetCapabilityContract(
    id ?? '',
    schemaName ?? '',
    // The URL segment is the enum's wire value; an unknown literal 404s server-side and
    // lands in the honest not-found state below.
    (capability ?? '') as Capability,
    { query: { enabled } },
  )

  const specData = spec.data?.status === 200 ? spec.data.data : undefined
  const contractData = contract.data?.status === 200 ? contract.data.data : undefined
  const notFound =
    (spec.error as { status?: number } | null)?.status === 404 ||
    (contract.error as { status?: number } | null)?.status === 404

  if (notFound) {
    return (
      <div className="mx-auto w-full max-w-5xl px-11 py-8">
        <h1 className="text-2xl font-bold">This capability doesn't exist</h1>
        <p className="mt-2 text-sm text-text-tertiary">
          It may have been removed.{' '}
          <Link
            to={id ? `/apis/${id}` : '/'}
            className="font-semibold text-foreground underline-offset-2 hover:underline"
          >
            Back to the API
          </Link>
        </p>
      </div>
    )
  }
  if (spec.isPending || contract.isPending || !specData || !contractData) return null

  const resource = specData.resources?.find((candidate) => candidate.name === schemaName)

  return (
    <div className="mx-auto w-full max-w-5xl px-11 py-8">
      <nav aria-label="Breadcrumb" className="text-[13px] text-text-tertiary">
        <Link to="/" className="hover:text-foreground hover:underline underline-offset-2">
          All APIs
        </Link>
        <span aria-hidden className="mx-1.5 text-text-faint">
          /
        </span>
        <Link
          to={`/apis/${id}`}
          className="hover:text-foreground hover:underline underline-offset-2"
        >
          {specData.title}
        </Link>
        <span aria-hidden className="mx-1.5 text-text-faint">
          /
        </span>
        <span className="text-foreground">{schemaName}</span>
      </nav>

      <div className="mt-5 flex gap-8">
        {resource && (
          <CapabilityRail specId={id ?? ''} resource={resource} current={capability ?? ''} />
        )}
        <CapabilityContract
          specId={id ?? ''}
          schemaName={schemaName ?? ''}
          contract={contractData}
        />
      </div>
    </div>
  )
}
