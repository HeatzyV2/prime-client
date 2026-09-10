/** Supported Minecraft targets for Prime Client instances. */

export interface MinecraftTarget {
  id: string
  mcVersion: string
  jarPrefix: string
  localBuildDir: string
  fabricApi: string
  fabricLoader: string
  /** Recommended Java major for this target. */
  javaMajor: number
  /** Shown as recommended in the UI. */
  recommended?: boolean
}

export const MINECRAFT_TARGETS: readonly MinecraftTarget[] = [
  {
    id: '26.2',
    mcVersion: '26.2',
    jarPrefix: 'prime-client-26.2',
    localBuildDir: 'mc-26.2',
    fabricApi: '0.160.0+26.2',
    fabricLoader: '0.19.3',
    javaMajor: 25,
    recommended: true
  },
  {
    id: '26.1',
    mcVersion: '26.1',
    jarPrefix: 'prime-client-26.1',
    localBuildDir: 'mc-26.1',
    fabricApi: '0.145.1+26.1',
    fabricLoader: '0.19.3',
    javaMajor: 25
  },
  {
    id: '1.21.11',
    mcVersion: '1.21.11',
    jarPrefix: 'prime-client-1.21.11',
    localBuildDir: 'mc-1.21.11',
    fabricApi: '0.141.6+1.21.11',
    fabricLoader: '0.19.3',
    javaMajor: 21
  },
  {
    id: '1.21.10',
    mcVersion: '1.21.10',
    jarPrefix: 'prime-client-1.21.10',
    localBuildDir: 'mc-1.21.10',
    fabricApi: '0.138.4+1.21.10',
    fabricLoader: '0.19.3',
    javaMajor: 21
  },
  {
    id: '1.21.9',
    mcVersion: '1.21.9',
    jarPrefix: 'prime-client-1.21.9',
    localBuildDir: 'mc-1.21.9',
    fabricApi: '0.134.1+1.21.9',
    fabricLoader: '0.19.3',
    javaMajor: 21
  },
  {
    id: '1.21.8',
    mcVersion: '1.21.8',
    jarPrefix: 'prime-client-1.21.8',
    localBuildDir: 'mc-1.21.8',
    fabricApi: '0.136.1+1.21.8',
    fabricLoader: '0.19.3',
    javaMajor: 21
  },
  {
    id: '1.21.7',
    mcVersion: '1.21.7',
    jarPrefix: 'prime-client-1.21.7',
    localBuildDir: 'mc-1.21.7',
    fabricApi: '0.129.0+1.21.7',
    fabricLoader: '0.19.3',
    javaMajor: 21
  },
  {
    id: '1.21.6',
    mcVersion: '1.21.6',
    jarPrefix: 'prime-client-1.21.6',
    localBuildDir: 'mc-1.21.6',
    fabricApi: '0.128.2+1.21.6',
    fabricLoader: '0.19.3',
    javaMajor: 21
  },
  {
    id: '1.21.5',
    mcVersion: '1.21.5',
    jarPrefix: 'prime-client-1.21.5',
    localBuildDir: 'mc-1.21.5',
    fabricApi: '0.128.2+1.21.5',
    fabricLoader: '0.19.3',
    javaMajor: 21
  },
  {
    id: '1.21.4',
    mcVersion: '1.21.4',
    jarPrefix: 'prime-client-1.21.4',
    localBuildDir: 'mc-1.21.4',
    fabricApi: '0.119.4+1.21.4',
    fabricLoader: '0.19.3',
    javaMajor: 21
  }
] as const

export const DEFAULT_MINECRAFT_TARGET = MINECRAFT_TARGETS[0]

/** Normalize user input and resolve a known Prime target (falls back to recommended). */
export function resolveTarget(minecraftVersion: string | null | undefined): MinecraftTarget {
  const raw = (minecraftVersion ?? '').trim()
  if (!raw) {
    return DEFAULT_MINECRAFT_TARGET
  }
  const exact = MINECRAFT_TARGETS.find(
    (t) => t.mcVersion === raw || t.id === raw || t.localBuildDir === raw
  )
  if (exact) {
    return exact
  }
  // Soft match: "26.2.0" → 26.2, "1.21.11-rc" → 1.21.11
  const soft = MINECRAFT_TARGETS.find(
    (t) => raw === t.mcVersion || raw.startsWith(`${t.mcVersion}.`) || raw.startsWith(`${t.mcVersion}-`)
  )
  return soft ?? DEFAULT_MINECRAFT_TARGET
}

export function isSupportedPrimeVersion(minecraftVersion: string | null | undefined): boolean {
  const raw = (minecraftVersion ?? '').trim()
  return MINECRAFT_TARGETS.some((t) => t.mcVersion === raw || t.id === raw)
}

export function primeJarPrefix(minecraftVersion: string | null | undefined): string {
  return resolveTarget(minecraftVersion).jarPrefix
}

/** Parse `prime-client-<target>-1.2.63.jar` → [1,2,63]. */
export function parsePrimeJarSemVer(fileName: string, jarPrefix: string): [number, number, number] | null {
  const escaped = jarPrefix.replace(/\./g, '\\.')
  const match = fileName.match(new RegExp(`^${escaped}-(\\d+)\\.(\\d+)\\.(\\d+)\\.jar$`))
  if (!match) {
    return null
  }
  return [Number(match[1]), Number(match[2]), Number(match[3])]
}

/** All known Prime jar prefixes (for cleaning stale jars when switching MC version). */
export function allPrimeJarPrefixes(): string[] {
  return MINECRAFT_TARGETS.map((t) => t.jarPrefix)
}
