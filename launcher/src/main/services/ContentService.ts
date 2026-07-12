import { dialog } from 'electron'
import { profileService } from './ProfileService'
import { instanceService } from './InstanceService'
import { downloadService } from './DownloadService'
import type { ModEntry, ResourcePackEntry, ShaderEntry } from '../../shared/content-types'
import type { ContentVersionDto } from '../../shared/ipc'
import type { ModrinthSearchHit } from '../content/ModrinthClient'
import { searchModrinth, listModrinthVersions } from '../content/ModrinthClient'
import type { CurseForgeSearchHit } from '../content/CurseForgeClient'
import { searchCurseForge, listCurseForgeFiles } from '../content/CurseForgeClient'
import * as ModManager from '../content/ModManager'
import * as ResourcePackManager from '../content/ResourcePackManager'
import * as ShaderManager from '../content/ShaderManager'

async function resolveInstanceId(instanceId?: string): Promise<string> {
  if (instanceId) {
    return instanceId
  }
  const profile = await profileService.getActiveProfile()
  if (profile.instanceId) {
    const inst = await instanceService.getStoredById(profile.instanceId)
    if (inst) {
      return profile.instanceId
    }
  }
  const fallback = await instanceService.getDefault()
  if (!fallback) {
    throw new Error('No instance available.')
  }
  return fallback.id
}

async function loaderForInstance(instanceId: string): Promise<'fabric' | 'forge' | 'quilt'> {
  const stored = await instanceService.getStoredById(instanceId)
  if (stored?.loader === 'fabric') {
    return 'fabric'
  }
  return 'fabric'
}

/** Mods, resource packs, shaders — local files + Modrinth public API. */
export class ContentService {
  async listMods(instanceId?: string): Promise<ModEntry[]> {
    return ModManager.listMods(await resolveInstanceId(instanceId))
  }

  async setModEnabled(fileName: string, enabled: boolean, instanceId?: string) {
    return ModManager.setModEnabled(await resolveInstanceId(instanceId), fileName, enabled)
  }

  async removeMod(fileName: string, instanceId?: string) {
    return ModManager.removeMod(await resolveInstanceId(instanceId), fileName)
  }

  async importMod(instanceId?: string) {
    const id = await resolveInstanceId(instanceId)
    const { canceled, filePaths } = await dialog.showOpenDialog({
      title: 'Import mod (.jar)',
      properties: ['openFile'],
      filters: [{ name: 'Fabric / Forge mods', extensions: ['jar'] }]
    })
    if (canceled || !filePaths[0]) {
      return { ok: false, error: 'Cancelled.' }
    }
    return ModManager.importModFile(id, filePaths[0])
  }

  async installModFromModrinth(projectId: string, title: string, instanceId?: string, versionId?: string) {
    const id = await resolveInstanceId(instanceId)
    const stored = await instanceService.getStoredById(id)
    if (!stored) {
      return { ok: false, error: 'Instance not found.' }
    }
    const result = await ModManager.installModFromModrinth(
      id,
      projectId,
      title,
      stored.minecraftVersion,
      await loaderForInstance(id),
      versionId
    )
    if (result.ok) {
      await downloadService.trackContentInstall(`Mod: ${title}`)
    }
    return result
  }

  async installModFromCurseForge(projectId: string, title: string, instanceId?: string, fileId?: string) {
    const id = await resolveInstanceId(instanceId)
    const stored = await instanceService.getStoredById(id)
    if (!stored) {
      return { ok: false, error: 'Instance not found.' }
    }
    const result = await ModManager.installModFromCurseForge(
      id,
      projectId,
      title,
      stored.minecraftVersion,
      await loaderForInstance(id),
      fileId
    )
    if (result.ok) {
      await downloadService.trackContentInstall(`Mod: ${title}`)
    }
    return result
  }

  async listResourcePacks(instanceId?: string): Promise<ResourcePackEntry[]> {
    return ResourcePackManager.listResourcePacks(await resolveInstanceId(instanceId))
  }

  async setResourcePackActive(fileName: string | null, instanceId?: string) {
    return ResourcePackManager.setResourcePackActive(await resolveInstanceId(instanceId), fileName)
  }

  async importResourcePack(instanceId?: string) {
    const id = await resolveInstanceId(instanceId)
    const { canceled, filePaths } = await dialog.showOpenDialog({
      title: 'Import resource pack (.zip)',
      properties: ['openFile'],
      filters: [{ name: 'Resource packs', extensions: ['zip'] }]
    })
    if (canceled || !filePaths[0]) {
      return { ok: false, error: 'Cancelled.' }
    }
    return ResourcePackManager.importResourcePack(id, filePaths[0])
  }

  async removeResourcePack(fileName: string, instanceId?: string) {
    return ResourcePackManager.removeResourcePack(await resolveInstanceId(instanceId), fileName)
  }

  async installResourcePackFromModrinth(projectId: string, title: string, instanceId?: string, versionId?: string) {
    const id = await resolveInstanceId(instanceId)
    const stored = await instanceService.getStoredById(id)
    if (!stored) {
      return { ok: false, error: 'Instance not found.' }
    }
    const result = await ResourcePackManager.installResourcePackFromModrinth(
      id,
      projectId,
      title,
      stored.minecraftVersion,
      versionId
    )
    if (result.ok) {
      await downloadService.trackContentInstall(`Resource pack: ${title}`)
    }
    return result
  }

