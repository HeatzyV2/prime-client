import type { CosmeticItem, PerformancePresetInfo, StoreItem } from './content-types'
import type { NewsItem } from './types'

/** Static store catalog — Cosmetics Update 2.2 — all unlocked for Prime. */
export const STORE_CATALOG: StoreItem[] = [
  { id: 'cape-prime-classic', name: 'Prime Classic', description: 'Signature blue Prime cape.', price: 0, category: 'cosmetic', owned: true },
  { id: 'cape-prime-founder', name: 'Prime Founder', description: 'Founder exclusive cape.', price: 0, category: 'cosmetic', owned: true },
  { id: 'cape-prime-neon', name: 'Prime Neon', description: 'Electric neon cape.', price: 0, category: 'cosmetic', owned: true },
  { id: 'cape-prime-shadow', name: 'Prime Shadow', description: 'Void shadow cape.', price: 0, category: 'cosmetic', owned: true },
  { id: 'cape-prime', name: 'Prime Cape', description: 'Legacy alias of Classic.', price: 0, category: 'cosmetic', owned: true },
  { id: 'cape-star', name: 'Star Cape', description: 'Gold star cape.', price: 0, category: 'cosmetic', owned: true },
  { id: 'cape-crimson', name: 'Crimson Cape', description: 'Signature crimson cape.', price: 0, category: 'cosmetic', owned: true },
  { id: 'cape-midnight', name: 'Midnight Cape', description: 'Indigo midnight cape.', price: 0, category: 'cosmetic', owned: true },
  { id: 'wings-inferno', name: 'Inferno Wings', description: 'Blazing inferno wings.', price: 0, category: 'cosmetic', owned: true },
  { id: 'wings-shadow', name: 'Shadow Wings', description: 'Dark ethereal wings.', price: 0, category: 'cosmetic', owned: true },
  { id: 'wings-galaxy', name: 'Galaxy Wings', description: 'Cosmic galaxy wings.', price: 0, category: 'cosmetic', owned: true },
  { id: 'wings-prime', name: 'Prime Wings', description: 'Official Prime wings.', price: 0, category: 'cosmetic', owned: true },
  { id: 'wings-aurora', name: 'Aurora Wings', description: 'Animated aurora wings.', price: 0, category: 'cosmetic', owned: true },
  { id: 'wings-ember', name: 'Ember Wings', description: 'Animated fiery wings.', price: 0, category: 'cosmetic', owned: true },
  { id: 'aura-prime-energy', name: 'Prime Energy', description: 'Blue energy aura.', price: 0, category: 'cosmetic', owned: true },
  { id: 'aura-fire', name: 'Fire Aura', description: 'Flame particles.', price: 0, category: 'cosmetic', owned: true },
  { id: 'aura-void', name: 'Void Aura', description: 'Dark void swirl.', price: 0, category: 'cosmetic', owned: true },
  { id: 'aura-lightning', name: 'Lightning Aura', description: 'Electric sparks.', price: 0, category: 'cosmetic', owned: true },
  { id: 'aura-royal', name: 'Royal Aura', description: 'Golden royal glow.', price: 0, category: 'cosmetic', owned: true },
  { id: 'trail-flame', name: 'Flame Trail', description: 'Fire trail while moving.', price: 0, category: 'cosmetic', owned: true },
  { id: 'trail-star', name: 'Star Trail', description: 'Sparkling star trail.', price: 0, category: 'cosmetic', owned: true },
  { id: 'trail-rainbow', name: 'Rainbow Trail', description: 'Rainbow particle trail.', price: 0, category: 'cosmetic', owned: true },
  { id: 'trail-shadow', name: 'Shadow Trail', description: 'Dark smoke trail.', price: 0, category: 'cosmetic', owned: true },
  { id: 'trail-prime', name: 'Prime Trail', description: 'Prime blue trail.', price: 0, category: 'cosmetic', owned: true },
  { id: 'hat-crown', name: 'Prime Crown', description: 'Royal crown hat.', price: 0, category: 'cosmetic', owned: true },
  { id: 'hat-horns', name: 'Dragon Horns', description: 'Dragon horn headpiece.', price: 0, category: 'cosmetic', owned: true },
  { id: 'hat-wizard', name: 'Wizard Hat', description: 'Classic wizard hat.', price: 0, category: 'cosmetic', owned: true },
  { id: 'hat-santa', name: 'Santa Hat', description: 'Festive santa hat.', price: 0, category: 'cosmetic', owned: true },
  { id: 'hat-dev', name: 'Developer Cap', description: 'Dev-only style cap.', price: 0, category: 'cosmetic', owned: true },
  { id: 'emote-wave', name: 'Wave', description: 'Friendly wave.', price: 0, category: 'cosmetic', owned: true },
  { id: 'emote-dance', name: 'Dance', description: 'Dance loop.', price: 0, category: 'cosmetic', owned: true },
  { id: 'emote-sit', name: 'Sit', description: 'Sit pose.', price: 0, category: 'cosmetic', owned: true },
  { id: 'emote-laugh', name: 'Laugh', description: 'Laugh animation.', price: 0, category: 'cosmetic', owned: true },
  { id: 'emote-cry', name: 'Cry', description: 'Sad cry.', price: 0, category: 'cosmetic', owned: true },
  { id: 'emote-flex', name: 'Flex', description: 'Flex muscles.', price: 0, category: 'cosmetic', owned: true },
  { id: 'emote-clap', name: 'Clap', description: 'Applause.', price: 0, category: 'cosmetic', owned: true },
  { id: 'emote-sleep', name: 'Sleep', description: 'Sleep pose.', price: 0, category: 'cosmetic', owned: true },
  { id: 'emote-victory', name: 'Victory', description: 'Victory celebration.', price: 0, category: 'cosmetic', owned: true },
  { id: 'theme-crimson', name: 'Crimson Theme', description: 'Signature red Prime theme.', price: 0, category: 'theme', owned: true },
  { id: 'theme-midnight', name: 'Midnight Theme', description: 'Cool indigo theme.', price: 0, category: 'theme', owned: true },
  { id: 'theme-aurora', name: 'Aurora Theme', description: 'Cyan aurora theme.', price: 0, category: 'theme', owned: true },
  { id: 'theme-obsidian', name: 'Obsidian Theme', description: 'Black & champagne gold.', price: 0, category: 'theme', owned: true },
  { id: 'theme-ember', name: 'Ember Theme', description: 'Copper glow on charcoal.', price: 0, category: 'theme', owned: true },
  { id: 'bg-nebula', name: 'Nebula Background', description: 'Animated space background.', price: 0, category: 'background', owned: true },
  { id: 'badge-founder', name: 'Founder', description: 'Early adopter badge.', price: 0, category: 'badge', owned: true },
  { id: 'badge-creator', name: 'Creator', description: 'Content creator badge.', price: 0, category: 'badge', owned: true },
  { id: 'badge-partner', name: 'Partner', description: 'Partner server badge.', price: 0, category: 'badge', owned: true },
  { id: 'badge-supporter', name: 'Supporter', description: 'Community supporter.', price: 0, category: 'badge', owned: true }
]

