// The settled method-color semantics (mockup v3+): olive = safe read, teal = create,
// ochre = change, terracotta = remove. Shared by the dialog rows and the resource cards.
export const METHOD_DOT_CLASS: Record<string, string> = {
  GET: 'bg-olive',
  POST: 'bg-teal',
  PATCH: 'bg-ochre',
  DELETE: 'bg-terracotta',
}
