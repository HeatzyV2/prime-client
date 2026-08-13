import type { HostSoftware, HostSoftwareBuildDto } from '../../../shared/host-types'

const UA = 'Prime-Launcher/2.5.0 (Local Server Host)'

/**
 * Catalog of Paper-family server JARs.
 *
 * API sources (documented for maintainers):
 * - Paper / Folia: PaperMC Fill API — https://fill.papermc.io/v3/projects/{paper|folia}
 * - Purpur: https://api.purpurmc.org/v2/purpur
 * - Leaf: Bibliothek-style API — https://api.leafmc.one/v2/projects/leaf
 *   (docs: https://api.leafmc.one/docs/swagger-ui/index.html)
 * - Canvas: official REST — https://canvasmc.io/api/v2/builds
 *   (docs: https://docs.canvasmc.io/canvas/developers/rest-api/)
 *   Fallback artifact pattern: Jenkins job Canvas on jenkins.canvasmc.io
 */

type FillProject = {
  versions: Record<string, string[]>
}

type FillBuild = {
  id: number
  time?: string
  channel?: string
  downloads?: Record<
    string,
    {
      name?: string
      url?: string
      checksums?: { sha256?: string }
    }
  >
}

type LeafBuildsResponse = {
  builds?: Array<{
    build: number
    time?: string
    channel?: string
    downloads?: {
      primary?: { name?: string; sha256?: string }
    }
  }>
}

type CanvasBuild = {
  result?: string
  buildNumber?: number
  downloadUrl?: string
  channelVersion?: string
  timestamp?: number
  isExperimental?: boolean
  channelName?: string
}

const cache = new Map<string, { at: number; data: unknown }>()
const CACHE_TTL_MS = 5 * 60 * 1000

async function fetchJson<T>(url: string, label: string): Promise<T> {
  const cached = cache.get(url)
  if (cached && Date.now() - cached.at < CACHE_TTL_MS) {
    return cached.data as T
  }

  let response: Response
  try {
    response = await fetch(url, {
      headers: { Accept: 'application/json', 'User-Agent': UA }
    })
  } catch (err) {
    const detail = err instanceof Error ? err.message : String(err)
    throw new Error(`${label} unreachable: ${detail}`)
  }

  if (!response.ok) {
    throw new Error(`${label} failed (HTTP ${response.status}).`)
  }

  const data = (await response.json()) as T
  cache.set(url, { at: Date.now(), data })
  return data
}

function flattenFillVersions(project: FillProject): string[] {
  const all: string[] = []
  for (const group of Object.values(project.versions ?? {})) {
    if (Array.isArray(group)) {
      all.push(...group)
    }
  }
  // Prefer newer first; keep release-looking versions ahead of pre/rc when equal length.
  return [...new Set(all)].sort((a, b) => b.localeCompare(a, undefined, { numeric: true }))
}

function fillDownload(build: FillBuild): HostSoftwareBuildDto | null {
  const downloads = build.downloads ?? {}
  const preferred =
    downloads['server:default'] ??
    downloads['application'] ??
    Object.values(downloads)[0]
  if (!preferred?.url || !preferred.name) {
    return null
  }
  return {
    id: String(build.id),
    channel: build.channel,
    time: build.time,
    downloadUrl: preferred.url,
    sha256: preferred.checksums?.sha256,
    fileName: preferred.name
  }
}

async function listFillVersions(project: 'paper' | 'folia'): Promise<string[]> {
  const data = await fetchJson<FillProject>(
    `https://fill.papermc.io/v3/projects/${project}`,
    `PaperMC Fill (${project})`
  )
  return flattenFillVersions(data)
}

async function listFillBuilds(
  project: 'paper' | 'folia',
  version: string
): Promise<HostSoftwareBuildDto[]> {
  const builds = await fetchJson<FillBuild[]>(
    `https://fill.papermc.io/v3/projects/${project}/versions/${encodeURIComponent(version)}/builds`,
    `PaperMC Fill builds (${project} ${version})`
  )
  if (!Array.isArray(builds)) {
    throw new Error(`Unexpected Fill builds payload for ${project} ${version}.`)
  }
  return builds
    .map(fillDownload)
    .filter((b): b is HostSoftwareBuildDto => b !== null)
    .sort((a, b) => Number(b.id) - Number(a.id))
}

async function listPurpurVersions(): Promise<string[]> {
  const data = await fetchJson<{ versions?: string[] }>(
    'https://api.purpurmc.org/v2/purpur',
    'Purpur API'
  )
  const versions = Array.isArray(data.versions) ? data.versions : []
  return [...versions].reverse()
}