  async installResourcePackFromCurseForge(projectId: string, title: string, instanceId?: string, fileId?: string) {
    const id = await resolveInstanceId(instanceId)
    const stored = await instanceService.getStoredById(id)
    if (!stored) {
      return { ok: false, error: 'Instance not found.' }
    }
    const result = await ResourcePackManager.installResourcePackFromCurseForge(
      id,
      projectId,
      title,
      stored.minecraftVersion,
      fileId
    )
    if (result.ok) {
      await downloadService.trackContentInstall(`Resource pack: ${title}`)
    }
    return result
  }

  async listShaders(instanceId?: string): Promise<ShaderEntry[]> {
    return ShaderManager.listShaders(await resolveInstanceId(instanceId))
  }

  async setShaderActive(fileName: string | null, instanceId?: string) {
    return ShaderManager.setShaderActive(await resolveInstanceId(instanceId), fileName)
  }

  async importShader(instanceId?: string) {
    const id = await resolveInstanceId(instanceId)
    const { canceled, filePaths } = await dialog.showOpenDialog({
      title: 'Import shader pack (.zip)',
      properties: ['openFile'],
      filters: [{ name: 'Shader packs', extensions: ['zip'] }]
    })
    if (canceled || !filePaths[0]) {
      return { ok: false, error: 'Cancelled.' }
    }
    return ShaderManager.importShaderPack(id, filePaths[0])
  }

  async removeShader(fileName: string, instanceId?: string) {
    return ShaderManager.removeShaderPack(await resolveInstanceId(instanceId), fileName)
  }

  async installShaderFromModrinth(projectId: string, title: string, instanceId?: string, versionId?: string) {
    const id = await resolveInstanceId(instanceId)
    const stored = await instanceService.getStoredById(id)
    if (!stored) {
      return { ok: false, error: 'Instance not found.' }
    }
    const result = await ShaderManager.installShaderFromModrinth(id, projectId, title, stored.minecraftVersion, versionId)
    if (result.ok) {
      await downloadService.trackContentInstall(`Shader: ${title}`)
    }
    return result
  }

  async installShaderFromCurseForge(projectId: string, title: string, instanceId?: string, fileId?: string) {
    const id = await resolveInstanceId(instanceId)
    const stored = await instanceService.getStoredById(id)
    if (!stored) {
      return { ok: false, error: 'Instance not found.' }
    }
    const result = await ShaderManager.installShaderFromCurseForge(
      id,
      projectId,
      title,
      stored.minecraftVersion,
      fileId
    )
    if (result.ok) {
      await downloadService.trackContentInstall(`Shader: ${title}`)
    }
    return result
  }

  async searchModrinth(
    query: string,
    type: 'mod' | 'resourcepack' | 'shader',
    instanceId?: string
  ): Promise<ModrinthSearchHit[]> {
    const id = await resolveInstanceId(instanceId)
    const stored = await instanceService.getStoredById(id)
    if (!stored) {
      return []
    }
    const loader = type === 'mod' ? await loaderForInstance(id) : undefined
    return searchModrinth(query, type, stored.minecraftVersion, loader)
  }

  async searchCurseForge(
    query: string,
    type: 'mod' | 'resourcepack' | 'shader',
    instanceId?: string
  ): Promise<CurseForgeSearchHit[]> {
    const id = await resolveInstanceId(instanceId)
    const stored = await instanceService.getStoredById(id)
    if (!stored) {
      return []
    }
    const loader = type === 'mod' ? await loaderForInstance(id) : undefined
    return searchCurseForge(query, type, stored.minecraftVersion, loader)
  }

  async listContentVersions(
    projectId: string,
    type: 'mod' | 'resourcepack' | 'shader',
    source: 'modrinth' | 'curseforge',
    instanceId?: string
  ): Promise<ContentVersionDto[]> {
    const id = await resolveInstanceId(instanceId)
    const stored = await instanceService.getStoredById(id)
    if (!stored) {
      return []
    }

    const loader = type === 'mod' ? await loaderForInstance(id) : undefined

    if (source === 'modrinth') {
      const versions = await listModrinthVersions(projectId, stored.minecraftVersion, loader)
      return versions.map((version, index) => {
        const file = version.files.find((f) => f.primary) ?? version.files[0]
        return {
          id: version.id,
          versionNumber: version.version_number,
          gameVersions: version.game_versions,
          loaders: version.loaders,
          fileName: file?.filename,
          recommended: index === 0
        }
      })
    }

    const files = await listCurseForgeFiles(projectId, stored.minecraftVersion, loader)
    return files.map((file, index) => ({
      id: String(file.id),
      versionNumber: file.fileName.replace(/\.jar$|\.zip$/i, ''),
      gameVersions: file.gameVersions ?? [stored.minecraftVersion],
      loaders: file.modLoaders?.map((entry) => entry.name.toLowerCase()) ?? [],
      fileName: file.fileName,
      recommended: index === 0
    }))
  }
}

export const contentService = new ContentService()
