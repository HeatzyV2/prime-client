import { app, BrowserWindow, ipcMain } from 'electron'
import { join } from 'path'
import { existsSync } from 'fs'
import { IPC } from '../shared/ipc'
import { registerServiceHandlers, registerSocialEventBridge } from './ipc/handlers'
import { registerMediaScheme, registerMediaProtocol } from './protocol/mediaProtocol'
import { readSettingsSync } from './utils/readSettingsSync'

registerMediaScheme()

const bootSettings = readSettingsSync()
if (!bootSettings.hardwareAccel) {
  app.disableHardwareAcceleration()
}

/** Keep the process alive on unexpected errors — log instead of silent death. */
function installProcessGuards(): void {
  process.on('uncaughtException', (error) => {
    console.error('[prime] uncaughtException', error)
  })
  process.on('unhandledRejection', (reason) => {
    console.error('[prime] unhandledRejection', reason)
  })
}

installProcessGuards()

let mainWindow: BrowserWindow | null = null
let discordStarted = false

function resolveWindowIcon(): string | undefined {
  const candidates = [
    join(__dirname, '../../resources/icon.png'),
    join(process.resourcesPath, 'icon.png')
  ]
  return candidates.find((path) => existsSync(path))
}

async function openDevToolsIfNeeded(): Promise<void> {
  const { settingsStore } = await import('./storage/SettingsStore')
  const settings = await settingsStore.load()
  if (settings.developerMode && mainWindow && !mainWindow.webContents.isDevToolsOpened()) {
    mainWindow.webContents.openDevTools({ mode: 'detach' })
  }
}

function createWindow(): void {
  const icon = resolveWindowIcon()
  mainWindow = new BrowserWindow({
    width: 1320,
    height: 860,
    minWidth: 1100,
    minHeight: 720,
    show: false,
    frame: false,
    backgroundColor: '#060608',
    title: 'Prime Launcher',
    ...(icon ? { icon } : {}),
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
      devTools: true
    }
  })

  mainWindow.on('ready-to-show', () => {
    mainWindow?.show()
    void openDevToolsIfNeeded()
    // Non-critical: Discord RPC after first paint so startup stays responsive.
    void startDiscordWhenReady()
  })

  mainWindow.webContents.setWindowOpenHandler(() => ({ action: 'deny' }))

  if (process.env.ELECTRON_RENDERER_URL) {
    mainWindow.loadURL(process.env.ELECTRON_RENDERER_URL)
  } else {
    mainWindow.loadFile(join(__dirname, '../renderer/index.html'))
  }
}

import { accountStore } from './storage/AccountStore'
import { instanceStore } from './storage/InstanceStore'
import { ecosystemStore } from './storage/EcosystemStore'
import { settingsStore } from './storage/SettingsStore'
import { downloadStore } from './storage/DownloadStore'
import { launcherDiscordService } from './services/LauncherDiscordService'
import { minecraftEngine } from './minecraft/MinecraftEngine'
import { socialService } from './services/SocialService'

async function startDiscordWhenReady(): Promise<void> {
  if (discordStarted) return
  discordStarted = true
  try {
    await launcherDiscordService.start()
  } catch (err) {
    console.error('[prime] Discord RPC start failed', err)
  }
}

app.whenReady().then(async () => {
  registerMediaProtocol()

  // Parallel store loads — independent JSON files, safe to race.
  await Promise.all([
    accountStore.load(),
    instanceStore.load(),
    ecosystemStore.load(),
    settingsStore.load(),
    downloadStore.load()
  ])

  registerServiceHandlers()
  registerSocialEventBridge()
  registerWindowHandlers()
  createWindow()

  // Deferred non-critical network — never block window creation.
  void socialService.ensureSession().catch(() => {
    // offline — social features degrade gracefully
  })

  void fetch(`${(process.env.PRIME_API_BASE || 'http://194.9.172.102:26005').replace(/\/$/, '')}/v1/stats/launch`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ client: 'launcher' })
  }).catch(() => {})

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow()
    }
  })
})

app.on('before-quit', () => {
  if (minecraftEngine.isRunning()) {
    void minecraftEngine.killGame()
  }
  launcherDiscordService.shutdown()
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

function registerWindowHandlers(): void {
  ipcMain.handle(IPC.APP_GET_VERSION, () => app.getVersion())
  ipcMain.handle(IPC.APP_GET_PLATFORM, () => process.platform)
  ipcMain.handle(IPC.APP_RESTART, () => {
    app.relaunch()
    app.exit(0)
  })

  ipcMain.on(IPC.WINDOW_MINIMIZE, () => mainWindow?.minimize())
  ipcMain.on(IPC.WINDOW_MAXIMIZE, () => {
    if (mainWindow?.isMaximized()) {
      mainWindow.unmaximize()
    } else {
      mainWindow?.maximize()
    }
  })
  ipcMain.on(IPC.WINDOW_CLOSE, () => {
    if (minecraftEngine.isRunning()) {
      void minecraftEngine.killGame()
    }
    mainWindow?.close()
  })
}
