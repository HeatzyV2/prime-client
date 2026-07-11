import { app } from 'electron'
import { mkdir, readFile, writeFile } from 'fs/promises'
import { join } from 'path'
import type { PerformancePreset } from '../../shared/content-types'

export interface LauncherSettings {
  version: 1
  language: 'en' | 'fr'
  closeOnLaunch: boolean
  autoUpdate: boolean
  theme: 'prime-dark' | 'prime-crimson'
  hardwareAccel: boolean
  defaultRamMb: number
  performancePreset: PerformancePreset
  analytics: boolean
  discordRpc: boolean
  concurrentDownloads: number
  developerMode: boolean
  jvmArgs: string[]
  lastUpdateCheck?: string
}

const DEFAULT_SETTINGS = (): LauncherSettings => ({
  version: 1,
  language: 'en',
  closeOnLaunch: true,
  autoUpdate: true,
  theme: 'prime-dark',
  hardwareAccel: true,
  defaultRamMb: 4096,
  performancePreset: 'balanced',
  analytics: false,
  discordRpc: true,
  concurrentDownloads: 3,
  developerMode: false,
  jvmArgs: ['-XX:+UseG1GC']
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
