import { ipcMain } from 'electron'
import { IPC } from '../../shared/ipc'
import { accountService } from '../services/AccountService'
import { profileService } from '../services/ProfileService'
import { instanceService } from '../services/InstanceService'
import { minecraftService } from '../services/MinecraftService'
import { contentService } from '../services/ContentService'
import { cloudService } from '../services/CloudService'
import { cosmeticService } from '../services/CosmeticService'
import { launchService } from '../services/LaunchService'
import { storeService } from '../services/StoreService'
import { friendsService } from '../services/FriendsService'
import { newsService } from '../services/NewsService'
import { mediaService } from '../services/MediaService'
import { performanceService } from '../services/PerformanceService'
import { downloadService } from '../services/DownloadService'
import { settingsService } from '../services/SettingsService'
import { updateService } from '../services/UpdateService'
import type { PerformancePreset } from '../../shared/content-types'
import type { LauncherSettings } from '../storage/SettingsStore'

/** Registers IPC handlers for all launcher services. */
export function registerServiceHandlers(): void {
  ipcMain.handle(IPC.ACCOUNT_GET_PRIME, () => accountService.getPrimeAccount())
  ipcMain.handle(IPC.ACCOUNT_GET_MINECRAFT, () => accountService.getMinecraftAccounts())
  ipcMain.handle(IPC.ACCOUNT_GET_ACTIVE, () => accountService.getActiveAccount())
  ipcMain.handle(IPC.ACCOUNT_SET_ACTIVE, (_e, accountId: string) =>
    accountService.setActiveAccount(accountId)
  )
  ipcMain.handle(IPC.ACCOUNT_LOGIN_MICROSOFT, () => accountService.loginMicrosoft())
  ipcMain.handle(IPC.ACCOUNT_ADD_OFFLINE, (_e, username: string) =>
    accountService.addOffline(username)
  )
  ipcMain.handle(IPC.ACCOUNT_REMOVE, (_e, accountId: string) =>
    accountService.removeAccount(accountId)
  )
  ipcMain.handle(IPC.ACCOUNT_REFRESH_MICROSOFT, (_e, accountId: string) =>
    accountService.refreshMicrosoftAccount(accountId)
  )
  ipcMain.handle(IPC.ACCOUNT_SYNC_PRIME, () => accountService.syncPrimeCloud())

  ipcMain.handle(IPC.LAUNCH_GAME, (_e, instanceId: string) => launchService.launch(instanceId))

  ipcMain.handle(IPC.INSTANCE_LIST, () => instanceService.list())
  ipcMain.handle(IPC.INSTANCE_GET, (_e, id: string) => instanceService.getById(id))
  ipcMain.handle(IPC.INSTANCE_GET_DEFAULT, () => instanceService.getDefault())
  ipcMain.handle(IPC.INSTANCE_CREATE, (_e, input) => instanceService.create(input))
  ipcMain.handle(IPC.INSTANCE_UPDATE, (_e, input) => instanceService.update(input))
  ipcMain.handle(IPC.INSTANCE_DELETE, (_e, id: string, deleteFiles?: boolean) =>
    instanceService.remove(id, deleteFiles)
  )
  ipcMain.handle(IPC.INSTANCE_DUPLICATE, (_e, id: string) => instanceService.duplicate(id))
  ipcMain.handle(IPC.INSTANCE_SET_DEFAULT, (_e, id: string) => instanceService.setDefault(id))
  ipcMain.handle(IPC.INSTANCE_OPEN_FOLDER, (_e, id: string) => instanceService.openFolder(id))
  ipcMain.handle(IPC.PROFILE_SET_INSTANCE, (_e, instanceId: string) =>
    profileService.setActiveInstance(instanceId)
  )

  ipcMain.handle('profile:get-active', () => profileService.getActiveProfile())
  ipcMain.handle('profile:get-all', () => profileService.getProfiles())
  ipcMain.handle('minecraft:get-instances', () => minecraftService.getInstances())
  ipcMain.handle('minecraft:get-news', () => newsService.getNews())
  ipcMain.handle('minecraft:get-favorite-servers', () => minecraftService.getFavoriteServers())

  ipcMain.handle(IPC.CONTENT_MODS_LIST, (_e, instanceId?: string) => contentService.listMods(instanceId))
  ipcMain.handle(IPC.CONTENT_MODS_SET_ENABLED, (_e, fileName: string, enabled: boolean, instanceId?: string) =>
    contentService.setModEnabled(fileName, enabled, instanceId)
  )
  ipcMain.handle(IPC.CONTENT_MODS_REMOVE, (_e, fileName: string, instanceId?: string) =>
    contentService.removeMod(fileName, instanceId)
  )
  ipcMain.handle(IPC.CONTENT_MODS_IMPORT, (_e, instanceId?: string) => contentService.importMod(instanceId))
  ipcMain.handle(
    IPC.CONTENT_MODS_INSTALL_MODRINTH,
    (_e, projectId: string, title: string, instanceId?: string) =>
      contentService.installModFromModrinth(projectId, title, instanceId)
  )

  ipcMain.handle(IPC.CONTENT_RESOURCE_LIST, (_e, instanceId?: string) =>
    contentService.listResourcePacks(instanceId)
  )
  ipcMain.handle(IPC.CONTENT_RESOURCE_SET_ACTIVE, (_e, fileName: string | null, instanceId?: string) =>
    contentService.setResourcePackActive(fileName, instanceId)
  )
  ipcMain.handle(IPC.CONTENT_RESOURCE_REMOVE, (_e, fileName: string, instanceId?: string) =>
    contentService.removeResourcePack(fileName, instanceId)
  )
  ipcMain.handle(IPC.CONTENT_RESOURCE_IMPORT, (_e, instanceId?: string) =>
    contentService.importResourcePack(instanceId)
  )
  ipcMain.handle(
    IPC.CONTENT_RESOURCE_INSTALL_MODRINTH,
    (_e, projectId: string, title: string, instanceId?: string) =>
      contentService.installResourcePackFromModrinth(projectId, title, instanceId)
  )

  ipcMain.handle(IPC.CONTENT_SHADER_LIST, (_e, instanceId?: string) => contentService.listShaders(instanceId))
  ipcMain.handle(IPC.CONTENT_SHADER_SET_ACTIVE, (_e, fileName: string | null, instanceId?: string) =>
    contentService.setShaderActive(fileName, instanceId)
  )
  ipcMain.handle(IPC.CONTENT_SHADER_REMOVE, (_e, fileName: string, instanceId?: string) =>
    contentService.removeShader(fileName, instanceId)
  )
  ipcMain.handle(IPC.CONTENT_SHADER_IMPORT, (_e, instanceId?: string) => contentService.importShader(instanceId))
  ipcMain.handle(
    IPC.CONTENT_SHADER_INSTALL_MODRINTH,
    (_e, projectId: string, title: string, instanceId?: string) =>
      contentService.installShaderFromModrinth(projectId, title, instanceId)
  )

  ipcMain.handle(IPC.CONTENT_MODRINTH_SEARCH, (_e, query: string, type: 'mod' | 'resourcepack' | 'shader', instanceId?: string) =>
    contentService.searchModrinth(query, type, instanceId)
  )

  ipcMain.handle('mod:list', () => contentService.listMods())
  ipcMain.handle('cloud:sync-status', () => cloudService.getSyncStatus())
  ipcMain.handle('cloud:sync', () => cloudService.sync())

  ipcMain.handle(IPC.STORE_CATALOG, () => storeService.getCatalog())
  ipcMain.handle(IPC.STORE_BALANCE, () => storeService.getBalance())
  ipcMain.handle(IPC.STORE_PURCHASE, (_e, itemId: string) => storeService.purchase(itemId))

  ipcMain.handle(IPC.COSMETIC_LIST, () => cosmeticService.list())
  ipcMain.handle(IPC.COSMETIC_TOGGLE, (_e, cosmeticId: string) => cosmeticService.toggleEquip(cosmeticId))

  ipcMain.handle(IPC.FRIENDS_LIST, () => friendsService.list())
  ipcMain.handle(IPC.FRIENDS_ADD, (_e, username: string, note?: string) => friendsService.add(username, note))
  ipcMain.handle(IPC.FRIENDS_REMOVE, (_e, friendId: string) => friendsService.remove(friendId))
  ipcMain.handle(IPC.FRIENDS_UPDATE_NOTE, (_e, friendId: string, note: string) =>
    friendsService.updateNote(friendId, note)
  )

  ipcMain.handle(IPC.NEWS_LIST, () => newsService.getNews())
  ipcMain.handle(IPC.MEDIA_LIST, (_e, instanceId?: string) => mediaService.list(instanceId))
  ipcMain.handle(IPC.MEDIA_OPEN_FOLDER, (_e, instanceId?: string) => mediaService.openFolder(instanceId))
  ipcMain.handle(IPC.MEDIA_OPEN_FILE, (_e, filePath: string) => mediaService.openFile(filePath))

  ipcMain.handle(IPC.PERFORMANCE_HARDWARE, () => performanceService.getHardware())
  ipcMain.handle(IPC.PERFORMANCE_PRESETS, () => performanceService.getPresets())
  ipcMain.handle(IPC.PERFORMANCE_SELECTED, () => performanceService.getSelectedPreset())
  ipcMain.handle(IPC.PERFORMANCE_APPLY, (_e, presetId: PerformancePreset, instanceId?: string) =>
    performanceService.applyPreset(presetId, instanceId)
  )

  ipcMain.handle(IPC.DOWNLOADS_LIST, () => downloadService.list())
  ipcMain.handle(IPC.DOWNLOADS_CLEAR, () => downloadService.clearCompleted())
  ipcMain.handle(IPC.DOWNLOADS_REMOVE, (_e, taskId: string) => downloadService.remove(taskId))

  ipcMain.handle(IPC.SETTINGS_GET, () => settingsService.get())
  ipcMain.handle(IPC.SETTINGS_UPDATE, (_e, partial: Partial<LauncherSettings>) => settingsService.update(partial))

  ipcMain.handle(IPC.UPDATE_CHECK, () => updateService.check())
  ipcMain.handle(IPC.UPDATE_OPEN_RELEASE, (_e, url?: string) => updateService.openReleasePage(url))
}
