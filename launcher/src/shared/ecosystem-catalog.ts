import type { CosmeticItem, PerformancePresetInfo, StoreItem } from './content-types'
import type { NewsItem } from './types'

/** Static store catalog — purchases unlock locally (Prime Coins, no payment server). */
export const STORE_CATALOG: StoreItem[] = [
  { id: 'cape-prime', name: 'Prime Cape', description: 'Official Prime Client cape.', price: 0, category: 'cosmetic', owned: false },
  { id: 'theme-crimson', name: 'Crimson Theme', description: 'Deep red launcher theme.', price: 250, category: 'theme', owned: false },
  { id: 'bg-nebula', name: 'Nebula Background', description: 'Animated space background.', price: 150, category: 'background', owned: false },
  { id: 'badge-founder', name: 'Founder Badge', description: 'Limited edition profile badge.', price: 500, category: 'badge', owned: false },
  { id: 'wings-ember', name: 'Ember Wings', description: 'Fiery cosmetic wings.', price: 400, category: 'cosmetic', owned: false },
  { id: 'pet-fox', name: 'Arctic Fox', description: 'Companion pet cosmetic.', price: 300, category: 'cosmetic', owned: false },
  { id: 'emote-wave', name: 'Prime Wave', description: 'Signature emote.', price: 100, category: 'cosmetic', owned: false }
]

export const COSMETIC_CATALOG: Omit<CosmeticItem, 'equipped'>[] = [
  { id: 'cape-prime', name: 'Prime Cape', type: 'cape', rarity: 'legendary' },
  { id: 'wings-ember', name: 'Ember Wings', type: 'wings', rarity: 'epic' },
  { id: 'pet-fox', name: 'Arctic Fox', type: 'pet', rarity: 'rare' },
  { id: 'emote-wave', name: 'Prime Wave', type: 'emote', rarity: 'common' },
  { id: 'badge-founder', name: 'Founder', type: 'badge', rarity: 'legendary' },
  { id: 'badge-veteran', name: 'Veteran', type: 'badge', rarity: 'rare' }
]

export const STORE_TO_COSMETIC: Record<string, string> = {
  'cape-prime': 'cape-prime',
  'wings-ember': 'wings-ember',
  'pet-fox': 'pet-fox',
  'emote-wave': 'emote-wave',
  'badge-founder': 'badge-founder'
}

export const BUNDLED_NEWS: NewsItem[] = [
  {
    id: 'n1',
    title: 'Prime Client v1.1 — Premium Update',
    summary: 'New title screen, Discord RPC, onboarding wizard, and 50 modules shipped.',
    date: '2026-07-11',
    tag: 'update'
  },
  {
    id: 'n2',
    title: 'Prime Launcher v0.8',
    summary: 'Local store, cosmetics, friends list, performance optimizer, and settings persistence.',
    date: '2026-07-11',
    tag: 'announcement'
  },
  {
    id: 'n3',
    title: 'Summer PvP Event (offline roster)',
    summary: 'Track scrim dates locally — add friends and notes from the Friends page.',
    date: '2026-07-08',
    tag: 'event'
  },
  {
    id: 'n4',
    title: 'Local sync only',
    summary: 'Prime profile and configs stay on this PC. No cloud account required.',
    date: '2026-07-01',
    tag: 'announcement'
  }
]

export const PERFORMANCE_PRESETS: PerformancePresetInfo[] = [
  { id: 'low', label: 'Low PC', ramMb: 2048, renderDistance: 8, description: 'Minimum settings for weak hardware.' },
  { id: 'balanced', label: 'Balanced', ramMb: 4096, renderDistance: 12, description: 'Recommended for most players.' },
  { id: 'performance', label: 'Performance', ramMb: 6144, renderDistance: 16, description: 'High FPS competitive setup.' },
  { id: 'ultra', label: 'Ultra', ramMb: 8192, renderDistance: 24, description: 'Maximum quality for powerful PCs.' }
]

export const DEFAULT_OWNED_STORE = ['cape-prime']
export const DEFAULT_EQUIPPED_COSMETICS = ['cape-prime', 'badge-veteran']