export const COSMETIC_CATALOG: Omit<CosmeticItem, 'equipped'>[] = [
  { id: 'cape-prime-classic', name: 'Prime Classic', type: 'cape', rarity: 'legendary' },
  { id: 'cape-prime-founder', name: 'Prime Founder', type: 'cape', rarity: 'prime_exclusive' },
  { id: 'cape-prime-neon', name: 'Prime Neon', type: 'cape', rarity: 'mythic' },
  { id: 'cape-prime-shadow', name: 'Prime Shadow', type: 'cape', rarity: 'epic' },
  { id: 'cape-star', name: 'Star Cape', type: 'cape', rarity: 'epic' },
  { id: 'cape-crimson', name: 'Crimson Cape', type: 'cape', rarity: 'epic' },
  { id: 'cape-midnight', name: 'Midnight Cape', type: 'cape', rarity: 'rare' },
  { id: 'wings-inferno', name: 'Inferno Wings', type: 'wings', rarity: 'mythic' },
  { id: 'wings-shadow', name: 'Shadow Wings', type: 'wings', rarity: 'epic' },
  { id: 'wings-galaxy', name: 'Galaxy Wings', type: 'wings', rarity: 'legendary' },
  { id: 'wings-prime', name: 'Prime Wings', type: 'wings', rarity: 'legendary' },
  { id: 'wings-aurora', name: 'Aurora Wings', type: 'wings', rarity: 'epic' },
  { id: 'wings-ember', name: 'Ember Wings', type: 'wings', rarity: 'legendary' },
  { id: 'aura-prime-energy', name: 'Prime Energy', type: 'aura', rarity: 'legendary' },
  { id: 'aura-fire', name: 'Fire Aura', type: 'aura', rarity: 'epic' },
  { id: 'aura-void', name: 'Void Aura', type: 'aura', rarity: 'mythic' },
  { id: 'aura-lightning', name: 'Lightning Aura', type: 'aura', rarity: 'epic' },
  { id: 'aura-royal', name: 'Royal Aura', type: 'aura', rarity: 'prime_exclusive' },
  { id: 'trail-flame', name: 'Flame Trail', type: 'trail', rarity: 'epic' },
  { id: 'trail-star', name: 'Star Trail', type: 'trail', rarity: 'rare' },
  { id: 'trail-rainbow', name: 'Rainbow Trail', type: 'trail', rarity: 'legendary' },
  { id: 'trail-shadow', name: 'Shadow Trail', type: 'trail', rarity: 'epic' },
  { id: 'trail-prime', name: 'Prime Trail', type: 'trail', rarity: 'legendary' },
  { id: 'hat-crown', name: 'Prime Crown', type: 'hat', rarity: 'legendary' },
  { id: 'hat-horns', name: 'Dragon Horns', type: 'hat', rarity: 'epic' },
  { id: 'hat-wizard', name: 'Wizard Hat', type: 'hat', rarity: 'rare' },
  { id: 'hat-santa', name: 'Santa Hat', type: 'hat', rarity: 'common' },
  { id: 'hat-dev', name: 'Developer Cap', type: 'hat', rarity: 'prime_exclusive' },
  { id: 'emote-wave', name: 'Wave', type: 'emote', rarity: 'common' },
  { id: 'emote-dance', name: 'Dance', type: 'emote', rarity: 'rare' },
  { id: 'emote-sit', name: 'Sit', type: 'emote', rarity: 'common' },
  { id: 'emote-laugh', name: 'Laugh', type: 'emote', rarity: 'rare' },
  { id: 'emote-cry', name: 'Cry', type: 'emote', rarity: 'common' },
  { id: 'emote-flex', name: 'Flex', type: 'emote', rarity: 'epic' },
  { id: 'emote-clap', name: 'Clap', type: 'emote', rarity: 'common' },
  { id: 'emote-sleep', name: 'Sleep', type: 'emote', rarity: 'rare' },
  { id: 'emote-victory', name: 'Victory', type: 'emote', rarity: 'legendary' },
  { id: 'badge-founder', name: 'Founder', type: 'badge', rarity: 'prime_exclusive' },
  { id: 'badge-creator', name: 'Creator', type: 'badge', rarity: 'legendary' },
  { id: 'badge-partner', name: 'Partner', type: 'badge', rarity: 'epic' },
  { id: 'badge-supporter', name: 'Supporter', type: 'badge', rarity: 'rare' }
]

export const STORE_TO_COSMETIC: Record<string, string> = Object.fromEntries(
  COSMETIC_CATALOG.map((c) => [c.id, c.id])
)

export const BUNDLED_NEWS: NewsItem[] = [
  {
    id: 'n-cosmetics-220',
    title: 'Prime Cosmetics Update 2.2',
    summary: 'Capes, wings, auras, trails, hats, emotes, badges & collection — all unlocked for Prime.',
    date: '2026-07-31',
    tag: 'update'
  },
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

export const DEFAULT_OWNED_STORE = STORE_CATALOG.filter((i) => i.price === 0).map((i) => i.id)
export const DEFAULT_EQUIPPED_COSMETICS = ['cape-prime-classic', 'wings-prime']
