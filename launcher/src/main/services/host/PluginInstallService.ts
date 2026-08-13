import { mkdir, readdir, rename, rm, stat } from 'fs/promises'
import { join } from 'path'
import { downloadService } from '../DownloadService'
import type { HostPluginHitDto, HostPluginSource, InstalledHostPluginDto } from '../../../shared/host-types'

const UA = 'Prime-Launcher/2.5.1 (Local Server Host)'
const MODRINTH_API = 'https://api.modrinth.com/v2'
const HANGAR_API = 'https://hangar.papermc.io/api/v1'
const SPIGET_API = 'https://api.spiget.org/v2'

async function fetchJson<T>(url: string, label: string): Promise<T> {
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
  return (await response.json()) as T
}

function pluginsDir(serverDir: string): string {
  return join(serverDir, 'plugins')
}

async function ensurePluginsDir(serverDir: string): Promise<string> {
  const dir = pluginsDir(serverDir)
  await mkdir(dir, { recursive: true })
  return dir
}

async function searchModrinth(query: string, mcVersion?: string): Promise<HostPluginHitDto[]> {
  const facets: string[][] = [['project_type:plugin']]
  if (mcVersion) {
    facets.push([`versions:${mcVersion}`])
  }
  // Prefer Paper-family loaders when present on Modrinth.
  facets.push(['categories:paper', 'categories:purpur', 'categories:folia', 'categories:bukkit', 'categories:spigot'])

  const params = new URLSearchParams({
    query,
    limit: '20',
    index: 'relevance',
    facets: JSON.stringify(facets)
  })

  const data = await fetchJson<{
    hits?: Array<{
      project_id: string
      slug?: string
      title?: string
      description?: string
      downloads?: number
      icon_url?: string
    }>
  }>(`${MODRINTH_API}/search?${params}`, 'Modrinth plugin search')

  return (data.hits ?? []).map((h) => ({
    id: h.project_id,
    slug: h.slug,
    name: h.title ?? h.slug ?? h.project_id,
    description: h.description ?? '',
    downloads: h.downloads ?? 0,
    iconUrl: h.icon_url,
    source: 'modrinth' as const
  }))
}

async function searchHangar(query: string): Promise<HostPluginHitDto[]> {
  const params = new URLSearchParams({
    q: query,
    limit: '20',
    sortBy: 'downloads'
  })
  const data = await fetchJson<{
    result?: Array<{
      name?: string
      namespace?: string
      description?: string
      stats?: { downloads?: number }
      avatarUrl?: string
    }>
  }>(`${HANGAR_API}/projects?${params}`, 'Hangar plugin search')

  return (data.result ?? []).map((p) => {
    const slug = p.namespace && p.name ? `${p.namespace}/${p.name}` : p.name ?? 'unknown'
    return {
      id: slug,
      slug,
      name: p.name ?? slug,
      description: p.description ?? '',
      downloads: p.stats?.downloads ?? 0,
      iconUrl: p.avatarUrl,
      source: 'hangar' as const
    }
  })
}

async function searchSpiget(query: string): Promise<HostPluginHitDto[]> {
  const encoded = encodeURIComponent(query.trim() || 'plugin')
  const data = await fetchJson<
    Array<{
      id?: number
      name?: string
      tag?: string
      downloads?: number
      icon?: { url?: string; data?: string }
    }>
  >(`${SPIGET_API}/search/resources/${encoded}?size=20&sort=-downloads`, 'Spiget (SpigotMC) search')

  if (!Array.isArray(data)) {
    throw new Error('Unexpected Spiget search payload.')
  }

  return data.map((r) => ({
    id: String(r.id ?? ''),
    name: r.name ?? `resource-${r.id}`,
    description: r.tag ?? '',
    downloads: r.downloads ?? 0,
    iconUrl: r.icon?.url
      ? r.icon.url.startsWith('http')
        ? r.icon.url
        : `https://www.spigotmc.org/${r.icon.url}`
      : undefined,
    source: 'spiget' as const
  })).filter((h) => h.id)
}

