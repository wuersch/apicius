// FEAT-009: the failure answers' plain-language names — UI vocabulary, keyed on the status
// (contract wording ≠ UI copy: the document carries the neutral shared descriptions). The
// 404 is the one noun-phrased name; the noun travels in the contract projection.
export function failureName(status: string, singularNoun: string): string {
  switch (status) {
    case '400':
      return "we couldn't read the request"
    case '401':
      return 'not signed in'
    case '404':
      return `no ${singularNoun} with this id`
    case '422':
      return 'bad input'
    case '429':
      return 'too many requests'
    case '500':
      return 'our fault'
    default:
      return ''
  }
}
