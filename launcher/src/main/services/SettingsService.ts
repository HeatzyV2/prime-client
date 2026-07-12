import { BrowserWindow, dialog } from 'electron'
import type { LauncherSettings } from '../storage/SettingsStore'
import { settingsStore } from '../storage/SettingsStore'
import { instanceService } from './InstanceService'
import { launcherBridgeService } from './LauncherBridgeService'
import { performanceService } from './PerformanceService'
import { launcherDiscordService } from './LauncherDiscordService'
import { validateJavaExecutable, type JavaInstallation } from '../minecraft/JavaService'

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

  async browseJavaExecutable(): Promise<{ ok: boolean; install?: JavaInstallation; error?: string }> {
    const { canceled, filePaths } = await dialog.showOpenDialog({
      title: 'Select Java executable',
      properties: ['openFile'],
      filters:
        process.platform === 'win32'
          ? [{ name: 'Java executable', extensions: ['exe'] }]
          : [{ name: 'Java executable', extensions: ['*'] }]
    })

    if (canceled || !filePaths[0]) {
      return { ok: false, error: 'Cancelled.' }
    }

    const install = await validateJavaExecutable(filePaths[0])
    if (!install) {
      return { ok: false, error: 'Selected file is not a valid Java 21+ executable.' }
    }

    return { ok: true, install }
  }

  async addCustomJavaPath(javaPath: string): Promise<{ ok: boolean; install?: JavaInstallation; error?: string }> {
    const install = await validateJavaExecutable(javaPath)
    if (!install) {
      return { ok: false, error: 'Selected file is not a valid Java 21+ executable.' }
    }

    await settingsStore.mutate((settings) => {
      const paths = settings.customJavaPaths ?? []
      if (!paths.includes(install.path)) {
        settings.customJavaPaths = [...paths, install.path]
      }
      settings.defaultJavaPath = install.path
    })

    return { ok: true, install }
  }
}

export const settingsService = new SettingsService()
