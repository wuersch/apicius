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

function nonBlank(value: string | undefined): string | undefined {
  const trimmed = value?.trim()
  return trimmed ? trimmed : undefined
}