async function installFromModrinth(
  projectId: string,
  serverDir: string,
  mcVersion: string
): Promise<string> {
  const params = new URLSearchParams({
    loaders: JSON.stringify(['paper', 'purpur', 'folia', 'bukkit', 'spigot']),
    game_versions: JSON.stringify([mcVersion])
  })
  let versions = await fetchJson<
    Array<{
      id: string
      files?: Array<{
        url: string
        filename: string
        primary?: boolean
        hashes?: { sha256?: string; sha512?: string; sha1?: string }
      }>
    }>
  >(`${MODRINTH_API}/project/${encodeURIComponent(projectId)}/version?${params}`, 'Modrinth versions')

  if (!Array.isArray(versions) || versions.length === 0) {
    // Retry without game version filter — some plugins lag behind MC releases.
    versions = await fetchJson(
      `${MODRINTH_API}/project/${encodeURIComponent(projectId)}/version?loaders=${encodeURIComponent(JSON.stringify(['paper', 'purpur', 'folia', 'bukkit', 'spigot']))}`,
      'Modrinth versions (unfiltered)'
    )
  }

  const version = versions[0]
  const file = version?.files?.find((f) => f.primary) ?? version?.files?.[0]
  if (!file?.url || !file.filename) {
    throw new Error('No downloadable Modrinth file found for this plugin.')
  }

  const dest = join(await ensurePluginsDir(serverDir), file.filename)
  await downloadService.downloadFile({
    url: file.url,
    destPath: dest,
    integrity: file.hashes?.sha256
      ? { algorithm: 'sha256', hash: file.hashes.sha256 }
      : file.hashes?.sha512
        ? { algorithm: 'sha512', hash: file.hashes.sha512 }
        : file.hashes?.sha1
          ? { algorithm: 'sha1', hash: file.hashes.sha1 }
          : undefined,
    headers: { 'User-Agent': UA }
  })
  return file.filename
}

async function installFromHangar(projectId: string, serverDir: string, mcVersion: string): Promise<string> {
  // projectId is "owner/slug"
  const versions = await fetchJson<{
    result?: Array<{
      name?: string
      downloads?: Record<
        string,
        {
          fileInfo?: { name?: string; sha256Hash?: string }
          downloadUrl?: string | null
          externalUrl?: string | null
        }
      >
    }>
  }>(
    `${HANGAR_API}/projects/${encodeURIComponent(projectId)}/versions?limit=10&platform=PAPER`,
    'Hangar versions'
  )

  const versionList = versions.result ?? []
  let chosenFile: { url: string; name: string; sha256?: string } | null = null

  for (const v of versionList) {
    const platforms = v.downloads ?? {}
    const paper = platforms.PAPER ?? Object.values(platforms)[0]
    if (!paper) continue
    const url = paper.downloadUrl || paper.externalUrl
    const name = paper.fileInfo?.name
    if (url && name) {
      chosenFile = { url, name, sha256: paper.fileInfo?.sha256Hash }
      // Prefer versions mentioning the MC version in the version name when possible.
      if (v.name?.includes(mcVersion)) break
      if (!chosenFile) continue
      break
    }
  }

  if (!chosenFile) {
    throw new Error(
      `No Hangar PAPER download found for ${projectId}. The project may only offer external downloads.`
    )
  }

  const dest = join(await ensurePluginsDir(serverDir), chosenFile.name)
  await downloadService.downloadFile({
    url: chosenFile.url,
    destPath: dest,
    sha256: chosenFile.sha256,
    headers: { 'User-Agent': UA }
  })
  return chosenFile.name
}

async function installFromSpiget(resourceId: string, serverDir: string): Promise<string> {
  const meta = await fetchJson<{
    id?: number
    name?: string
    file?: { type?: string; size?: number; sizeUnit?: string; url?: string }
  }>(`${SPIGET_API}/resources/${encodeURIComponent(resourceId)}`, 'Spiget resource')

  const safeName = (meta.name ?? `spiget-${resourceId}`)
    .replace(/[^\w.\-]+/g, '_')
    .replace(/_+/g, '_')
  const fileName = `${safeName}-${resourceId}.jar`
  const dest = join(await ensurePluginsDir(serverDir), fileName)

  // Spiget download endpoint — may fail for premium / external resources.
  try {
    await downloadService.downloadFile({
      url: `${SPIGET_API}/resources/${encodeURIComponent(resourceId)}/download`,
      destPath: dest,
      headers: { 'User-Agent': UA },
      bypassCache: true
    })
  } catch (err) {
    const detail = err instanceof Error ? err.message : String(err)
    throw new Error(
      `Spiget download failed for resource ${resourceId}. Premium or external-only resources cannot be installed automatically. (${detail})`
    )
  }
  return fileName
}

