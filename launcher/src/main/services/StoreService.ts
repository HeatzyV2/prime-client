import type { StoreItem } from '../../shared/content-types'
import { STORE_CATALOG, STORE_TO_COSMETIC } from '../../shared/ecosystem-catalog'
import { ecosystemStore } from '../storage/EcosystemStore'
import { settingsStore } from '../storage/SettingsStore'

export class StoreService {
  async getCatalog(): Promise<StoreItem[]> {
    const db = await ecosystemStore.load()
    return STORE_CATALOG.map((item) => ({
      ...item,
      owned: db.ownedStoreItems.includes(item.id)
    }))
  }

  async getBalance(): Promise<number> {
    const db = await ecosystemStore.load()
    return db.primeCoins
  }

  async purchase(itemId: string): Promise<{ ok: boolean; error?: string }> {
    const item = STORE_CATALOG.find((i) => i.id === itemId)
    if (!item) {
      return { ok: false, error: 'Item not found.' }
    }

    const db = await ecosystemStore.load()
    if (db.ownedStoreItems.includes(itemId)) {
      return { ok: false, error: 'Already owned.' }
    }

    if (item.price > 0 && db.primeCoins < item.price) {
      return { ok: false, error: `Need ${item.price} Prime Coins (you have ${db.primeCoins}).` }
    }

    await ecosystemStore.mutate((db) => {
      if (item.price > 0) {
        db.primeCoins -= item.price
      }
      db.ownedStoreItems.push(itemId)
    })

    if (itemId === 'bg-nebula') {
      await settingsStore.mutate((s) => {
        s.backgroundNebula = true
      })
    }

    return { ok: true }
  }

  async grantLaunchReward(): Promise<void> {
    await ecosystemStore.mutate((db) => {
      db.primeCoins += 10
    })
  }
}

export const storeService = new StoreService()
