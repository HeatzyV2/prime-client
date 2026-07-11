import { randomUUID } from 'crypto'
import type { FriendEntry } from '../../shared/content-types'
import { ecosystemStore } from '../storage/EcosystemStore'

function validateUsername(username: string): string | null {
  const trimmed = username.trim()
  if (trimmed.length < 3 || trimmed.length > 16) {
    return 'Username must be 3–16 characters.'
  }
  if (!/^[a-zA-Z0-9_]+$/.test(trimmed)) {
    return 'Only letters, numbers, and underscores.'
  }
  return null
}

/** Local friends roster — no online API, notes stored on disk. */
export class FriendsService {
  async list(): Promise<FriendEntry[]> {
    const db = await ecosystemStore.load()
    return db.friends
  }

  async add(username: string, note?: string): Promise<{ ok: boolean; error?: string }> {
    const err = validateUsername(username)
    if (err) {
      return { ok: false, error: err }
    }

    const db = await ecosystemStore.load()
    if (db.friends.some((f) => f.username.toLowerCase() === username.trim().toLowerCase())) {
      return { ok: false, error: 'Friend already in list.' }
    }

    await ecosystemStore.mutate((db) => {
      db.friends.push({
        id: randomUUID(),
        username: username.trim(),
        status: 'offline',
        activity: note?.trim() || 'Added locally — no live status without a server'
      })
    })

    return { ok: true }
  }

  async remove(friendId: string): Promise<{ ok: boolean; error?: string }> {
    const db = await ecosystemStore.load()
    if (!db.friends.some((f) => f.id === friendId)) {
      return { ok: false, error: 'Friend not found.' }
    }

    await ecosystemStore.mutate((db) => {
      db.friends = db.friends.filter((f) => f.id !== friendId)
    })

    return { ok: true }
  }

  async updateNote(friendId: string, activity: string): Promise<{ ok: boolean; error?: string }> {
    await ecosystemStore.mutate((db) => {
      const friend = db.friends.find((f) => f.id === friendId)
      if (friend) {
        friend.activity = activity.trim()
      }
    })
    return { ok: true }
  }
}

export const friendsService = new FriendsService()