export class PluginInstallService {
  async search(
    query: string,
    sources: HostPluginSource[] = ['modrinth', 'hangar', 'spiget'],
    mcVersion?: string
  ): Promise<HostPluginHitDto[]> {
    const q = query.trim()
    if (!q) return []

    const tasks: Promise<HostPluginHitDto[]>[] = []
    if (sources.includes('modrinth')) {
      tasks.push(
        searchModrinth(q, mcVersion).catch((err) => {
          console.warn('[host-plugins] Modrinth search failed:', err)
          return [] as HostPluginHitDto[]
        })
      )
    }
    if (sources.includes('hangar')) {
      tasks.push(
        searchHangar(q).catch((err) => {
          console.warn('[host-plugins] Hangar search failed:', err)
          return [] as HostPluginHitDto[]
        })
      )
    }
    if (sources.includes('spiget')) {
      tasks.push(
        searchSpiget(q).catch((err) => {
          console.warn('[host-plugins] Spiget search failed:', err)
          return [] as HostPluginHitDto[]
        })
      )
    }

    const batches = await Promise.all(tasks)
    const merged = batches.flat()
    if (merged.length === 0 && sources.length > 0) {
      throw new Error('Plugin search failed on all sources. Check your network and try again.')
    }
    return merged.sort((a, b) => b.downloads - a.downloads)
  }

  async install(
    serverDir: string,
    source: HostPluginSource,
    projectId: string,
    mcVersion: string
  ): Promise<{ ok: boolean; fileName?: string; error?: string }> {
    try {
      let fileName: string
      if (source === 'modrinth') {
        fileName = await installFromModrinth(projectId, serverDir, mcVersion)
      } else if (source === 'hangar') {
        fileName = await installFromHangar(projectId, serverDir, mcVersion)
      } else {
        fileName = await installFromSpiget(projectId, serverDir)
      }
      return { ok: true, fileName }
    } catch (err) {
      return { ok: false, error: err instanceof Error ? err.message : String(err) }
    }
  }

  async listInstalled(serverDir: string): Promise<InstalledHostPluginDto[]> {
    const dir = await ensurePluginsDir(serverDir)
    const entries = await readdir(dir)
    const plugins: InstalledHostPluginDto[] = []
    for (const name of entries) {
      const lower = name.toLowerCase()
      if (!lower.endsWith('.jar') && !lower.endsWith('.jar.disabled')) continue
      const full = join(dir, name)
      const info = await stat(full)
      if (!info.isFile()) continue
      const enabled = lower.endsWith('.jar') && !lower.endsWith('.jar.disabled')
      plugins.push({
        fileName: name,
        enabled,
        size: info.size
      })
    }
    return plugins.sort((a, b) => a.fileName.localeCompare(b.fileName))
  }

  async setEnabled(
    serverDir: string,
    fileName: string,
    enabled: boolean
  ): Promise<{ ok: boolean; error?: string; fileName?: string }> {
    const dir = pluginsDir(serverDir)
    const current = join(dir, fileName)
    try {
      await stat(current)
    } catch {
      return { ok: false, error: 'Plugin file not found.' }
    }

    const isDisabled = fileName.toLowerCase().endsWith('.jar.disabled')
    const isJar = fileName.toLowerCase().endsWith('.jar') && !isDisabled

    let nextName = fileName
    if (enabled && isDisabled) {
      nextName = fileName.slice(0, -'.disabled'.length)
    } else if (!enabled && isJar) {
      nextName = `${fileName}.disabled`
    } else {
      return { ok: true, fileName }
    }

    await rename(current, join(dir, nextName))
    return { ok: true, fileName: nextName }
  }

  async remove(
    serverDir: string,
    fileName: string
  ): Promise<{ ok: boolean; error?: string }> {
    const full = join(pluginsDir(serverDir), fileName)
    try {
      await rm(full, { force: true })
      return { ok: true }
    } catch (err) {
      return { ok: false, error: err instanceof Error ? err.message : String(err) }
    }
  }
}

export const pluginInstallService = new PluginInstallService()
