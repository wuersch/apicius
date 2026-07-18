import { useState, type FormEvent, type KeyboardEvent } from 'react'

// The canonical editor copy — contract wording ≠ UI copy, the DeclarationEditor stance.
const GHOST = 'add a note for readers…'
const HINT = 'Plain words — this becomes docs and SDK comments · Enter saves · Esc cancels'
const SAVE_FAILED = "That didn't save — try again."

// FEAT-012: the quiet description (state 3·7) — one grammar for every described element:
// the note reads as plain body copy and is edited in place; an element without one carries
// a faint ghost line inviting a note (always visible — a hover-only reveal hides the
// affordance entirely on an undescribed element). Enter saves, Shift+Enter breaks a line
// (a note is free prose), Esc cancels; a blank save is the clear gesture. Everything shown
// after a save is projected back through the caller's invalidation, nothing echoed locally.
export function QuietDescription({
  value,
  onSave,
  ariaLabel,
  className,
}: {
  /** The element's current description — the projection's text, absent for none. */
  value?: string | null
  /**
   * Persists the edit — `null` clears. Resolve closes the editor (the caller invalidates,
   * the projection re-renders); rejection keeps it open with the standing error copy.
   */
  onSave: (description: string | null) => Promise<unknown>
  ariaLabel: string
  /** The site's typography — text styles cascade into the note and its editor. */
  className?: string
}) {
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState('')
  const [saving, setSaving] = useState(false)
  const [failed, setFailed] = useState(false)

  function open() {
    setDraft(value ?? '')
    setFailed(false)
    setEditing(true)
  }

  async function save() {
    if (saving) return
    const trimmed = draft.trim()
    // An unchanged note is a cancel, not a mutation.
    if (trimmed === (value ?? '')) {
      setEditing(false)
      return
    }
    setSaving(true)
    setFailed(false)
    try {
      await onSave(trimmed === '' ? null : trimmed)
      setEditing(false)
    } catch {
      setFailed(true)
    } finally {
      setSaving(false)
    }
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    void save()
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    // An Enter that confirms an IME composition is the input method's, not ours.
    if (event.nativeEvent.isComposing) return
    if (event.key === 'Escape') setEditing(false)
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      void save()
    }
  }

  // The auto-growing gesture: the note is prose, the editor follows its height.
  function fitToContent(area: HTMLTextAreaElement) {
    area.style.height = 'auto'
    area.style.height = `${area.scrollHeight}px`
  }

  if (!editing) {
    return (
      <div className={className}>
        <button
          type="button"
          onClick={open}
          aria-label={`${value ? 'Edit' : 'Add'} ${ariaLabel}`}
          // bg-input, not bg-accent — accent sits within a hair of the page background, so
          // the tint would vanish everywhere but on cards; input is darker than both.
          className={`-mx-1 cursor-text rounded-sm px-1 text-left whitespace-pre-wrap transition-colors hover:bg-input ${
            value ? '' : 'text-text-faint'
          }`}
        >
          {value ?? GHOST}
        </button>
      </div>
    )
  }

  return (
    <div className={className}>
      <form onSubmit={handleSubmit} noValidate>
        <textarea
          value={draft}
          rows={1}
          autoFocus
          disabled={saving}
          ref={(area) => {
            if (area) fitToContent(area)
          }}
          onChange={(event) => {
            setDraft(event.target.value)
            fitToContent(event.target)
          }}
          onKeyDown={handleKeyDown}
          placeholder={GHOST}
          aria-label={ariaLabel}
          className="block w-full resize-none overflow-hidden rounded-[7px] border-[1.5px] border-control-border bg-control px-2 py-1 outline-none placeholder:text-text-faint focus-visible:border-olive focus-visible:ring-3 focus-visible:ring-olive/15"
        />
        {failed ? (
          <p role="alert" className="mt-1 text-[11.5px] text-terracotta">
            {SAVE_FAILED}
          </p>
        ) : (
          <p className="mt-1 text-[11.5px] text-hint">{HINT}</p>
        )}
      </form>
    </div>
  )
}
