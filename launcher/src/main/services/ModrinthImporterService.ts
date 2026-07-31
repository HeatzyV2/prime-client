import { readdir, readFile, mkdir, copyFile, cp, writeFile } from 'fs/promises'
import { join } from 'path'
import { app } from 'electron'
import Zip from 'adm-zip'
import { instanceService } from './InstanceService'
import { getInstanceGameDir } from '../minecraft/paths'
import type { InstanceMutationResult } from '../storage/instance-types'
import { downloadService } from './DownloadService'
import { settingsStore } from '../storage/SettingsStore'

export interface ModrinthDetectedProfile {
  name: string
  path: string
  minecraftVersion: string
  loader: 'fabric' | 'vanilla'
  iconUrl?: string
  modCount: number
}

export class ModrinthImporterService {
  /**
   * Scans default Modrinth App profiles directory for installed instances.
   */
  async scanModrinthAppProfiles(): Promise<ModrinthDetectedProfile[]> {
    const profilesDir = this.getModrinthProfilesDir()
    const results: ModrinthDetectedProfile[] = []

    try {
      const entries = await readdir(profilesDir, { withFileTypes: true })
      for (const entry of entries) {
        if (!entry.isDirectory()) continue
        const profilePath = join(profilesDir, entry.name)
        try {
          const profileMetaPath = join(profilePath, 'profile.json')
          const metaRaw = await readFile(profileMetaPath, 'utf-8')
          const meta = JSON.parse(metaRaw)

          let modCount = 0
          try {
            const mods = await readdir(join(profilePath, 'mods'))
            modCount = mods.filter((m) => m.endsWith('.jar')).length
          } catch {
            // no mods folder
          }

          results.push({
            name: meta.name || entry.name,
            path: profilePath,
            minecraftVersion: meta.game_version || '1.21.11',
            loader: meta.loader === 'fabric' ? 'fabric' : 'vanilla',
            iconUrl: meta.icon,
            modCount
          })
        } catch {
          // Fallback: directory scan without profile.json
          let modCount = 0
          try {
            const mods = await readdir(join(profilePath, 'mods'))
            modCount = mods.filter((m) => m.endsWith('.jar')).length
          } catch {
            // ignore
          }
          results.push({
            name: entry.name,
            path: profilePath,
            minecraftVersion: '1.21.11',
            loader: 'fabric',
            modCount
          })
        }
      }
    } catch {
      // Modrinth App not installed or path not found
    }

    return results
  }

  /**
   * Imports a Modrinth App profile directory into a new Prime Launcher instance.
   */
  async importFromDirectory(
    sourcePath: string,
    instanceName: string,
    mcVersion = '1.21.11',
    loader: 'fabric' | 'vanilla' = 'fabric'
  ): Promise<InstanceMutationResult> {
    const settings = await settingsStore.load()
    const createResult = await instanceService.create({
      name: instanceName,
      minecraftVersion: mcVersion,
      loader,
      ramMb: settings.defaultRamMb,
      includePrimeMod: true
    })

    if (!createResult.ok || !createResult.instance) {
      return createResult
    }

    const instanceId = createResult.instance.id
    const targetGameDir = getInstanceGameDir(instanceId)

    // Copy configs, mods, resourcepacks, shaderpacks, options.txt
    const copyDirs = ['mods', 'config', 'resourcepacks', 'shaderpacks']
    for (const dirName of copyDirs) {
      const srcDir = join(sourcePath, dirName)
      const dstDir = join(targetGameDir, dirName)
      try {
        await cp(srcDir, dstDir, { recursive: true })
      } catch {
        // Folder might not exist in source
      }
    }

    const optionsSrc = join(sourcePath, 'options.txt')
    const optionsDst = join(targetGameDir, 'options.txt')
    try {
      await copyFile(optionsSrc, optionsDst)
    } catch {
      // options.txt optional
    }

    return createResult
  }

  /**
   * Imports a .mrpack archive into a new Prime Launcher instance.
   */
  async importMrpackFile(
    mrpackPath: string,
    instanceName: string
  ): Promise<InstanceMutationResult> {
    try {
      const zip = new Zip(mrpackPath)
      const indexEntry = zip.getEntry('modrinth.index.json')
      if (!indexEntry) {
        return { ok: false, error: 'Invalid .mrpack file: missing modrinth.index.json' }
      }

      const indexJson = JSON.parse(indexEntry.getData().toString('utf-8'))
      const mcVersion = indexJson.dependencies?.minecraft || '1.21.11'
      const deps = indexJson.dependencies || {}
      const isFabric = Boolean(deps['fabric-loader'])

      const settings = await settingsStore.load()
      const createResult = await instanceService.create({
        name: instanceName,
        minecraftVersion: mcVersion,
        loader: isFabric ? 'fabric' : 'vanilla',
        ramMb: settings.defaultRamMb,
        includePrimeMod: true
      })

      if (!createResult.ok || !createResult.instance) {
        return createResult
      }

      const instanceId = createResult.instance.id
      const targetGameDir = getInstanceGameDir(instanceId)

      // Download files listed in modrinth.index.json
      if (Array.isArray(indexJson.files)) {
        const modsDir = join(targetGameDir, 'mods')
        await mkdir(modsDir, { recursive: true })

        for (const file of indexJson.files) {
          if (file.downloads && file.downloads.length > 0) {
            const url = file.downloads[0]
            const fileName = file.path ? file.path.replace(/^mods\//, '') : 'mod.jar'
            const destPath = join(modsDir, fileName)
            try {
              await downloadService.downloadFile({ url, destPath })
            } catch {
              // skip failed mod download
            }
          }
        }
      }

      // Extract overrides (configs, options.txt)
      const entries = zip.getEntries()
      for (const entry of entries) {
        if (entry.isDirectory || !entry.entryName.startsWith('overrides/')) continue
        const relativePath = entry.entryName.replace(/^overrides\//, '')
        const destPath = join(targetGameDir, relativePath)
        const parentDir = join(destPath, '..')
        await mkdir(parentDir, { recursive: true })
        await writeFile(destPath, entry.getData())
      }

      return createResult
    } catch (err: any) {
      return { ok: false, error: err.message || 'Failed to import .mrpack file.' }
    }
  }

  private getModrinthProfilesDir(): string {
    const platform = process.platform
    if (platform === 'win32') {
      const appData = process.env.APPDATA || join(app.getPath('home'), 'AppData', 'Roaming')
      return join(appData, 'ModrinthApp', 'profiles')
    } else if (platform === 'darwin') {
      return join(app.getPath('home'), 'Library', 'Application Support', 'ModrinthApp', 'profiles')
    } else {
      return join(app.getPath('home'), '.local', 'share', 'modrinthapp', 'profiles')
    }
  }
}

export const modrinthImporterService = new ModrinthImporterService()
