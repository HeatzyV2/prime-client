import { mkdir, readFile, writeFile } from 'fs/promises'
import { join, dirname } from 'path'
import { getInstanceGameDir } from '../minecraft/paths'
import { ecosystemStore } from '../storage/EcosystemStore'
import { settingsStore } from '../storage/SettingsStore'

/** Maps launcher cosmetic IDs to Prime Client mod slot + catalog ID. */
const MOD_COSMETIC_MAP: Record<string, { slot: string; modId: string }> = {
  'cape-prime': { slot: 'CAPE', modId: 'cape-prime' },
  'wings-ember': { slot: 'WINGS', modId: 'wings-light' },
  'badge-founder': { slot: 'BADGE', modId: 'badge-founder' },
  'badge-veteran': { slot: 'BADGE', modId: 'badge-founder' }
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

      const modules = (root.modules as Record<string, Record<string, unknown>> | undefined) ?? {}
      modules['discord-rpc'] = { ...(modules['discord-rpc'] ?? {}), enabled: settings.discordRpc }
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
