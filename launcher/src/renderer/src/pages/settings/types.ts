import type { PerformancePreset } from '@shared/content-types'
import type { PrimeThemeId } from '@shared/ipc'

export const SECTION_IDS = [
  'general',
  'appearance',
  'minecraft',
  'performance',
  'accounts',
  'privacy',
  'downloads',
  'updates',
  'advanced'
] as const

export type SettingsSectionId = (typeof SECTION_IDS)[number]

export interface SettingsState {
  language: 'en' | 'fr'
  closeOnLaunch: boolean
  autoUpdate: boolean
  theme: PrimeThemeId
  backgroundNebula: boolean
  hardwareAccel: boolean
  performanceMode: boolean
  defaultRamMb: number
  defaultJavaPath: string | null
  performancePreset: PerformancePreset
  analytics: boolean
  discordRpc: boolean
  concurrentDownloads: number
  developerMode: boolean
  jvmArgs: string
  gameWidth: number
  gameHeight: number
  gameDisplayMode: 'windowed' | 'borderless' | 'fullscreen'
  wallpaperPath: string | null
  accentColor: string | null
  uiSounds: boolean
}

export type SettingsPatch = (partial: Partial<SettingsState>) => Promise<void>
