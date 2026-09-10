import { app } from 'electron'
import { join } from 'path'
import { DEFAULT_MINECRAFT_TARGET } from '../../shared/minecraft-targets'
import type { InstanceDatabase, StoredInstance } from './instance-types'
import { atomicWriteJson, quarantineCorrupt, readJsonFile } from './atomicWrite'

const PRIME_DEFAULT: StoredInstance = {
  id: 'prime-fabric',
  name: 'Prime Client',
  minecraftVersion: DEFAULT_MINECRAFT_TARGET.mcVersion,
  loader: 'fabric',
  fabricLoaderVersion: DEFAULT_MINECRAFT_TARGET.fabricLoader,
  fabricApiVersion: DEFAULT_MINECRAFT_TARGET.fabricApi,
  includePrimeMod: true,
  ramMb: 4096,
  jvmArgs: ['-XX:+UseG1GC'],
  isDefault: true,
  createdAt: new Date().toISOString()
}

const DEFAULT_DB = (): InstanceDatabase => ({
  version: 1,
  instances: [PRIME_DEFAULT]
})

export class InstanceStore {
  private db: InstanceDatabase | null = null

  private get path(): string {
    return join(app.getPath('userData'), 'instances.json')
  }

  async load(): Promise<InstanceDatabase> {
    if (this.db) {
      return this.db
    }
    const parsed = await readJsonFile<InstanceDatabase>(this.path)
    if (parsed?.instances?.length) {
      this.db = parsed
    } else {
      if (parsed) {
        // Empty instances array — reset without quarantine.
      } else {
        await quarantineCorrupt(this.path)
      }
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

  async mutate(fn: (db: InstanceDatabase) => void): Promise<InstanceDatabase> {
    const db = await this.load()
    fn(db)
    await this.save()
    return db
  }
}

export const instanceStore = new InstanceStore()
