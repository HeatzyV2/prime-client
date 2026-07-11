/** IPC channel names — single source of truth for main ↔ renderer. */
export const IPC = {
  WINDOW_MINIMIZE: 'window:minimize',
  WINDOW_MAXIMIZE: 'window:maximize',
  WINDOW_CLOSE: 'window:close',
  APP_GET_VERSION: 'app:get-version',
  APP_GET_PLATFORM: 'app:get-platform',
  BOOT_COMPLETE: 'boot:complete',

  ACCOUNT_GET_PRIME: 'account:get-prime',
  ACCOUNT_GET_MINECRAFT: 'account:get-minecraft',
  ACCOUNT_GET_ACTIVE: 'account:get-active',
  ACCOUNT_SET_ACTIVE: 'account:set-active',
  ACCOUNT_LOGIN_MICROSOFT: 'account:login-microsoft',
  ACCOUNT_ADD_OFFLINE: 'account:add-offline',
  ACCOUNT_REMOVE: 'account:remove',
  ACCOUNT_REFRESH_MICROSOFT: 'account:refresh-microsoft',
  ACCOUNT_SYNC_PRIME: 'account:sync-prime',
  LAUNCH_GAME: 'launch:game',
  /** Main → renderer progress while downloading / launching. */
  LAUNCH_PROGRESS: 'launch:progress',

  INSTANCE_LIST: 'instance:list',
  INSTANCE_GET: 'instance:get',
  INSTANCE_GET_DEFAULT: 'instance:get-default',
  INSTANCE_CREATE: 'instance:create',
  INSTANCE_UPDATE: 'instance:update',
  INSTANCE_DELETE: 'instance:delete',
  INSTANCE_DUPLICATE: 'instance:duplicate',
  INSTANCE_SET_DEFAULT: 'instance:set-default',
  INSTANCE_OPEN_FOLDER: 'instance:open-folder',

  PROFILE_SET_INSTANCE: 'profile:set-instance',

  CONTENT_MODS_LIST: 'content:mods-list',
  CONTENT_MODS_SET_ENABLED: 'content:mods-set-enabled',
  CONTENT_MODS_REMOVE: 'content:mods-remove',
  CONTENT_MODS_IMPORT: 'content:mods-import',
  CONTENT_MODS_INSTALL_MODRINTH: 'content:mods-install-modrinth',
  CONTENT_RESOURCE_LIST: 'content:resource-list',
  CONTENT_RESOURCE_SET_ACTIVE: 'content:resource-set-active',
  CONTENT_RESOURCE_REMOVE: 'content:resource-remove',
  CONTENT_RESOURCE_IMPORT: 'content:resource-import',
  CONTENT_RESOURCE_INSTALL_MODRINTH: 'content:resource-install-modrinth',
  CONTENT_SHADER_LIST: 'content:shader-list',
  CONTENT_SHADER_SET_ACTIVE: 'content:shader-set-active',
  CONTENT_SHADER_REMOVE: 'content:shader-remove',
  CONTENT_SHADER_IMPORT: 'content:shader-import',
  CONTENT_SHADER_INSTALL_MODRINTH: 'content:shader-install-modrinth',
  CONTENT_MODRINTH_SEARCH: 'content:modrinth-search',

  STORE_CATALOG: 'store:catalog',
  STORE_BALANCE: 'store:balance',
  STORE_PURCHASE: 'store:purchase',
  COSMETIC_LIST: 'cosmetic:list',
  COSMETIC_TOGGLE: 'cosmetic:toggle',
  FRIENDS_LIST: 'friends:list',
  FRIENDS_ADD: 'friends:add',
  FRIENDS_REMOVE: 'friends:remove',
  FRIENDS_UPDATE_NOTE: 'friends:update-note',
  NEWS_LIST: 'news:list',
  MEDIA_LIST: 'media:list',
  MEDIA_OPEN_FOLDER: 'media:open-folder',
  MEDIA_OPEN_FILE: 'media:open-file',
  PERFORMANCE_HARDWARE: 'performance:hardware',
  PERFORMANCE_PRESETS: 'performance:presets',
  PERFORMANCE_SELECTED: 'performance:selected',
  PERFORMANCE_APPLY: 'performance:apply',
  DOWNLOADS_LIST: 'downloads:list',
  DOWNLOADS_CLEAR: 'downloads:clear',
  DOWNLOADS_REMOVE: 'downloads:remove',
  SETTINGS_GET: 'settings:get',
  SETTINGS_UPDATE: 'settings:update',
  UPDATE_CHECK: 'update:check',
  UPDATE_OPEN_RELEASE: 'update:open-release'
} as const

export type IpcChannel = (typeof IPC)[keyof typeof IPC]

export interface AuthResultDto {
  ok: boolean
  error?: string
  accountId?: string
}

export interface LaunchResultDto {
  ok: boolean
  message: string
  error?: string
}

export interface SyncResultDto {
  ok: boolean
  lastSync: string
  message: string
}

export interface LaunchProgressDto {
  phase: 'start' | 'fabric' | 'mods' | 'download' | 'launch' | 'running' | 'log'
  detail: string
  percent?: number
}

export interface CreateInstanceDto {
  name: string
  minecraftVersion: string
  loader: 'vanilla' | 'fabric'
  fabricLoaderVersion?: string
  fabricApiVersion?: string
  includePrimeMod?: boolean
  ramMb: number
  jvmArgs?: string[]
}

export interface UpdateInstanceDto {
  id: string
  name?: string
  minecraftVersion?: string
  loader?: 'vanilla' | 'fabric'
  fabricLoaderVersion?: string
  fabricApiVersion?: string
  includePrimeMod?: boolean
  ramMb?: number
  javaPath?: string
  jvmArgs?: string[]
}

export interface InstanceMutationDto {
  ok: boolean
  error?: string
}

export interface ModrinthSearchHitDto {
  project_id: string
  slug: string
  title: string
  description: string
  downloads: number
  icon_url?: string
  project_type: string
}

export interface ContentMutationDto {
  ok: boolean
  error?: string
  fileName?: string
}

export interface UpdateCheckDto {
  current: string
  latest: string
  updateAvailable: boolean
  notes: string
  checkedAt: string
  releaseUrl?: string
  downloadUrl?: string
}
