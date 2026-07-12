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
const VERSION_PATTERN = /prime-client-1\.21\.11-(\d+)\.(\d+)\.(\d+)\.jar$/

interface ModCacheManifest {
  tag: string
  fileName: string
  downloadedAt: string
}

type SemVer = [major: number, minor: number, patch: number]

interface JarCandidate {
  path: string
  version: SemVer
  source: string
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

function parseJarVersion(fileName: string): SemVer | null {
  const match = fileName.match(VERSION_PATTERN)
  if (!match) {
    return null
  }
  return [Number(match[1]), Number(match[2]), Number(match[3])]
}

function compareSemVer(a: SemVer, b: SemVer): number {
  for (let i = 0; i < 3; i++) {
    if (a[i] !== b[i]) {
      return a[i] - b[i]
    }
  }
  return 0
}

function pickNewestCandidate(candidates: JarCandidate[]): JarCandidate | null {
  let best: JarCandidate | null = null
  for (const candidate of candidates) {
    if (!best || compareSemVer(candidate.version, best.version) > 0) {
      best = candidate
    }
  }
  return best
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

async function candidateFromPath(path: string, source: string): Promise<JarCandidate | null> {
  try {
    const info = await stat(path)
    if (!info.isFile() || info.size <= 0) {
      return null
    }
    const version = parseJarVersion(basename(path))
    if (!version) {
      return null
    }
    return { path, version, source }
  } catch {
    return null
  }
}

async function findLocalBuildJar(): Promise<JarCandidate | null> {
  const libsDir = join(getRepoRoot(), 'mc-1.21.11', 'build', 'libs')
  try {
    const files = await readdir(libsDir)
    const matches = files.filter((f) => isPrimeModJarAsset(f, PRIME_JAR_PREFIX))
    let best: JarCandidate | null = null
    for (const file of matches) {
      const candidate = await candidateFromPath(join(libsDir, file), 'local-build')
      if (candidate && (!best || compareSemVer(candidate.version, best.version) > 0)) {
        best = candidate
      }
    }
    return best
  } catch {
    return null
  }
}

async function findEnvOverrideJar(): Promise<JarCandidate | null> {
  if (!process.env.PRIME_CLIENT_JAR) {
    return null
  }
  return candidateFromPath(process.env.PRIME_CLIENT_JAR, 'env-override')
}

async function findCachedGitHubJar(manifest: ModCacheManifest): Promise<JarCandidate | null> {
  return candidateFromPath(join(modCacheDir(), manifest.fileName), 'github-cache')
}

async function findInstalledInstanceJar(instanceId: string): Promise<JarCandidate | null> {
  const modsDir = getInstanceModsDir(instanceId)
  try {
    const files = await readdir(modsDir)
    let best: JarCandidate | null = null
    for (const file of files) {
      if (!isPrimeModJarAsset(file, PRIME_JAR_PREFIX)) {
        continue
      }
      const candidate = await candidateFromPath(join(modsDir, file), 'instance-mods')
      if (candidate && (!best || compareSemVer(candidate.version, best.version) > 0)) {
        best = candidate
      }
    }
    return best
  } catch {
    return null
  }
}

async function downloadPrimeModFromRelease(release: GitHubRelease): Promise<JarCandidate | null> {
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
  return candidateFromPath(dest, 'github-download')
}

/** Resolves the newest Prime Client jar across dev build, instance, cache and GitHub. */
async function resolvePrimeClientSource(instanceId: string): Promise<JarCandidate | null> {
  const candidates: JarCandidate[] = []

  const envJar = await findEnvOverrideJar()
  if (envJar) {
    candidates.push(envJar)
  }

  const localJar = await findLocalBuildJar()
  if (localJar) {
    candidates.push(localJar)
  }

  const installedJar = await findInstalledInstanceJar(instanceId)
  if (installedJar) {
    candidates.push(installedJar)
  }

  const manifest = await readModCacheManifest()
  if (manifest) {
    const cached = await findCachedGitHubJar(manifest)
    if (cached) {
      candidates.push(cached)
    }
  }

  let best = pickNewestCandidate(candidates)

  emitLaunchProgress({
    phase: 'mods',
    detail: 'Checking GitHub Releases for Prime Client mod…',
    percent: 36
  })

  const release = await fetchLatestGitHubRelease()
  if (release) {
    const asset = pickPrimeModAsset(release, PRIME_JAR_PREFIX)
    const remoteVersion = asset ? parseJarVersion(asset.name) : null

    const shouldDownload =
      asset?.browser_download_url &&
      remoteVersion &&
      (!best || compareSemVer(remoteVersion, best.version) > 0)

    if (shouldDownload) {
      const downloaded = await downloadPrimeModFromRelease(release)
      if (downloaded && (!best || compareSemVer(downloaded.version, best.version) > 0)) {
        best = downloaded
      }
    } else if (asset && remoteVersion && !best) {
      const downloaded = await downloadPrimeModFromRelease(release)
      if (downloaded) {
        best = downloaded
      }
    }
  }

  if (best) {
    emitLaunchProgress({
      phase: 'mods',
      detail: `Using Prime Client ${best.version.join('.')} (${best.source})`,
      percent: 39
    })
  }

  return best
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

  const primeCandidate = await resolvePrimeClientSource(instanceId)
  if (!primeCandidate) {
    return { primeJar: null, fabricApiJar }
  }

  const primeDestName = basename(primeCandidate.path)
  await removeStalePrimeJars(modsDir, primeDestName)

  const primeDest = join(modsDir, primeDestName)
  emitLaunchProgress({
    phase: 'mods',
    detail: `Installing Prime Client mod ${primeCandidate.version.join('.')}…`,
    percent: 40
  })
  await copyFile(primeCandidate.path, primeDest)
  return { primeJar: primeDest, fabricApiJar }
}

export function primeClientBuildHint(): string {
  return (
    'Build the mod with `.\\gradlew :mc-1.21.11:build`, set PRIME_CLIENT_JAR, ' +
    'or publish a GitHub Release with a prime-client-1.21.11*.jar asset.'
  )
}
