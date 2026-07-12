import { BrowserWindow } from 'electron'
import type { LauncherSettings } from '../storage/SettingsStore'
import { settingsStore } from '../storage/SettingsStore'
import { instanceService } from './InstanceService'
import { launcherBridgeService } from './LauncherBridgeService'
import { performanceService } from './PerformanceService'
import { launcherDiscordService } from './LauncherDiscordService'

export class SettingsService {
  async get(): Promise<LauncherSettings> {
    return settingsStore.load()
  }

  async update(partial: Partial<LauncherSettings>): Promise<{ settings: LauncherSettings; restartRequired?: boolean }> {
    const before = await settingsStore.load()
    const updated = await settingsStore.mutate((s) => {
      Object.assign(s, partial)
    })

    let restartRequired = false

    if (partial.discordRpc !== undefined) {
      await launcherDiscordService.setEnabled(partial.discordRpc)
      const instances = await instanceService.list()
      await Promise.all(
        instances.filter((inst) => inst.includePrimeMod).map((inst) => launcherBridgeService.syncToInstance(inst.id))
      )
    }

    if (partial.performancePreset !== undefined) {
      await performanceService.applyPreset(partial.performancePreset)
    }

    if (partial.hardwareAccel !== undefined && partial.hardwareAccel !== before.hardwareAccel) {
      restartRequired = true
    }

    if (partial.developerMode !== undefined) {
      const win = BrowserWindow.getAllWindows()[0]
      if (win) {
        if (partial.developerMode) {
          win.webContents.openDevTools({ mode: 'detach' })
        } else if (win.webContents.isDevToolsOpened()) {
          win.webContents.closeDevTools()
        }
      }
    }

    return { settings: updated, restartRequired }
  }
}

export const settingsService = new SettingsService()
