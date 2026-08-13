/** Shared types for Prime Local Server Host (Electron). */

export type HostSoftware = 'paper' | 'purpur' | 'folia' | 'canvas' | 'leaf'

export type HostServerRuntimeStatus = 'stopped' | 'starting' | 'online' | 'stopping' | 'crashed'

export type HostPluginSource = 'modrinth' | 'hangar' | 'spiget'

export const HOST_SOFTWARE_LABELS: Record<HostSoftware, string> = {
  paper: 'Paper',
  purpur: 'Purpur',
  folia: 'Folia',
  canvas: 'Canvas',
  leaf: 'Leaf'
}

export const HOST_EDITABLE_PROPERTIES = [
  'server-port',
  'motd',
  'max-players',
  'online-mode',
  'view-distance',
  'simulation-distance',
  'difficulty',
  'gamemode',
  'white-list',
  'enforce-whitelist',
  'pvp',
  'spawn-protection',
  'max-world-size',
  'level-name',
  'level-type',
  'allow-nether',
  'enable-command-block',
  'spawn-monsters',
  'spawn-animals',
  'spawn-npcs',
  'force-gamemode',
  'hardcore'
] as const

export type HostEditablePropertyKey = (typeof HOST_EDITABLE_PROPERTIES)[number]

/** Persisted metadata in `{userData}/host-servers/{id}/prime-host.json`. */
export interface HostServerMeta {
  id: string
  name: string
  software: HostSoftware
  mcVersion: string
  build: string
  ramMb: number
  port: number
  javaPath?: string | null
  eulaAccepted: boolean
  jarFileName: string
  createdAt: string
  updatedAt: string
}

export interface CreateHostServerDto {
  name: string
  software: HostSoftware
  mcVersion: string
  /** Omit or `"latest"` to pin the newest build at create time. */
  build?: string
  ramMb: number
  port: number
  javaPath?: string | null
  acceptEula?: boolean
}

export interface UpdateHostServerDto {
  id: string
  name?: string
  ramMb?: number
  port?: number
  javaPath?: string | null
}

export interface HostServerView extends HostServerMeta {
  status: HostServerRuntimeStatus
  dir: string
  jarReady: boolean
}

export interface HostSoftwareBuildDto {
  id: string
  channel?: string
  time?: string
  downloadUrl: string
  sha256?: string
  fileName: string
}

export interface HostDownloadProgressDto {
  serverId: string
  phase: 'start' | 'download' | 'done' | 'error'
  percent: number
  detail: string
  speed?: string
}

export interface HostConsoleLineDto {
  serverId: string
  stream: 'stdout' | 'stderr' | 'system'
  line: string
  timestamp: string
}

export interface HostStatusEventDto {
  serverId: string
  status: HostServerRuntimeStatus
  exitCode?: number | null
  detail?: string
}

export interface HostPluginHitDto {
  id: string
  slug?: string
  name: string
  description: string
  downloads: number
  iconUrl?: string
  source: HostPluginSource
}

export interface InstalledHostPluginDto {
  fileName: string
  enabled: boolean
  size: number
}

export interface HostWorldEntryDto {
  name: string
  path: string
  isLevel: boolean
}

export interface HostMutationDto {
  ok: boolean
  error?: string
  server?: HostServerView
}

export interface HostPropertiesDto {
  raw: Record<string, string>
  editable: Partial<Record<HostEditablePropertyKey, string>>
}
