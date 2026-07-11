import { createWriteStream } from 'fs'
import { copyFile, mkdir, readdir, readFile, stat, unlink, writeFile } from 'fs/promises'
import { app } from 'electron'
import { basename, join } from 'path'
import { pipeline } from 'stream/promises'
import {
  fetchLatestGitHubRelease,
  isPrimeModJarAsset,
  pickPrimeModAsset,
  type GitHubRelease
} from '../../shared/githubRelease'
import { getInstanceModsDir, getRepoRoot } from './paths'
import { emitLaunchProgress } from './launchProgress'
import type { InstanceLaunchConfig } from './constants'

const FABRIC_API_PROJECT = 'P7dR8mSH'
const PRIME_JAR_PREFIX = 'prime-client-1.21.11'

interface ModCacheManifest {
  tag: string
  fileName: string
  downloadedAt: string
}

async function downloadFile(url: string, dest: string): Promise<void> {
  const response = await fetch(url)
  if (!response.ok || !response.body) {
    throw new Error(`Download failed (${response.status}): ${url}`)
  }
  await pipeline(response.body as unknown as NodeJS.ReadableStream, createWriteStream(dest))
}

function modCacheDir(): string {
  return join(app.getPath('userData'), 'cache', 'prime-mod')
}

function modCacheManifestPath(): string {
  return join(modCacheDir(), 'manifest.json')
}

async function readModCacheManifest(): Promise<ModCacheManifest | null> {
  try {
    const raw = await readFile(modCacheManifestPath(), 'utf8')
    return JSON.parse(raw) as ModCacheManifest
  } catch {
    return null
  }
}

async function writeModCacheManifest(manifest: ModCacheManifest): Promise<void> {
  await mkdir(modCacheDir(), { recursive: true })
  await writeFile(modCacheManifestPath(), JSON.stringify(manifest, null, 2), 'utf8')
}

async function findLocalBuildJar(): Promise<string | null> {
  const libsDir = join(getRepoRoot(), 'mc-1.21.11', 'build', 'libs')
  try {
    const files = await readdir(libsDir)
    const match = files
      .filter((f) => isPrimeModJarAsset(f, PRIME_JAR_PREFIX))
      .sort()
      .at(-1)
    if (match) {
      return join(libsDir, match)
    }
  } catch {
    // build output missing
  }
  return null
}

async function findEnvOverrideJar(): Promise<string | null> {
  if (!process.env.PRIME_CLIENT_JAR) {
    return null
  }
  try {
    const info = await stat(process.env.PRIME_CLIENT_JAR)
    if (info.isFile()) {
      return process.env.PRIME_CLIENT_JAR
    }
  } catch {
    // fall through
  }
  return null
}

async function resolveCachedGitHubJar(manifest: ModCacheManifest): Promise<string | null> {
  const cached = join(modCacheDir(), manifest.fileName)
  try {
    const info = await stat(cached)
    if (info.isFile() && info.size > 0) {
      return cached
    }
  } catch {
    // re-download below
  }
  return null
}

async function downloadPrimeModFromRelease(release: GitHubRelease): Promise<string | null> {
  const asset = pickPrimeModAsset(release, PRIME_JAR_PREFIX)
  if (!asset?.browser_download_url) {
    return null
  }

  await mkdir(modCacheDir(), { recursive: true })
  const dest = join(modCacheDir(), asset.name)

  emitLaunchProgress({
    phase: 'mods',
    detail: `Downloading Prime Client ${release.tag_name} from GitHub…`,
    percent: 38
  })

  await downloadFile(asset.browser_download_url, dest)
  await writeModCacheManifest({
    tag: release.tag_name,
    fileName: asset.name,
    downloadedAt: new Date().toISOString()
  })
  return dest
}

