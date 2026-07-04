// FEAT-002 AC2: the card's single-letter monogram tile, from the API title.

export function monogram(title: string): string {
  const match = title.match(/\p{Letter}|\p{Number}/u)
  return match ? match[0].toUpperCase() : '?'
}
