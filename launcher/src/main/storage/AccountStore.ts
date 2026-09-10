import { app } from 'electron'
import { join } from 'path'
import type { LauncherProfile, PrimeAccount } from '../../shared/types'
import type { StoredMinecraftAccount } from './account-types'
import { atomicWriteJson, quarantineCorrupt, readJsonFile } from './atomicWrite'
import { openSecret, sealSecret } from './secretSeal'

export interface AccountDatabase {
  version: 1
  activeAccountId: string | null
  activeProfileId: string
  accounts: StoredMinecraftAccount[]
  primeAccount: PrimeAccount
  profiles: LauncherProfile[]
}

const DEFAULT_DB = (): AccountDatabase => ({
  version: 1,
  activeAccountId: null,
  activeProfileId: 'default',
  accounts: [],
  primeAccount: {
    id: 'prime-local',
    username: 'Guest',
    tier: 'free',
    level: 1,
    createdAt: new Date().toISOString()
  },
  profiles: [
    {
      id: 'default',
      name: 'Default',
      minecraftAccountId: '',
      instanceId: 'prime-fabric',
      playTimeMinutes: 0
    }
  ]
})

function sealAccounts(db: AccountDatabase): AccountDatabase {
  return {
    ...db,
    accounts: db.accounts.map((account) => ({
      ...account,
      msRefreshToken: account.msRefreshToken ? sealSecret(account.msRefreshToken) : account.msRefreshToken
    }))
  }
}

function openAccounts(db: AccountDatabase): AccountDatabase {
  return {
    ...db,
    accounts: db.accounts.map((account) => ({
      ...account,
      msRefreshToken: openSecret(account.msRefreshToken)
    }))
  }
}

export class AccountStore {
  private db: AccountDatabase | null = null

  private get path(): string {
    return join(app.getPath('userData'), 'accounts.json')
  }

  async load(): Promise<AccountDatabase> {
    if (this.db) {
      return this.db
    }
    const parsed = await readJsonFile<AccountDatabase>(this.path)
    if (parsed) {
      this.db = openAccounts(parsed)
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
    await atomicWriteJson(this.path, sealAccounts(this.db))
  }

  async getDb(): Promise<AccountDatabase> {
    return this.load()
  }

  async mutate(fn: (db: AccountDatabase) => void): Promise<AccountDatabase> {
    const db = await this.load()
    fn(db)
    await this.save()
    return db
  }
}

export const accountStore = new AccountStore()
