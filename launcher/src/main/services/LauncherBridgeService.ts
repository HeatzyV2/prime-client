import { mkdir, readFile, writeFile } from 'fs/promises'
import { join, dirname } from 'path'
import { getInstanceGameDir } from '../minecraft/paths'
import { ecosystemStore } from '../storage/EcosystemStore'
import { settingsStore } from '../storage/SettingsStore'
import { normalizePrimeTheme } from '../../shared/theme'
import { applyPerfPresetToModules } from '../../shared/perf-preset-modules'
import type { PerformancePreset } from '../../shared/content-types'

/** Maps launcher cosmetic IDs to Prime Client mod slot + catalog ID (1:1). */
const MOD_COSMETIC_MAP: Record<string, { slot: string; modId: string }> = {
  'cape-prime': { slot: 'CAPE', modId: 'cape-prime' },
  'cape-star': { slot: 'CAPE', modId: 'cape-star' },
  'cape-crimson': { slot: 'CAPE', modId: 'cape-crimson' },
  'cape-midnight': { slot: 'CAPE', modId: 'cape-midnight' },
  'wings-ember': { slot: 'WINGS', modId: 'wings-ember' },
  'wings-aurora': { slot: 'WINGS', modId: 'wings-aurora' }
}

/**
 * Writes launcher state into the instance game dir so Prime Client mod picks it up
 * on next launch (`config/primeclient/profiles/default.json`).
 */
export class LauncherBridgeService {
  async syncToInstance(instanceId: string): Promise<{ ok: boolean; error?: string }> {
    try {
      const [db, settings] = await Promise.all([ecosystemStore.load(), settingsStore.load()])
      const profilePath = join(getInstanceGameDir(instanceId), 'config', 'primeclient', 'profiles', 'default.json')

      let root: Record<string, unknown> = {}
      try {
        root = JSON.parse(await readFile(profilePath, 'utf8')) as Record<string, unknown>
      } catch {
        // Mod will create full profile on first run — seed cosmetics section now.
      }

      const cosmetics: Record<string, string> = {}
      for (const equippedId of db.equippedCosmetics) {
        const mapping = MOD_COSMETIC_MAP[equippedId]
        if (mapping) {
          cosmetics[mapping.slot] = mapping.modId
        }
      }

      root.cosmetics = cosmetics
      // Always write launcher theme — in-game poll reloads this section without wiping the rest.
      root.theme = { active: normalizePrimeTheme(settings.theme) }

      let modules = (root.modules as Record<string, Record<string, unknown>> | undefined) ?? {}
      modules = { ...modules }
      modules['discord-rpc'] = { ...(modules['discord-rpc'] ?? {}), enabled: settings.discordRpc }
      modules = applyPerfPresetToModules(modules, settings.performancePreset as PerformancePreset)
      root.modules = modules

      await mkdir(dirname(profilePath), { recursive: true })
      await writeFile(profilePath, JSON.stringify(root, null, 2), 'utf8')

      return { ok: true }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Bridge sync failed.'
      return { ok: false, error: message }
    }
  }
}

export const launcherBridgeService = new LauncherBridgeService()
