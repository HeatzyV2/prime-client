import { app } from 'electron'
import { mkdir, readFile, writeFile } from 'fs/promises'
import { join } from 'path'
import type { PerformancePreset } from '../../shared/content-types'

export type GameDisplayMode = 'windowed' | 'borderless' | 'fullscreen'

export interface LauncherSettings {
  version: 1
  language: 'en' | 'fr'
  closeOnLaunch: boolean
  autoUpdate: boolean
  theme: 'prime-dark' | 'prime-crimson'
  backgroundNebula: boolean
  hardwareAccel: boolean
  defaultRamMb: number
  performancePreset: PerformancePreset
  analytics: boolean
  discordRpc: boolean
  curseForgeApiKey?: string
  concurrentDownloads: number
  developerMode: boolean
  jvmArgs: string[]
  defaultJavaPath: string | null
  customJavaPaths: string[]
  gameWidth: number
  gameHeight: number
  gameDisplayMode: GameDisplayMode
  lastUpdateCheck?: string
  lastPrimeSync?: string
  /** Suppresses home-screen update banner until newer versions appear. */
  dismissedUpdateBanner?: string
}

const DEFAULT_SETTINGS = (): LauncherSettings => ({
  version: 1,
  language: 'en',
  closeOnLaunch: false,
  autoUpdate: true,
  theme: 'prime-dark',
  backgroundNebula: false,
  hardwareAccel: true,
  defaultRamMb: 4096,
  performancePreset: 'balanced',
  analytics: false,
  discordRpc: true,
  concurrentDownloads: 3,
  developerMode: false,
  jvmArgs: ['-XX:+UseG1GC'],
  defaultJavaPath: null,
  customJavaPaths: [],
  gameWidth: 1920,
  gameHeight: 1080,
  gameDisplayMode: 'windowed'
})

export class SettingsStore {
  private settings: LauncherSettings | null = null

  private get path(): string {
    return join(app.getPath('userData'), 'settings.json')
  }

  async load(): Promise<LauncherSettings> {
    if (this.settings) {
      return this.settings
    }
    try {
      const raw = await readFile(this.path, 'utf8')
      this.settings = { ...DEFAULT_SETTINGS(), ...(JSON.parse(raw) as LauncherSettings) }
    } catch {
      this.settings = DEFAULT_SETTINGS()
      await this.save()
    }
    return this.settings!
  }

  async save(): Promise<void> {
    if (!this.settings) {
      return
    }
    await mkdir(app.getPath('userData'), { recursive: true })
    await writeFile(this.path, JSON.stringify(this.settings, null, 2), 'utf8')
  }

  async mutate(fn: (s: LauncherSettings) => void): Promise<LauncherSettings> {
    const settings = await this.load()
    fn(settings)
    await this.save()
    return settings
  }
}

export const settingsStore = new SettingsStore()
