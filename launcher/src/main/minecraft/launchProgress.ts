import { BrowserWindow } from 'electron'
import { IPC } from '../../shared/ipc'
import type { LaunchProgressDto } from '../../shared/ipc'
import { downloadService } from '../services/DownloadService'

export function emitLaunchProgress(payload: LaunchProgressDto): void {
  downloadService.onLaunchProgress(payload)

  for (const win of BrowserWindow.getAllWindows()) {
    win.webContents.send(IPC.LAUNCH_PROGRESS, payload)
  }
}
