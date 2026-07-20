/** Shared theme IDs between launcher and in-game client. */
export type PrimeThemeId = 'prime-crimson' | 'prime-midnight' | 'prime-aurora'

export const PRIME_THEMES: readonly PrimeThemeId[] = [
  'prime-crimson',
  'prime-midnight',
  'prime-aurora'
] as const

/** Maps legacy settings / profile values onto the current trio. */
export function normalizePrimeTheme(id: string | null | undefined): PrimeThemeId {
  switch (id) {
    case 'prime-midnight':
    case 'prime-aurora':
    case 'prime-crimson':
      return id
    case 'prime-light':
      return 'prime-midnight'
    case 'prime-dark':
    default:
      return 'prime-crimson'
  }
}
