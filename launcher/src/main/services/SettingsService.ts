import type { LauncherSettings } from '../storage/SettingsStore'
import { settingsStore } from '../storage/SettingsStore'

export class SettingsService {
  async get(): Promise<LauncherSettings> {
    return settingsStore.load()
  }

  async update(partial: Partial<LauncherSettings>): Promise<LauncherSettings> {
    return settingsStore.mutate((s) => {
      Object.assign(s, partial)
    })
  }
}

export const settingsService = new SettingsService()
