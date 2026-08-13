import {
  DEFAULT_MINECRAFT_TARGET,
  MINECRAFT_TARGETS,
  isSupportedPrimeVersion,
  resolveTarget
} from '../../shared/minecraft-targets'
import type { MinecraftVersionOptionDto } from '../../shared/ipc'

const UA = 'Prime-Launcher/2.5.1 (Version Catalog)'
const MOJANG_MANIFEST = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'
const FABRIC_GAME = 'https://meta.fabricmc.net/v2/versions/game'
const FABRIC_LOADER = (mc: string) => `https://meta.fabricmc.net/v2/versions/loader/${encodeURIComponent(mc)}`

const CACHE_TTL_MS = 30 * 60 * 1000

type MojangManifest = {
  latest: { release: string; snapshot: string }
  versions: Array<{ id: string; type: string }>
}

type FabricGameEntry = { version: string; stable: boolean }

type FabricLoaderEntry = {
  loader: { version: string; stable: boolean }
}

let cached: { at: number; versions: MinecraftVersionOptionDto[] } | null = null
let inflight: Promise<MinecraftVersionOptionDto[]> | null = null

function primeMeta(mcVersion: string): {
  primeAvailable: boolean
  recommended: boolean
  javaMajor?: number
  fabricLoader?: string
  fabricApi?: string
} {
  const known = MINECRAFT_TARGETS.find((t) => t.mcVersion === mcVersion || t.id === mcVersion)
  if (!known) {
    return { primeAvailable: false, recommended: false }
  }
  return {
    primeAvailable: true,
    recommended: Boolean(known.recommended),
    javaMajor: known.javaMajor,
    fabricLoader: known.fabricLoader,
    fabricApi: known.fabricApi
  }
}

function fallbackVersions(): MinecraftVersionOptionDto[] {
  const fromTargets = MINECRAFT_TARGETS.map((t) => ({
    id: t.mcVersion,
    type: 'release' as const,
    fabricAvailable: true,
    primeAvailable: true,
    recommended: Boolean(t.recommended),
    javaMajor: t.javaMajor,
    fabricLoader: t.fabricLoader,
    fabricApi: t.fabricApi
  }))

  // Recent releases users expect even when offline / APIs fail.
  const extras = [
    '26.1.2',
    '26.1.1',
    '26.1',
    '1.21.10',
    '1.21.9',
    '1.21.8',
    '1.21.7',
    '1.21.6',
    '1.21.5',
    '1.21.4',
    '1.21.3',
    '1.21.2',
    '1.21.1',
    '1.21',
    '1.20.6',
    '1.20.4',
    '1.20.1',
    '1.19.4',
    '1.18.2',
    '1.16.5'
  ]

  const seen = new Set(fromTargets.map((v) => v.id))
  const extraRows: MinecraftVersionOptionDto[] = extras
    .filter((id) => !seen.has(id))
    .map((id) => ({
      id,
      type: 'release' as const,
      fabricAvailable: true,
      primeAvailable: false,
      recommended: false,
      fabricLoader: 'latest'
    }))

  return [...fromTargets, ...extraRows]
}

async function fetchJson<T>(url: string): Promise<T> {
  const response = await fetch(url, {
    headers: { Accept: 'application/json', 'User-Agent': UA }
  })
  if (!response.ok) {
    throw new Error(`HTTP ${response.status} for ${url}`)
  }
  return (await response.json()) as T
}

async function resolveLatestFabricLoader(mcVersion: string): Promise<string | undefined> {
  try {
    const entries = await fetchJson<FabricLoaderEntry[]>(FABRIC_LOADER(mcVersion))
    const stable = entries.find((e) => e.loader?.stable)
    return stable?.loader.version ?? entries[0]?.loader.version
  } catch {
    return undefined
  }
}

async function buildCatalog(): Promise<MinecraftVersionOptionDto[]> {
  const [manifest, fabricGames] = await Promise.all([
    fetchJson<MojangManifest>(MOJANG_MANIFEST),
    fetchJson<FabricGameEntry[]>(FABRIC_GAME)
  ])

  const fabricStable = new Set(
    fabricGames.filter((g) => g.stable).map((g) => g.version)
  )
  // Also treat unstable fabric game entries as available for matching Mojang releases
  // when Fabric already published a loader (covers brand-new releases briefly).
  const fabricAny = new Set(fabricGames.map((g) => g.version))

  const releaseIds = manifest.versions
    .filter((v) => v.type === 'release')
    .map((v) => v.id)

  // Ensure known Prime targets appear even if Mojang naming drifts temporarily.
  for (const t of MINECRAFT_TARGETS) {
    if (!releaseIds.includes(t.mcVersion)) {
      releaseIds.unshift(t.mcVersion)
    }
  }

  const versions: MinecraftVersionOptionDto[] = releaseIds.map((id) => {
    const meta = primeMeta(id)
    const fabricAvailable = fabricStable.has(id) || fabricAny.has(id) || meta.primeAvailable
    return {
      id,
      type: 'release' as const,
      fabricAvailable,
      primeAvailable: meta.primeAvailable,
      recommended: meta.recommended || id === manifest.latest.release,
      javaMajor: meta.javaMajor,
      fabricLoader: meta.fabricLoader ?? (fabricAvailable ? 'latest' : undefined),
      fabricApi: meta.fabricApi
    }
  })

  // Prefer a single recommended flag: Prime recommended target wins over Mojang latest.
  const recommendedId =
    MINECRAFT_TARGETS.find((t) => t.recommended)?.mcVersion ??
    DEFAULT_MINECRAFT_TARGET.mcVersion
  for (const row of versions) {
    row.recommended = row.id === recommendedId
  }

  return versions
}

export class MinecraftVersionCatalog {
  async listVersions(): Promise<MinecraftVersionOptionDto[]> {
    if (cached && Date.now() - cached.at < CACHE_TTL_MS) {
      return cached.versions
    }
    if (inflight) {
      return inflight
    }

    inflight = (async () => {
      try {
        const versions = await buildCatalog()
        cached = { at: Date.now(), versions }
        return versions
      } catch {
        const fallback = fallbackVersions()
        cached = { at: Date.now(), versions: fallback }
        return fallback
      } finally {
        inflight = null
      }
    })()

    return inflight
  }

  /**
   * Resolve Fabric loader / API pins for instance create.
   * Prime targets keep pinned metadata; other Fabric versions use live loader or "latest".
   */
  async resolveFabricMeta(
    minecraftVersion: string,
    includePrimeMod: boolean
  ): Promise<{ fabricLoader: string; fabricApi?: string }> {
    const target = resolveTarget(minecraftVersion)
    if (isSupportedPrimeVersion(minecraftVersion)) {
      return {
        fabricLoader: target.fabricLoader,
        fabricApi: includePrimeMod ? target.fabricApi : undefined
      }
    }

    const live = await resolveLatestFabricLoader(minecraftVersion)
    return {
      fabricLoader: live ?? 'latest',
      fabricApi: undefined
    }
  }
}

export const minecraftVersionCatalog = new MinecraftVersionCatalog()
