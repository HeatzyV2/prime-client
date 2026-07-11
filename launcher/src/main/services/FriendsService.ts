import { randomUUID } from 'crypto'
import type { FriendEntry } from '../../shared/content-types'
import { ecosystemStore } from '../storage/EcosystemStore'
import { minecraftEngine } from '../minecraft/MinecraftEngine'
import { accountService } from './AccountService'

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

interface PlayerDbResponse {
  success?: boolean
  data?: {
    player?: {
      online?: boolean
      meta?: { name?: string }
    }
  }
}

async function lookupPlayerStatus(username: string): Promise<{ status: FriendEntry['status']; activity: string }> {
  const active = await accountService.getActiveAccount()
  const mcRunning = minecraftEngine.isRunning()

  if (active && active.username.toLowerCase() === username.toLowerCase() && mcRunning) {
    return { status: 'in-game', activity: 'Playing on this PC' }
  }

  try {
    const response = await fetch(`https://playerdb.co/api/player/minecraft/${encodeURIComponent(username)}`, {
      headers: { Accept: 'application/json', 'User-Agent': 'Prime-Launcher' }
    })
    if (response.ok) {
      const data = (await response.json()) as PlayerDbResponse
      if (data.success && data.data?.player) {
        if (data.data.player.online) {
          return { status: 'online', activity: 'Online on Minecraft' }
        }
        return { status: 'offline', activity: 'Offline' }
      }
    }
  } catch {
    // fall through
  }

  try {
    const response = await fetch(
      `https://api.mojang.com/users/profiles/minecraft/${encodeURIComponent(username)}`,
      { headers: { Accept: 'application/json', 'User-Agent': 'Prime-Launcher' } }
    )
    if (response.ok) {
      return { status: 'offline', activity: 'Valid Minecraft account' }
    }
  } catch {
    // ignore
  }

  return { status: 'offline', activity: 'Could not verify account' }
}

/** Local friends roster with live status via PlayerDB + Mojang profile lookup. */
export class FriendsService {
  async list(): Promise<FriendEntry[]> {
    const db = await ecosystemStore.load()
    return db.friends
  }

  async refreshAllStatuses(): Promise<FriendEntry[]> {
    const db = await ecosystemStore.load()
    const updated: FriendEntry[] = []

    for (const friend of db.friends) {
      const live = await lookupPlayerStatus(friend.username)
      updated.push({
        ...friend,
        status: live.status,
        activity: friend.activity?.startsWith('Added locally') ? live.activity : friend.activity || live.activity
      })
    }

    await ecosystemStore.mutate((db) => {
      db.friends = updated
    })

    return updated
  }

  async refreshStatus(friendId: string): Promise<FriendEntry | null> {
    const db = await ecosystemStore.load()
    const friend = db.friends.find((f) => f.id === friendId)
    if (!friend) {
      return null
    }

    const live = await lookupPlayerStatus(friend.username)
    const updated: FriendEntry = {
      ...friend,
      status: live.status,
      activity: friend.activity?.startsWith('Added locally') ? live.activity : friend.activity || live.activity
    }

    await ecosystemStore.mutate((db) => {
      const idx = db.friends.findIndex((f) => f.id === friendId)
      if (idx >= 0) {
        db.friends[idx] = updated
      }
    })

    return updated
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

    const live = await lookupPlayerStatus(username.trim())

    await ecosystemStore.mutate((db) => {
      db.friends.push({
        id: randomUUID(),
        username: username.trim(),
        status: live.status,
        activity: note?.trim() || live.activity
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
