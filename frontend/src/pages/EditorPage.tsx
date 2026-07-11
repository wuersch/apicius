import { Link, useParams } from 'react-router'
import { useAuth } from 'react-oidc-context'
import { Plus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { NewResourceDialog } from '@/components/editor/NewResourceDialog'
import { ResourceCard } from '@/components/editor/ResourceCard'
import { useGetSpec } from '@/api/endpoints/specs/specs'

// FEAT-005 AC8: the editor's minimal honest display — the API's identity, and either the
// plain statement that it has no resources yet (with creation offered) or the resources with
// their capabilities. Everything shown is projected from the document server-side; the full
// View-2 shell (nav sections, guidelines rail, counts footer) grows with later features.
export function EditorPage() {
  const { id } = useParams<{ id: string }>()
  const auth = useAuth()
  // Gated on the token so no query fires before auth is wired (FEAT-001 pattern).
  const enabled = Boolean(auth.user?.access_token) && Boolean(id)
  const { data: response, error, isPending } = useGetSpec(id ?? '', { query: { enabled } })

  const spec = response?.status === 200 ? response.data : undefined
  const notFound = (error as { status?: number } | null)?.status === 404

  if (notFound) {
    return (
      <div className="mx-auto w-full max-w-4xl px-11 py-8">
        <h1 className="text-2xl font-bold">This API doesn't exist</h1>
        <p className="mt-2 text-sm text-text-tertiary">
          It may have been removed.{' '}
          <Link to="/" className="font-semibold text-foreground underline-offset-2 hover:underline">
            Back to all APIs
          </Link>
        </p>
      </div>
    )
  }
  if (isPending || !spec) return null

  const resources = spec.resources ?? []
  const newResourceTrigger = (label: string) => (
    <Button className="h-[42px] shrink-0 rounded-md px-[18px] text-[13.5px] font-semibold">
      <Plus aria-hidden className="size-3.5" />
      {label}
    </Button>
  )

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-6 px-11 py-8">
      <header className="flex items-start justify-between gap-4">
        <div className="min-w-0">
          <div className="flex items-center gap-2.5">
            <h1 className="truncate text-3xl font-bold">{spec.title}</h1>
            <span className="shrink-0 rounded-sm bg-input px-2 py-0.5 font-mono text-[11px] text-text-tertiary">
              v{spec.apiVersion}
            </span>
          </div>
          {spec.description && (
            <p className="mt-1.5 max-w-xl text-sm text-text-tertiary">{spec.description}</p>
          )}
        </div>
        <NewResourceDialog
          specId={spec.id ?? ''}
          existingNames={resources.map((resource) => resource.name ?? '')}
          trigger={newResourceTrigger('New resource')}
        />
      </header>

      {resources.length ? (
        <section aria-label="Resources">
          <div className="flex items-baseline gap-2">
            <h2 className="text-[11px] font-semibold tracking-[.1em] text-text-tertiary uppercase">
              Resources
            </h2>
            <span className="font-mono text-[11px] text-text-faint">{resources.length}</span>
          </div>
          <div className="mt-2.5 flex flex-col gap-3.5">
            {resources.map((resource) => (
              <ResourceCard key={resource.name} resource={resource} />
            ))}
          </div>
        </section>
      ) : (
        // AC8, empty half: state it plainly, offer the first move.
        <section
          aria-label="Resources"
          className="flex flex-col items-center gap-3 rounded-xl bg-card px-8 py-14 text-center shadow-sm"
        >
          <h2 className="text-lg font-bold">This API has no resources yet</h2>
          <p className="max-w-sm text-sm text-text-tertiary">
            Start with the first noun your API is about — a Product, an Order, a Customer. Its
            operations are derived for you.
          </p>
          <div className="mt-2">
            <NewResourceDialog
              specId={spec.id ?? ''}
              existingNames={[]}
              trigger={newResourceTrigger('Create your first resource')}
            />
          </div>
        </section>
      )}
    </div>
  )
}
