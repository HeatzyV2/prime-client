import { randomUUID } from 'crypto'
import type { FavoriteServer } from '../../shared/types'
import { ecosystemStore } from '../storage/EcosystemStore'

export function parseServerAddress(address: string): { host: string; port: number } {
  const trimmed = address.trim()
  const colonIdx = trimmed.lastIndexOf(':')
  if (colonIdx > 0 && /^\d+$/.test(trimmed.slice(colonIdx + 1))) {
    return { host: trimmed.slice(0, colonIdx), port: Number(trimmed.slice(colonIdx + 1)) }
  }
  return { host: trimmed, port: 25565 }
}

function validateAddress(address: string): string | null {
  const { host } = parseServerAddress(address)
  if (!host || host.length > 255) {
    return 'Invalid server address.'
  }
  return null
}

function validateName(name: string): string | null {
  const trimmed = name.trim()
  if (trimmed.length < 1 || trimmed.length > 48) {
    return 'Name must be 1–48 characters.'
  }
  return null
}

/** Favorite servers — persisted locally with live status via mcstatus.io. */
export class ServerService {
  async list(): Promise<FavoriteServer[]> {
    const db = await ecosystemStore.load()
    return db.favoriteServers ?? []
  }

  async add(name: string, address: string): Promise<{ ok: boolean; error?: string; server?: FavoriteServer }> {
    const nameErr = validateName(name)
    if (nameErr) {
      return { ok: false, error: nameErr }
    }
    const addrErr = validateAddress(address)
    if (addrErr) {
      return { ok: false, error: addrErr }
    }

    const server: FavoriteServer = {
      id: randomUUID(),
      name: name.trim(),
      address: address.trim()
    }

    await ecosystemStore.mutate((db) => {
      if (!db.favoriteServers) {
        db.favoriteServers = []
      }
      db.favoriteServers.push(server)
    })

    return { ok: true, server }
  }

  async remove(serverId: string): Promise<{ ok: boolean; error?: string }> {
    const db = await ecosystemStore.load()
    if (!db.favoriteServers?.some((s) => s.id === serverId)) {
      return { ok: false, error: 'Server not found.' }
    }

    await ecosystemStore.mutate((db) => {
      db.favoriteServers = (db.favoriteServers ?? []).filter((s) => s.id !== serverId)
    })

    return { ok: true }
  }

  async refreshStatus(serverId: string): Promise<FavoriteServer | null> {
    const db = await ecosystemStore.load()
    const server = db.favoriteServers?.find((s) => s.id === serverId)
    if (!server) {
      return null
    }

    const { host, port } = parseServerAddress(server.address)
    let updated: FavoriteServer = { ...server, ping: undefined, players: 0, maxPlayers: 0 }

    try {
      const start = Date.now()
      const response = await fetch(`https://api.mcstatus.io/v2/status/java/${host}:${port}`, {
        headers: { Accept: 'application/json', 'User-Agent': 'Prime-Launcher' }
      })
      const ping = Date.now() - start

      if (response.ok) {
        const data = (await response.json()) as {
          online?: boolean
          players?: { online?: number; max?: number }
        }
        if (data.online) {
          updated = {
            ...server,
            players: data.players?.online ?? 0,
            maxPlayers: data.players?.max ?? 0,
            ping
          }
        } else {
          updated = { ...server, ping: undefined, players: 0, maxPlayers: 0 }
        }
      }
    } catch {
      // offline or unreachable
    }

    await ecosystemStore.mutate((db) => {
      const idx = db.favoriteServers?.findIndex((s) => s.id === serverId) ?? -1
      if (idx >= 0 && db.favoriteServers) {
        db.favoriteServers[idx] = updated
      }
    })

    return updated
  }

  async refreshAll(): Promise<FavoriteServer[]> {
    const servers = await this.list()
    if (servers.length === 0) {
      return []
    }
    const refreshed = await Promise.all(servers.map((s) => this.refreshStatus(s.id)))
    return refreshed.filter((s): s is FavoriteServer => s !== null)
  }
}

export const serverService = new ServerService()
