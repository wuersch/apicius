// FEAT-002 AC4: first run, no APIs — the Create and Import entry points as ghost tiles
// (the mockup's onboarding state). Deliberately inert: FEAT-003/004 wire them up.
export function EmptyState() {
  return (
    <div className="grid gap-4 py-6 sm:grid-cols-2">
      <GhostTile title="Start from scratch" hint="Name a resource, we scaffold the rest." />
      <GhostTile title="Import a spec" hint="Bring an existing OpenAPI file, losslessly." />
    </div>
  )
}

function GhostTile({ title, hint }: { title: string; hint: string }) {
  return (
    <div className="flex flex-col items-center justify-center gap-1 rounded-lg border-2 border-dashed border-ghost px-6 py-10 text-center">
      <p className="text-[15px] font-bold text-text-secondary">{title}</p>
      <p className="text-[13px] text-text-tertiary">{hint}</p>
    </div>
  )
}
