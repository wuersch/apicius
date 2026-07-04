// FEAT-001 AC6: avatar initials are derived from the token's name claims, never stored.
type NameClaims = {
  given_name?: string
  family_name?: string
  name?: string
  preferred_username?: string
  email?: string
}

export function getInitials(profile: NameClaims | undefined | null): string {
  if (!profile) return '?'

  const given = nonBlank(profile.given_name)
  const family = nonBlank(profile.family_name)
  if (given && family) return (given[0] + family[0]).toUpperCase()

  const name = nonBlank(profile.name)
  if (name) {
    const parts = name.split(/\s+/)
    const first = parts[0][0]
    return parts.length > 1 ? (first + parts[parts.length - 1][0]).toUpperCase() : first.toUpperCase()
  }

  const fallback = given ?? family ?? nonBlank(profile.preferred_username) ?? nonBlank(profile.email)
  return fallback ? fallback[0].toUpperCase() : '?'
}

// Address the current user by first name (the mockups' convention). Derived from the
// viewer's own OIDC profile — no server round-trip — with the canonical /users/me
// displayName as a fallback when the token carries no separate name claims.
export function getFirstName(
  profile: NameClaims | undefined | null,
  fallbackFullName?: string,
): string | undefined {
  const given = nonBlank(profile?.given_name)
  if (given) return given

  const fullName = nonBlank(profile?.name) ?? nonBlank(fallbackFullName)
  return fullName ? fullName.split(/\s+/)[0] : undefined
}

function nonBlank(value: string | undefined): string | undefined {
  const trimmed = value?.trim()
  return trimmed ? trimmed : undefined
}
