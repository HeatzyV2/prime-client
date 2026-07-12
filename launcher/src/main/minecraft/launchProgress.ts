import { BrowserWindow } from 'electron'
import { IPC } from '../../shared/ipc'
import type { LaunchProgressDto } from '../../shared/ipc'
import { downloadService } from '../services/DownloadService'
import { launchLogService } from '../services/LaunchLogService'
import { launcherDiscordService } from '../services/LauncherDiscordService'

export function emitLaunchProgress(payload: LaunchProgressDto): void {
  downloadService.onLaunchProgress(payload)
  launchLogService.fromProgress(payload)
  launcherDiscordService.onLaunchProgress(payload)

  for (const win of BrowserWindow.getAllWindows()) {
    win.webContents.send(IPC.LAUNCH_PROGRESS, payload)
  }
}

export function emitLaunchError(message: string, err?: unknown): void {
  emitLaunchProgress({ phase: 'error', detail: message })
  if (err instanceof Error && err.stack) {
    launchLogService.append('error', err.stack, 'log')
  }
}
