import { BrowserWindow } from 'electron'
import { accountService } from './AccountService'
import { minecraftEngine } from '../minecraft/MinecraftEngine'
import { emitLaunchProgress } from '../minecraft/launchProgress'
import { parseServerAddress } from './ServerService'
import { performanceService } from './PerformanceService'
import { launcherBridgeService } from './LauncherBridgeService'
import { analyticsService } from './AnalyticsService'
import { storeService } from './StoreService'
import { settingsStore } from '../storage/SettingsStore'
import type { LaunchResult } from '../storage/account-types'

export class LaunchService {
  async launch(instanceId: string, serverAddress?: string): Promise<LaunchResult> {
    const prep = await accountService.prepareLaunch(instanceId)
    if (!prep.ok) {
      return prep
    }

    try {
      const settings = await settingsStore.load()
      await performanceService.applyPreset(settings.performancePreset, instanceId)
      await launcherBridgeService.syncToInstance(instanceId)

      const joinServer = serverAddress ? parseServerAddress(serverAddress) : undefined
      const result = await minecraftEngine.launchInstance(instanceId, joinServer)

      await storeService.grantLaunchReward()
      await analyticsService.track('game_launch', { instanceId, username: result.username })

      if (settings.closeOnLaunch) {
        for (const win of BrowserWindow.getAllWindows()) {
          win.hide()
        }
      }

      const joinHint = serverAddress ? ` → ${serverAddress}` : ''
      return {
        ok: true,
        message: `Minecraft started as ${result.username}${joinHint}.`
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Launch failed.'
      emitLaunchProgress({ phase: 'log', detail: message })
      await analyticsService.track('launch_failed', { instanceId, message })
      return { ok: false, message, error: 'LAUNCH_FAILED' }
    }
  }
}

export const launchService = new LaunchService()
