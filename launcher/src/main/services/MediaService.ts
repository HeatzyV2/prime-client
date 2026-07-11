import { readdir, stat } from 'fs/promises'
import { join } from 'path'
import { shell } from 'electron'
import type { MediaItem } from '../../shared/content-types'
import { getInstanceGameDir } from '../minecraft/paths'
import { profileService } from './ProfileService'
import { instanceService } from './InstanceService'

function formatSize(bytes: number): string {
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

async function resolveInstanceId(instanceId?: string): Promise<string | null> {
  if (instanceId) {
    return instanceId
  }
  const profile = await profileService.getActiveProfile()
  if (profile.instanceId && (await instanceService.getStoredById(profile.instanceId))) {
    return profile.instanceId
  }
  const fallback = await instanceService.getDefault()
  return fallback?.id ?? null
}

/** Scans instance screenshots folder — replays/clips are placeholders until Prime Client exports them. */
export class MediaService {
  async list(instanceId?: string): Promise<MediaItem[]> {
    const id = await resolveInstanceId(instanceId)
    if (!id) {
      return []
    }

    const screenshotsDir = join(getInstanceGameDir(id), 'screenshots')
    const items: MediaItem[] = []

    try {
      const files = await readdir(screenshotsDir)
      for (const file of files) {
        if (!/\.(png|jpg|jpeg)$/i.test(file)) {
          continue
        }
        const filePath = join(screenshotsDir, file)
        const info = await stat(filePath)
        items.push({
          id: encodeURIComponent(file),
          type: 'screenshot',
          title: file.replace(/\.(png|jpg|jpeg)$/i, ''),
          date: info.mtime.toISOString().slice(0, 10),
          size: formatSize(info.size),
          filePath
        })
      }
    } catch {
      // no screenshots yet
    }

    return items.sort((a, b) => b.date.localeCompare(a.date))
  }

  async openFolder(instanceId?: string): Promise<void> {
    const id = await resolveInstanceId(instanceId)
    if (!id) {
      return
    }
    const dir = join(getInstanceGameDir(id), 'screenshots')
    await shell.openPath(dir)
  }

  async openFile(filePath: string): Promise<void> {
    await shell.openPath(filePath)
  }
}

export const mediaService = new MediaService()
