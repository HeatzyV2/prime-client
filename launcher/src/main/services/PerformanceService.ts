import { execFile } from 'child_process'
import { cpus, totalmem } from 'os'
import { promisify } from 'util'
import type { HardwareProfile, PerformancePreset } from '../../shared/content-types'
import { PERFORMANCE_PRESETS } from '../../shared/ecosystem-catalog'
import { readOptionsLines, writeOptionsLines } from '../content/options'
import { mergePresetGameOptions, presetGameOptions } from '../content/optionsMerge'
import { instanceService } from './InstanceService'
import { launcherBridgeService } from './LauncherBridgeService'
import { profileService } from './ProfileService'
import { settingsStore } from '../storage/SettingsStore'

const execFileAsync = promisify(execFile)

async function detectGpuWindows(): Promise<string> {
  try {
    const { stdout } = await execFileAsync('wmic', ['path', 'win32_VideoController', 'get', 'name'], {
      timeout: 5000
    })
    const lines = stdout
      .split('\n')
      .map((l) => l.trim())
      .filter((l) => l && l !== 'Name')
    return lines[0] ?? 'Unknown GPU'
  } catch {
    return 'Unknown GPU'
  }
}

async function detectGpu(): Promise<string> {
  if (process.platform === 'win32') {
    return detectGpuWindows()
  }
  return process.env['GPU_DEVICE'] ?? 'Unknown GPU'
}

export class PerformanceService {
  async getHardware(): Promise<HardwareProfile> {
    const cpuList = cpus()
    const cpu = cpuList[0]?.model?.trim() ?? 'Unknown CPU'
    const gpu = await detectGpu()
    const ramGb = Math.round(totalmem() / (1024 * 1024 * 1024))
    return { cpu, gpu, ramGb }
  }

  getPresets() {
    return PERFORMANCE_PRESETS
  }

  async getSelectedPreset(): Promise<PerformancePreset> {
    const settings = await settingsStore.load()
    return settings.performancePreset
  }

  /**
   * Explicit user action (Performance page / settings). Overwrites game options.
   * Do not call this on every launch — that resets FPS and related options.
   */
  async applyPreset(presetId: PerformancePreset, instanceId?: string): Promise<{ ok: boolean; error?: string }> {
    const preset = PERFORMANCE_PRESETS.find((p) => p.id === presetId)
    if (!preset) {
      return { ok: false, error: 'Unknown preset.' }
    }

    const targetId = await this.resolveInstanceId(instanceId)
    if (!targetId) {
      return { ok: false, error: 'No instance to optimize.' }
    }

    const settings = await settingsStore.load()
    const jvmArgs = [...new Set([...settings.jvmArgs, '-XX:+UseG1GC'])]

    await instanceService.update({
      id: targetId,
      ramMb: Math.min(preset.ramMb, Math.floor((await this.getHardware()).ramGb * 1024 * 0.75)),
      jvmArgs
    })

    try {
      let lines = await readOptionsLines(targetId)
      lines = mergePresetGameOptions(lines, presetGameOptions(preset.id, preset.renderDistance), 'overwrite')
      await writeOptionsLines(targetId, lines)
    } catch (err) {
      return {
        ok: false,
        error: err instanceof Error ? err.message : 'Could not update options.txt'
      }
    }

    await settingsStore.mutate((s) => {
      s.performancePreset = presetId
      s.defaultRamMb = preset.ramMb
    })

    const bridge = await launcherBridgeService.syncToInstance(targetId)
    if (!bridge.ok) {
      return { ok: false, error: bridge.error ?? 'Bridge sync failed.' }
    }

    return { ok: true }
  }

  /**
   * Launch-safe: seed missing performance keys only (new instance / empty options).
   * Never overwrites maxFps, render distance, graphics, or any other existing value.
   */
  async seedPresetOptionsIfNeeded(
    presetId: PerformancePreset,
    instanceId: string
  ): Promise<{ ok: boolean; seeded: boolean; error?: string }> {
    const preset = PERFORMANCE_PRESETS.find((p) => p.id === presetId)
    if (!preset) {
      return { ok: false, seeded: false, error: 'Unknown preset.' }
    }

    try {
      const before = await readOptionsLines(instanceId)
      const after = mergePresetGameOptions(
        before,
        presetGameOptions(preset.id, preset.renderDistance),
        'fill-absent'
      )
      if (after === before || linesEqual(before, after)) {
        return { ok: true, seeded: false }
      }
      await writeOptionsLines(instanceId, after)
      return { ok: true, seeded: true }
    } catch (err) {
      return {
        ok: false,
        seeded: false,
        error: err instanceof Error ? err.message : 'Could not seed options.txt'
      }
    }
  }

  private async resolveInstanceId(instanceId?: string): Promise<string | undefined> {
    if (instanceId) {
      return instanceId
    }
    const profile = await profileService.getActiveProfile()
    if (profile.instanceId) {
      return profile.instanceId
    }
    const fallback = await instanceService.getDefault()
    return fallback?.id
  }
}

function linesEqual(a: string[], b: string[]): boolean {
  if (a.length !== b.length) {
    return false
  }
  for (let i = 0; i < a.length; i++) {
    if (a[i] !== b[i]) {
      return false
    }
  }
  return true
}

export const performanceService = new PerformanceService()
