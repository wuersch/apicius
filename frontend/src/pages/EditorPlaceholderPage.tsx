import { useParams } from 'react-router'

// FEAT-002 AC3's destination: cards navigate here; the editor feature fills this route in.
export function EditorPlaceholderPage() {
  const { id } = useParams<{ id: string }>()
  return (
    <div className="mx-auto w-full max-w-4xl px-11 py-8">
      <h1 className="text-2xl font-bold">Editor</h1>
      <p className="mt-2 text-sm text-text-tertiary">
        The editor arrives with a future feature. API <span className="font-mono">{id}</span>
      </p>
    </div>
  )
}
