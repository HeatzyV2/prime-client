import { settingsStore } from '../storage/SettingsStore'

/** Limits parallel Modrinth / file downloads based on settings.concurrentDownloads. */
class DownloadQueue {
  private active = 0
  private waiters: Array<() => void> = []

  private async acquire(): Promise<void> {
    const settings = await settingsStore.load()
    const max = Math.max(1, settings.concurrentDownloads)

    if (this.active < max) {
      this.active++
      return
    }

    await new Promise<void>((resolve) => {
      this.waiters.push(() => {
        this.active++
        resolve()
      })
    })
  }

  private release(): void {
    this.active = Math.max(0, this.active - 1)
    const next = this.waiters.shift()
    if (next) {
      next()
    }
  }

  async run<T>(fn: () => Promise<T>): Promise<T> {
    await this.acquire()
    try {
      return await fn()
    } finally {
      this.release()
    }
  }
}

export const downloadQueue = new DownloadQueue()
