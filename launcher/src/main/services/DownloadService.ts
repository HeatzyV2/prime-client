import { randomUUID } from 'crypto'
import type { DownloadTask } from '../../shared/content-types'
import type { LaunchProgressDto } from '../../shared/ipc'
import { downloadStore } from '../storage/DownloadStore'

const ACTIVE_LAUNCH_ID = 'active-minecraft-launch'

/** Persists download / launch progress locally for the Download Center. */
export class DownloadService {
  async list(): Promise<DownloadTask[]> {
    const db = await downloadStore.load()
    return db.tasks.sort((a, b) => {
      if (a.status === 'downloading' && b.status !== 'downloading') {
        return -1
      }
      if (b.status === 'downloading' && a.status !== 'downloading') {
        return 1
      }
      return 0
    })
  }

  async clearCompleted(): Promise<void> {
    await downloadStore.mutate((db) => {
      db.tasks = db.tasks.filter((t) => t.status !== 'completed')
    })
  }

  async remove(taskId: string): Promise<void> {
    await downloadStore.mutate((db) => {
      db.tasks = db.tasks.filter((t) => t.id !== taskId)
    })
  }

  onLaunchProgress(payload: LaunchProgressDto): void {
    void this.trackProgress(payload)
  }

  private async trackProgress(payload: LaunchProgressDto): Promise<void> {
    if (payload.phase === 'log') {
      return
    }

    const progress = payload.percent ?? (payload.phase === 'running' ? 100 : 0)
    const status: DownloadTask['status'] =
      payload.phase === 'running'
        ? 'completed'
        : payload.phase === 'start'
          ? 'queued'
          : 'downloading'

    await downloadStore.mutate((db) => {
      let task = db.tasks.find((t) => t.id === ACTIVE_LAUNCH_ID)
      if (!task) {
        task = {
          id: ACTIVE_LAUNCH_ID,
          name: 'Minecraft launch',
          progress: 0,
          speed: '—',
          size: '—',
          eta: '—',
          status: 'queued'
        }
        db.tasks.unshift(task)
      }

      task.name = payload.detail.slice(0, 80) || 'Minecraft launch'
      task.progress = progress
      task.status = status
      task.speed = status === 'downloading' ? 'CDN' : '—'
      task.eta = status === 'completed' ? '—' : status === 'downloading' ? '…' : '—'

      if (status === 'completed') {
        task.id = randomUUID()
        task.name = 'Last launch completed'
      }
    })
  }
}

export const downloadService = new DownloadService()
