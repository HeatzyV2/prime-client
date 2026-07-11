export type AccountTier = 'free' | 'prime' | 'prime_plus'

export interface PrimeAccount {
  id: string
  username: string
  tier: AccountTier
  level: number
  avatarUrl?: string
  createdAt: string
}

export interface MinecraftAccount {
  id: string
  type: 'microsoft' | 'offline'
  username: string
  uuid: string
  skinUrl?: string
  capeUrl?: string
}

export interface LauncherProfile {
  id: string
  name: string
  minecraftAccountId: string
  instanceId: string
  lastPlayed?: string
  playTimeMinutes: number
}

export interface GameInstance {
  id: string
  name: string
  minecraftVersion: string
  loader: 'vanilla' | 'fabric' | 'forge' | 'neoforge'
  ramMb: number
  javaPath?: string
  jvmArgs: string[]
  modCount: number
  isDefault?: boolean
  includePrimeMod?: boolean
  fabricLoaderVersion?: string
  fabricApiVersion?: string
  createdAt?: string
  lastPlayed?: string
}

export interface NewsItem {
  id: string
  title: string
  summary: string
  date: string
  tag: 'update' | 'event' | 'announcement'
}

export interface FavoriteServer {
  id: string
  name: string
  address: string
  players?: number
  maxPlayers?: number
  ping?: number
}

export interface BootStep {
  id: string
  label: string
}

export const BOOT_STEPS: BootStep[] = [
  { id: 'core', label: 'Initializing Prime Core...' },
  { id: 'updates', label: 'Checking Updates...' },
  { id: 'minecraft', label: 'Loading Minecraft...' },
  { id: 'ready', label: 'Ready.' }
]

export type NavSection =
  | 'dashboard'
  | 'accounts'
  | 'instances'
  | 'mods'
  | 'resources'
  | 'shaders'
  | 'store'
  | 'cosmetics'
  | 'servers'
  | 'friends'
  | 'news'
  | 'media'
  | 'performance'
  | 'downloads'
  | 'settings'

export interface NavItem {
  id: NavSection
  label: string
  phase: number
}

export const NAV_ITEMS: NavItem[] = [
  { id: 'dashboard', label: 'Home', phase: 3 },
  { id: 'accounts', label: 'Accounts', phase: 3 },
  { id: 'instances', label: 'Instances', phase: 3 },
  { id: 'mods', label: 'Mods', phase: 3 },
  { id: 'resources', label: 'Resource Packs', phase: 3 },
  { id: 'shaders', label: 'Shaders', phase: 3 },
  { id: 'store', label: 'Prime Store', phase: 3 },
  { id: 'cosmetics', label: 'Cosmetics', phase: 3 },
  { id: 'servers', label: 'Servers', phase: 3 },
  { id: 'friends', label: 'Friends', phase: 3 },
  { id: 'news', label: 'News', phase: 3 },
  { id: 'media', label: 'Media', phase: 3 },
  { id: 'performance', label: 'Performance', phase: 3 },
  { id: 'downloads', label: 'Downloads', phase: 3 },
  { id: 'settings', label: 'Settings', phase: 3 }
]