/** Resolves Prime Client jar: env override → local Gradle build → GitHub Releases cache/download. */
async function resolvePrimeClientSource(): Promise<string | null> {
  const envJar = await findEnvOverrideJar()
  if (envJar) {
    return envJar
  }

  const localJar = await findLocalBuildJar()
  if (localJar) {
    return localJar
  }

  const manifest = await readModCacheManifest()
  if (manifest) {
    const cached = await resolveCachedGitHubJar(manifest)
    if (cached) {
      return cached
    }
  }

  emitLaunchProgress({
    phase: 'mods',
    detail: 'Checking GitHub Releases for Prime Client mod…',
    percent: 36
  })

  const release = await fetchLatestGitHubRelease()
  if (!release) {
    return null
  }

  if (manifest?.tag === release.tag_name) {
    const cached = await resolveCachedGitHubJar(manifest)
    if (cached) {
      return cached
    }
  }

  return downloadPrimeModFromRelease(release)
}

async function removeStalePrimeJars(modsDir: string, keepFileName: string): Promise<void> {
  let files: string[]
  try {
    files = await readdir(modsDir)
  } catch {
    return
  }

  await Promise.all(
    files
      .filter((f) => isPrimeModJarAsset(f, PRIME_JAR_PREFIX) && f !== keepFileName)
      .map((f) => unlink(join(modsDir, f)).catch(() => undefined))
  )
}

async function ensureFabricApi(config: InstanceLaunchConfig, modsDir: string): Promise<string> {
  const destName = `fabric-api-${config.fabricApiModrinthVersion}.jar`
  const dest = join(modsDir, destName)
  try {
    const info = await stat(dest)
    if (info.isFile() && info.size > 0) {
      return dest
    }
  } catch {
    // download below
  }

  emitLaunchProgress({
    phase: 'mods',
    detail: 'Downloading Fabric API from Modrinth…',
    percent: 35
  })

  const query = new URLSearchParams({
    game_versions: JSON.stringify([config.minecraftVersion]),
    loaders: JSON.stringify(['fabric'])
  })
  const response = await fetch(
    `https://api.modrinth.com/v2/project/${FABRIC_API_PROJECT}/version?${query.toString()}`
  )
  if (!response.ok) {
    throw new Error(`Modrinth API error (${response.status}) while fetching Fabric API.`)
  }

  const versions = (await response.json()) as Array<{
    version_number: string
    files: Array<{ url: string; primary: boolean; filename: string }>
  }>

  const exact = versions.find((v) => v.version_number === config.fabricApiModrinthVersion)
  const chosen = exact ?? versions[0]
  if (!chosen) {
    throw new Error(`No Fabric API build found for Minecraft ${config.minecraftVersion}.`)
  }

  const file = chosen.files.find((f) => f.primary) ?? chosen.files[0]
  if (!file?.url) {
    throw new Error('Fabric API version has no downloadable file.')
  }

  await downloadFile(file.url, dest)
  return dest
}

/** Installs Prime Client + Fabric API when includePrimeMod is enabled. */
export async function installInstanceMods(
  instanceId: string,
  config: InstanceLaunchConfig
): Promise<{ primeJar: string | null; fabricApiJar: string | null }> {
  if (!config.includePrimeMod) {
    return { primeJar: null, fabricApiJar: null }
  }

  const modsDir = getInstanceModsDir(instanceId)
  await mkdir(modsDir, { recursive: true })

  emitLaunchProgress({
    phase: 'mods',
    detail: 'Preparing mods folder…',
    percent: 30
  })

  const fabricApiJar = await ensureFabricApi(config, modsDir)

  const primeSource = await resolvePrimeClientSource()
  if (!primeSource) {
    return { primeJar: null, fabricApiJar }
  }

  const primeDestName = basename(primeSource)
  await removeStalePrimeJars(modsDir, primeDestName)

  const primeDest = join(modsDir, primeDestName)
  emitLaunchProgress({
    phase: 'mods',
    detail: 'Installing Prime Client mod…',
    percent: 40
  })
  await copyFile(primeSource, primeDest)
  return { primeJar: primeDest, fabricApiJar }
}

export function primeClientBuildHint(): string {
  return (
    'Build the mod with `.\\gradlew :mc-1.21.11:build`, set PRIME_CLIENT_JAR, ' +
    'or publish a GitHub Release with a prime-client-1.21.11*.jar asset.'
  )
}
