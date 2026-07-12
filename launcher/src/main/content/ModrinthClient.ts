import { writeFile } from 'fs/promises'
import { downloadQueue } from '../utils/DownloadQueue'

const MODRINTH_API = 'https://api.modrinth.com/v2'

export interface ModrinthSearchHit {
  project_id: string
  slug: string
  title: string
  description: string
  downloads: number
  icon_url?: string
  project_type: string
}

export interface ModrinthSearchResult {
  hits: ModrinthSearchHit[]
  offset: number
  limit: number
  total_hits: number
}

export interface ModrinthVersionFile {
  url: string
  filename: string
  primary: boolean
}

export interface ModrinthVersion {
  id: string
  version_number: string
  game_versions: string[]
  loaders: string[]
  date_published: string
  files: ModrinthVersionFile[]
}

export async function searchModrinth(
  query: string,
  projectType: 'mod' | 'resourcepack' | 'shader',
  minecraftVersion: string,
  loader?: 'fabric' | 'forge' | 'quilt'
): Promise<ModrinthSearchHit[]> {
  const facets: string[][] = [[`project_type:${projectType}`], [`versions:${minecraftVersion}`]]
  if (loader && projectType === 'mod') {
    facets.push([`categories:${loader}`])
  }

  const params = new URLSearchParams({
    query,
    limit: '20',
    index: 'relevance',
    facets: JSON.stringify(facets)
  })

  const response = await fetch(`${MODRINTH_API}/search?${params.toString()}`)
  if (!response.ok) {
    throw new Error(`Modrinth search failed (${response.status}).`)
  }

  const data = (await response.json()) as ModrinthSearchResult
  return data.hits
}

export async function listModrinthVersions(
  projectId: string,
  minecraftVersion: string,
  loader?: 'fabric' | 'forge' | 'quilt'
): Promise<ModrinthVersion[]> {
  const params = new URLSearchParams({
    game_versions: JSON.stringify([minecraftVersion])
  })
  if (loader) {
    params.set('loaders', JSON.stringify([loader]))
  }

  const response = await fetch(`${MODRINTH_API}/project/${projectId}/version?${params.toString()}`)
  if (!response.ok) {
    throw new Error(`Modrinth version lookup failed (${response.status}).`)
  }

  return (await response.json()) as ModrinthVersion[]
}

export async function getModrinthVersionById(versionId: string): Promise<ModrinthVersion> {
  const response = await fetch(`${MODRINTH_API}/version/${versionId}`)
  if (!response.ok) {
    throw new Error(`Modrinth version lookup failed (${response.status}).`)
  }
  return (await response.json()) as ModrinthVersion
}

export async function getModrinthVersion(
  projectId: string,
  minecraftVersion: string,
  loader?: 'fabric' | 'forge' | 'quilt'
): Promise<ModrinthVersion> {
  const versions = await listModrinthVersions(projectId, minecraftVersion, loader)
  const version = versions[0]
  if (!version) {
    throw new Error(`No compatible version for Minecraft ${minecraftVersion}.`)
  }
  return version
}

export async function downloadModrinthFile(
  url: string,
  destPath: string,
  onProgress?: (percent: number, speed: string) => void
): Promise<void> {
  await downloadQueue.run(async () => {
    const response = await fetch(url)
    if (!response.ok || !response.body) {
      throw new Error(`Download failed (${response.status}).`)
    }

    const total = Number(response.headers.get('content-length') || 0)
    const reader = response.body.getReader()
    const chunks: Uint8Array[] = []
    let received = 0
    const start = Date.now()

    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        break
      }
      if (value) {
        chunks.push(value)
        received += value.length
        if (total > 0 && onProgress) {
          const elapsed = Math.max(1, Date.now() - start) / 1000
          const speed = `${(received / elapsed / 1024).toFixed(1)} KB/s`
          onProgress(Math.round((received / total) * 100), speed)
        }
      }
    }

    const buffer = Buffer.concat(chunks)
    const { writeFile } = await import('fs/promises')
    await writeFile(destPath, buffer)
    onProgress?.(100, 'Done')
  })
}
