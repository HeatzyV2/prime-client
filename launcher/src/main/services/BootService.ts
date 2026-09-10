import { accountStore } from '../storage/AccountStore'
import { instanceStore } from '../storage/InstanceStore'
import { ecosystemStore } from '../storage/EcosystemStore'
import { settingsStore } from '../storage/SettingsStore'
import { downloadStore } from '../storage/DownloadStore'
import { updateService } from './UpdateService'
import { launchLogService } from './LaunchLogService'

export type BootProgress = {
  step: number
  total: number
  label: string
}

/** Real boot tasks — stores and optional update check. */
export class BootService {
  async initialize(onProgress?: (progress: BootProgress) => void): Promise<void> {
    const steps: Array<{ label: string; run: () => Promise<unknown> }> = [
      { label: 'Accounts', run: () => accountStore.load() },
      { label: 'Instances', run: () => instanceStore.load() },
      { label: 'Ecosystem', run: () => ecosystemStore.load() },
      { label: 'Settings', run: () => settingsStore.load() },
      { label: 'Downloads', run: () => downloadStore.load() }
    ]

    const total = steps.length + 1
    for (let i = 0; i < steps.length; i++) {
      onProgress?.({ step: i + 1, total, label: steps[i].label })
      await steps[i].run()
    }

    onProgress?.({ step: total, total, label: 'Updates' })
    void launchLogService.purgeOldLogs()
    const settings = await settingsStore.load()
    if (settings.autoUpdate) {
      try {
        await updateService.check()
      } catch {
        // offline — continue boot
      }
    }
  }
}

export const bootService = new BootService()
