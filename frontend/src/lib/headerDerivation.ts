// FEAT-011: the declaration mirror for live previews and inline pre-checks — header-name
// derivation, the reserved header names, and the "one of" value rules. The backend
// (HeaderNameDerivation / Declarations / ParameterKind.OneOf) is the only writer; this
// module never produces document content. Kept honest by the shared vectors in
// backend/src/test/resources/derivation/declaration-vectors.json, asserted by both test
// suites (headerDerivation.test.ts here, DeclarationVectorsTest there).

/**
 * Freeform name → the Hyphenated-Capitalized header name that is the declaration's identity:
 * spaces, hyphens, and underscores are segment boundaries; only ASCII letters and digits
 * survive (header names are RFC 9110 tokens — stripping is visible in the live preview);
 * each segment's first character is uppercased, the rest kept as typed. Returns '' when
 * nothing survives — the blocked AC6 state.
 */
export function deriveHeaderName(rawName: string): string {
  return rawName
    .split(/[ \-_]/)
    .map((segment) => segment.replace(/[^A-Za-z0-9]/g, ''))
    .filter((segment) => segment.length > 0)
    .map((segment) => segment[0].toUpperCase() + segment.slice(1))
    .join('-')
}

/**
 * The reserved header names (both header locations): Accept and Content-Type belong to
 * content negotiation, Authorization to the future security feature.
 */
export const RESERVED_HEADER_NAMES = ['Accept', 'Content-Type', 'Authorization']

/** The reservation, case-insensitive like every other name rule. */
export function isReservedHeaderName(headerName: string): boolean {
  return RESERVED_HEADER_NAMES.some(
    (reserved) => reserved.toLowerCase() === headerName.toLowerCase(),
  )
}

/** The "one of" normalization — trimming, mirroring ParameterKind.OneOf's constructor. */
export function normalizeOneOfValues(values: string[]): string[] {
  return values.map((value) => value.trim())
}

/**
 * The "one of" rules (AC7) on the normalized values: at least one, non-blank, distinct
 * (case-sensitively — values are payload, not names). Null means valid.
 */
export function oneOfValuesProblem(values: string[]): 'empty' | 'blank' | 'duplicate' | null {
  const normalized = normalizeOneOfValues(values)
  if (normalized.length === 0) return 'empty'
  if (normalized.some((value) => value === '')) return 'blank'
  if (new Set(normalized).size !== normalized.length) return 'duplicate'
  return null
}

/**
 * The editor's comma-separated values input, parsed: split, trim, drop empties — typing
 * "available, pending, sold" previews exactly the derived value list. Dropping empties is
 * input tolerance (trailing commas); validation runs on what survives.
 */
export function parseOneOfInput(input: string): string[] {
  return input
    .split(',')
    .map((value) => value.trim())
    .filter((value) => value.length > 0)
}
