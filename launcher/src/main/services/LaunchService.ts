import { BrowserWindow } from 'electron'
import { accountService } from './AccountService'
import { minecraftEngine } from '../minecraft/MinecraftEngine'
import { emitLaunchProgress } from '../minecraft/launchProgress'
import { settingsStore } from '../storage/SettingsStore'
import type { LaunchResult } from '../storage/account-types'

export class LaunchService {
  async launch(instanceId: string): Promise<LaunchResult> {
    const prep = await accountService.prepareLaunch(instanceId)
    if (!prep.ok) {
      return prep
    }

    try {
      const result = await minecraftEngine.launchInstance(instanceId)
      const settings = await settingsStore.load()
      if (settings.closeOnLaunch) {
        for (const win of BrowserWindow.getAllWindows()) {
          win.minimize()
        }
      }
      return {
        ok: true,
        message: `Minecraft started as ${result.username}.`
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Launch failed.'
      emitLaunchProgress({ phase: 'log', detail: message })
      return { ok: false, message, error: 'LAUNCH_FAILED' }
    }
  }
}

export const launchService = new LaunchService()
