import { createWriteStream } from 'fs'
import { copyFile, mkdir, readdir, stat } from 'fs/promises'
import { basename, join } from 'path'
import { pipeline } from 'stream/promises'
import { getInstanceModsDir, getRepoRoot } from './paths'
import { emitLaunchProgress } from './launchProgress'
import type { InstanceLaunchConfig } from './constants'

const FABRIC_API_PROJECT = 'P7dR8mSH'
const PRIME_JAR_PREFIX = 'prime-client-1.21.11'

async function downloadFile(url: string, dest: string): Promise<void> {
  const response = await fetch(url)
  if (!response.ok || !response.body) {
    throw new Error(`Download failed (${response.status}): ${url}`)
  }
  await pipeline(response.body as unknown as NodeJS.ReadableStream, createWriteStream(dest))
}

async function findPrimeClientJar(): Promise<string | null> {
  if (process.env.PRIME_CLIENT_JAR) {
    try {
      const info = await stat(process.env.PRIME_CLIENT_JAR)
      if (info.isFile()) {
        return process.env.PRIME_CLIENT_JAR
      }
    } catch {
      // fall through
    }
  }

  const libsDir = join(getRepoRoot(), 'mc-1.21.11', 'build', 'libs')
  try {
    const files = await readdir(libsDir)
    const match = files
      .filter((f) => f.startsWith(PRIME_JAR_PREFIX) && f.endsWith('.jar'))
      .filter((f) => !f.includes('-sources') && !f.includes('-dev'))
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

  const primeSource = await findPrimeClientJar()
  if (!primeSource) {
    return { primeJar: null, fabricApiJar }
  }

  const primeDest = join(modsDir, basename(primeSource))
  emitLaunchProgress({
    phase: 'mods',
    detail: 'Copying Prime Client mod…',
    percent: 40
  })
  await copyFile(primeSource, primeDest)
  return { primeJar: primeDest, fabricApiJar }
}

export function primeClientBuildHint(): string {
  return 'Build the mod with `.\\gradlew :mc-1.21.11:build` or set PRIME_CLIENT_JAR to the jar path.'
}
