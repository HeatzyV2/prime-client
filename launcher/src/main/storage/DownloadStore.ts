import { app } from 'electron'
import { join } from 'path'
import type { DownloadTask } from '../../shared/content-types'
import { atomicWriteJson, quarantineCorrupt, readJsonFile } from './atomicWrite'

export interface DownloadDatabase {
  version: 1
  tasks: DownloadTask[]
}

const DEFAULT_DB = (): DownloadDatabase => ({
  version: 1,
  tasks: []
})

export class DownloadStore {
  private db: DownloadDatabase | null = null

  private get path(): string {
    return join(app.getPath('userData'), 'downloads.json')
  }

  async load(): Promise<DownloadDatabase> {
    if (this.db) {
      return this.db
    }
    const parsed = await readJsonFile<DownloadDatabase>(this.path)
    if (parsed) {
      this.db = parsed
    } else {
      await quarantineCorrupt(this.path)
      this.db = DEFAULT_DB()
      await this.save()
    }
    return this.db!
  }

  async save(): Promise<void> {
    if (!this.db) {
      return
    }
    await atomicWriteJson(this.path, this.db)
  }

  async mutate(fn: (db: DownloadDatabase) => void): Promise<DownloadDatabase> {
    const db = await this.load()
    fn(db)
    await this.save()
    return db
  }
}

export const downloadStore = new DownloadStore()
