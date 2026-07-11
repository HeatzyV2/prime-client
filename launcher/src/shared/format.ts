import type { AccountTier, NewsItem } from './types'
import type { Locale } from './i18n'

export function formatTier(tier: AccountTier | string, locale: Locale = 'en'): string {
  const labels: Record<Locale, Record<string, string>> = {
    en: {
      free: 'Free',
      prime: 'Prime',
      prime_plus: 'Prime Plus'
    },
    fr: {
      free: 'Gratuit',
      prime: 'Prime',
      prime_plus: 'Prime Plus'
    }
  }
  return labels[locale][tier] ?? tier
}

export function formatNewsTag(tag: NewsItem['tag']): string {
  const labels: Record<NewsItem['tag'], string> = {
    update: 'Update',
    event: 'Event',
    announcement: 'Announcement'
  }
  return labels[tag]
}

export function formatLoader(loader: string): string {
  if (loader === 'neoforge') return 'NeoForge'
  return loader.charAt(0).toUpperCase() + loader.slice(1)
}

export function playerHeadUrl(uuid: string | undefined, username: string, pixelSize: number): string {
  const id = uuid ? uuid.replace(/-/g, '') : encodeURIComponent(username)
  return `https://mc-heads.net/avatar/${id}/${pixelSize}`
}

export function isSkinTextureUrl(url: string): boolean {
  return url.includes('textures.minecraft.net') || url.includes('/texture/')
}
