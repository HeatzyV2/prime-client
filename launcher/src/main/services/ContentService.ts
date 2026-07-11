import { dialog } from 'electron'
import { profileService } from './ProfileService'
import { instanceService } from './InstanceService'
import type { ModEntry, ResourcePackEntry, ShaderEntry } from '../../shared/content-types'
import type { ModrinthSearchHit } from '../content/ModrinthClient'
import { searchModrinth } from '../content/ModrinthClient'
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

  async installModFromModrinth(projectId: string, title: string, instanceId?: string) {
    const id = await resolveInstanceId(instanceId)
    const stored = await instanceService.getStoredById(id)
    if (!stored) {
      return { ok: false, error: 'Instance not found.' }
    }
    return ModManager.installModFromModrinth(
      id,
      projectId,
      title,
      stored.minecraftVersion,
      await loaderForInstance(id)
    )
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

  async installResourcePackFromModrinth(projectId: string, title: string, instanceId?: string) {
    const id = await resolveInstanceId(instanceId)
    const stored = await instanceService.getStoredById(id)
    if (!stored) {
      return { ok: false, error: 'Instance not found.' }
    }
    return ResourcePackManager.installResourcePackFromModrinth(
      id,
      projectId,
      title,
      stored.minecraftVersion
    )
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

  async installShaderFromModrinth(projectId: string, title: string, instanceId?: string) {
    const id = await resolveInstanceId(instanceId)
    const stored = await instanceService.getStoredById(id)
    if (!stored) {
      return { ok: false, error: 'Instance not found.' }
    }
    return ShaderManager.installShaderFromModrinth(id, projectId, title, stored.minecraftVersion)
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
}

export const contentService = new ContentService()
