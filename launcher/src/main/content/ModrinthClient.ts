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

export async function getModrinthVersion(
  projectId: string,
  minecraftVersion: string,
  loader?: 'fabric' | 'forge' | 'quilt'
): Promise<ModrinthVersion> {
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

  const versions = (await response.json()) as ModrinthVersion[]
  const version = versions[0]
  if (!version) {
    throw new Error(`No compatible version for Minecraft ${minecraftVersion}.`)
  }
  return version
}

export async function downloadModrinthFile(url: string, destPath: string): Promise<void> {
  const { createWriteStream } = await import('fs')
  const { pipeline } = await import('stream/promises')
  const response = await fetch(url)
  if (!response.ok || !response.body) {
    throw new Error(`Download failed (${response.status}).`)
  }
  await pipeline(response.body as unknown as NodeJS.ReadableStream, createWriteStream(destPath))
}