async function listPurpurBuilds(version: string): Promise<HostSoftwareBuildDto[]> {
  const data = await fetchJson<{
    builds?: { latest?: string; all?: string[] }
  }>(`https://api.purpurmc.org/v2/purpur/${encodeURIComponent(version)}`, `Purpur builds (${version})`)
  const all = data.builds?.all ?? []
  return [...all]
    .reverse()
    .map((id) => ({
      id: String(id),
      downloadUrl: `https://api.purpurmc.org/v2/purpur/${encodeURIComponent(version)}/${encodeURIComponent(id)}/download`,
      fileName: `purpur-${version}-${id}.jar`
    }))
}

async function listLeafVersions(): Promise<string[]> {
  const data = await fetchJson<{ versions?: string[] }>(
    'https://api.leafmc.one/v2/projects/leaf',
    'Leaf API'
  )
  const versions = Array.isArray(data.versions) ? data.versions : []
  return [...versions].reverse()
}

async function listLeafBuilds(version: string): Promise<HostSoftwareBuildDto[]> {
  const data = await fetchJson<LeafBuildsResponse>(
    `https://api.leafmc.one/v2/projects/leaf/versions/${encodeURIComponent(version)}/builds`,
    `Leaf builds (${version})`
  )
  const builds = data.builds ?? []
  return [...builds]
    .reverse()
    .map((b) => {
      const name = b.downloads?.primary?.name ?? `leaf-${version}-${b.build}.jar`
      return {
        id: String(b.build),
        channel: b.channel,
        time: b.time,
        sha256: b.downloads?.primary?.sha256,
        fileName: name,
        downloadUrl: `https://api.leafmc.one/v2/projects/leaf/versions/${encodeURIComponent(version)}/builds/${b.build}/downloads/${encodeURIComponent(name)}`
      } satisfies HostSoftwareBuildDto
    })
}

async function listCanvasVersions(): Promise<string[]> {
  // Canvas channels are Minecraft versions; pull recent builds and unique channelVersion.
  const builds = await fetchJson<CanvasBuild[]>(
    'https://canvasmc.io/api/v2/builds/all?project=canvas&experimental=false',
    'Canvas API'
  )
  if (!Array.isArray(builds)) {
    throw new Error('Unexpected Canvas builds payload.')
  }
  const versions: string[] = []
  const seen = new Set<string>()
  for (const b of builds) {
    const v = b.channelVersion?.trim()
    if (v && !seen.has(v)) {
      seen.add(v)
      versions.push(v)
    }
  }
  return versions
}

async function listCanvasBuilds(version: string): Promise<HostSoftwareBuildDto[]> {
  const builds = await fetchJson<CanvasBuild[]>(
    `https://canvasmc.io/api/v2/builds/all?project=canvas&channel=${encodeURIComponent(version)}&experimental=true`,
    `Canvas builds (${version})`
  )
  if (!Array.isArray(builds)) {
    throw new Error(`Unexpected Canvas builds payload for ${version}.`)
  }
  return builds
    .filter((b) => b.downloadUrl && b.buildNumber != null)
    .map((b) => ({
      id: String(b.buildNumber),
      channel: b.channelName,
      time: b.timestamp ? new Date(b.timestamp).toISOString() : undefined,
      downloadUrl: b.downloadUrl!,
      fileName: `canvas-build.${b.buildNumber}.jar`
    }))
    .sort((a, b) => Number(b.id) - Number(a.id))
}

export class ServerSoftwareCatalog {
  async listVersions(software: HostSoftware): Promise<string[]> {
    switch (software) {
      case 'paper':
        return listFillVersions('paper')
      case 'folia':
        return listFillVersions('folia')
      case 'purpur':
        return listPurpurVersions()
      case 'leaf':
        return listLeafVersions()
      case 'canvas':
        return listCanvasVersions()
      default:
        throw new Error(`Unsupported software: ${software}`)
    }
  }

  async listBuilds(software: HostSoftware, version: string): Promise<HostSoftwareBuildDto[]> {
    switch (software) {
      case 'paper':
        return listFillBuilds('paper', version)
      case 'folia':
        return listFillBuilds('folia', version)
      case 'purpur':
        return listPurpurBuilds(version)
      case 'leaf':
        return listLeafBuilds(version)
      case 'canvas':
        return listCanvasBuilds(version)
      default:
        throw new Error(`Unsupported software: ${software}`)
    }
  }

  async resolveBuild(
    software: HostSoftware,
    version: string,
    buildId?: string
  ): Promise<HostSoftwareBuildDto> {
    const builds = await this.listBuilds(software, version)
    if (builds.length === 0) {
      throw new Error(`No builds found for ${software} ${version}.`)
    }
    if (!buildId || buildId === 'latest') {
      return builds[0]
    }
    const match = builds.find((b) => b.id === String(buildId))
    if (!match) {
      throw new Error(`Build ${buildId} not found for ${software} ${version}.`)
    }
    return match
  }
}

export const serverSoftwareCatalog = new ServerSoftwareCatalog()
