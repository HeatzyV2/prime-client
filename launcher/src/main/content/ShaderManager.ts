import { mkdir, readdir, copyFile, rm } from 'fs/promises'
import { join, basename } from 'path'
import type { ShaderEntry } from '../../shared/content-types'
import { getShaderPacksDir } from './paths'
import { getActiveShaderPack, setActiveShaderPack } from './options'
import { patchContentMeta, readContentMeta } from './contentMeta'
import { downloadModrinthFile, getModrinthVersion } from './ModrinthClient'
import { downloadService } from '../services/DownloadService'

function shaderId(fileName: string): string {
  return encodeURIComponent(fileName)
}

function displayName(fileName: string, title?: string): string {
  return title ?? fileName.replace(/\.zip$/i, '').replace(/[-_+]/g, ' ').trim()
}

export async function listShaders(instanceId: string): Promise<ShaderEntry[]> {
  const dir = getShaderPacksDir(instanceId)
  await mkdir(dir, { recursive: true })

  let files: string[] = []
  try {
    files = await readdir(dir)
  } catch {
    return []
  }

  const meta = await readContentMeta(instanceId)
  const activeFile = await getActiveShaderPack(instanceId)
  const packs = files.filter((f) => f.endsWith('.zip'))

  return packs.map((fileName) => {
    const tracked = meta.shaders[fileName]
    return {
      id: shaderId(fileName),
      fileName,
      name: displayName(fileName, tracked?.title),
      description: tracked ? 'Installed from Modrinth' : 'Local shader pack',
      backend: 'iris',
      active: activeFile === fileName
    }
  })
}

export async function setShaderActive(
  instanceId: string,
  fileName: string | null
): Promise<{ ok: boolean; error?: string }> {
  try {
    await setActiveShaderPack(instanceId, fileName)
    return { ok: true }
  } catch (err) {
    return { ok: false, error: err instanceof Error ? err.message : 'Could not update options.txt' }
  }
}

export async function importShaderPack(
  instanceId: string,
  sourcePath: string
): Promise<{ ok: boolean; error?: string }> {
  const name = basename(sourcePath)
  if (!name.endsWith('.zip')) {
    return { ok: false, error: 'Shader packs must be .zip files.' }
  }

  const dir = getShaderPacksDir(instanceId)
  await mkdir(dir, { recursive: true })
  await copyFile(sourcePath, join(dir, name))
  return { ok: true }
}

export async function removeShaderPack(
  instanceId: string,
  fileName: string
): Promise<{ ok: boolean; error?: string }> {
  const active = await getActiveShaderPack(instanceId)
  if (active === fileName) {
    await setActiveShaderPack(instanceId, null)
  }

  await rm(join(getShaderPacksDir(instanceId), fileName), { force: true })
  await patchContentMeta(instanceId, (meta) => {
    delete meta.shaders[fileName]
  })
  return { ok: true }
}

export async function installShaderFromModrinth(
  instanceId: string,
  projectId: string,
  title: string,
  minecraftVersion: string
): Promise<{ ok: boolean; error?: string; fileName?: string }> {
  try {
    const version = await getModrinthVersion(projectId, minecraftVersion)
    const file = version.files.find((f) => f.primary) ?? version.files[0]
    if (!file) {
      return { ok: false, error: 'No downloadable file for this project.' }
    }

    const dir = getShaderPacksDir(instanceId)
    await mkdir(dir, { recursive: true })
    const dest = join(dir, file.filename)
    const taskId = await downloadService.beginDownload(`Shader: ${title}`)
    await downloadModrinthFile(file.url, dest, (percent, speed) => {
      void downloadService.updateDownload(taskId, percent, speed)
    })

    await patchContentMeta(instanceId, (meta) => {
      meta.shaders[file.filename] = {
        projectId,
        title,
        versionNumber: version.version_number,
        source: 'modrinth'
      }
    })

    return { ok: true, fileName: file.filename }
  } catch (err) {
    return { ok: false, error: err instanceof Error ? err.message : 'Install failed.' }
  }
}
