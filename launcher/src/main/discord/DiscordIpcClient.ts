import net from 'net'
import type { DiscordPresencePayload } from './types'

const OP_HANDSHAKE = 0
const OP_FRAME = 1
const OP_CLOSE = 2

function trim(text: string, max: number): string {
  if (text.length <= max) {
    return text
  }
  return `${text.slice(0, Math.max(0, max - 1))}…`
}

/** Minimal Discord Rich Presence IPC — Windows named pipes + Unix sockets. */
export class DiscordIpcClient {
  private socket: net.Socket | null = null
  private connected = false
  private lastConnectAttempt = 0
  private recvBuffer = Buffer.alloc(0)
  private readonly queue: Array<() => Promise<void>> = []
  private draining = false

  constructor(private readonly applicationId: string) {}

  isConnected(): boolean {
    return this.connected
  }

  async connect(): Promise<boolean> {
    if (this.connected) {
      return true
    }
    const now = Date.now()
    if (now - this.lastConnectAttempt < 3000) {
      return false
    }
    this.lastConnectAttempt = now
    this.close()

    for (let i = 0; i < 10; i++) {
      try {
        await this.openTransport(i)
        await this.handshake()
        this.connected = true
        return true
      } catch {
        this.close()
      }
    }
    return false
  }

  async setActivity(payload: DiscordPresencePayload): Promise<boolean> {
    if (!(await this.ensureConnected())) {
      return false
    }
    try {
      await this.sendFrame(this.buildSetActivity(payload))
      return true
    } catch {
      this.connected = false
      this.close()
      return false
    }
  }

  async clearActivity(): Promise<void> {
    if (!this.connected) {
      return
    }
    try {
      await this.sendFrame(this.buildClearActivity())
    } catch {
      this.connected = false
      this.close()
    }
  }

  shutdown(): void {
    void this.enqueue(async () => {
      if (this.connected) {
        try {
          await this.sendFrame(this.buildClearActivity())
        } catch {
          // ignore
        }
      }
      this.close()
    })
  }

  private async ensureConnected(): Promise<boolean> {
    if (this.connected) {
      return true
    }
    return this.connect()
  }

  private openTransport(index: number): Promise<void> {
    return new Promise((resolve, reject) => {
      const path =
        process.platform === 'win32'
          ? `\\\\.\\pipe\\discord-ipc-${index}`
          : `${process.env.XDG_RUNTIME_DIR || '/tmp'}/discord-ipc-${index}`

      const socket = net.connect(path)
      this.socket = socket
      this.recvBuffer = Buffer.alloc(0)

      socket.once('error', reject)
      socket.once('connect', () => {
        socket.off('error', reject)
        socket.on('data', (chunk: Buffer) => {
          this.recvBuffer = Buffer.concat([this.recvBuffer, chunk])
        })
        socket.on('error', () => {
          this.connected = false
        })
        socket.on('close', () => {
          this.connected = false
        })
        resolve()
      })
    })
  }

  private async handshake(): Promise<void> {
    const payload = JSON.stringify({ v: 1, client_id: this.applicationId })
    await this.writePacket(OP_HANDSHAKE, payload)
    await this.readPacket()
  }

  private buildSetActivity(snapshot: DiscordPresencePayload): string {
    const activity: Record<string, unknown> = { type: 0 }
    if (snapshot.details) {
      activity.details = trim(snapshot.details, 128)
    }
    if (snapshot.state) {
      activity.state = trim(snapshot.state, 128)
    }
    if (snapshot.startTimestamp) {
      activity.timestamps = { start: snapshot.startTimestamp }
    }

    activity.assets = {
      large_image: snapshot.largeImageKey ?? 'prime_logo',
      large_text: trim(snapshot.largeImageText ?? 'Prime Client', 128),
      small_image: snapshot.smallImageKey ?? 'prime_logo',
      ...(snapshot.smallImageText ? { small_text: trim(snapshot.smallImageText, 128) } : {})
    }

    if (snapshot.buttons?.length) {
      const buttons: string[] = []
      const labels: string[] = []
      for (const button of snapshot.buttons.slice(0, 2)) {
        buttons.push(button.url)
        labels.push(trim(button.label, 32))
      }
      activity.buttons = buttons
      activity.metadata = { button_label: labels }
    }

    return JSON.stringify({
      cmd: 'SET_ACTIVITY',
      nonce: `${Date.now()}`,
      args: {
        pid: process.pid,
        activity
      }
    })
  }

  private buildClearActivity(): string {
    return JSON.stringify({
      cmd: 'SET_ACTIVITY',
      nonce: `${Date.now()}`,
      args: {
        pid: process.pid,
        activity: null
      }
    })
  }

  private async sendFrame(json: string): Promise<void> {
    await this.writePacket(OP_FRAME, json)
    await this.readPacket()
  }

  private writePacket(opcode: number, json: string): Promise<void> {
    const body = Buffer.from(json, 'utf8')
    const header = Buffer.alloc(8)
    header.writeInt32LE(opcode, 0)
    header.writeInt32LE(body.length, 4)
    return this.write(Buffer.concat([header, body]))
  }

  private write(data: Buffer): Promise<void> {
    return new Promise((resolve, reject) => {
      if (!this.socket) {
        reject(new Error('Discord IPC not connected'))
        return
      }
      this.socket.write(data, (err) => {
        if (err) {
          this.connected = false
          reject(err)
        } else {
          resolve()
        }
      })
    })
  }

  private readPacket(): Promise<void> {
    return new Promise((resolve, reject) => {
      const tryRead = (): void => {
        if (this.recvBuffer.length < 8) {
          return
        }
        const opcode = this.recvBuffer.readInt32LE(0)
        const length = this.recvBuffer.readInt32LE(4)
        if (this.recvBuffer.length < 8 + length) {
          return
        }
        this.recvBuffer = this.recvBuffer.subarray(8 + length)
        if (opcode === OP_CLOSE) {
          this.connected = false
          reject(new Error('Discord IPC closed'))
          return
        }
        cleanup()
        resolve()
      }

      const onData = (): void => {
        tryRead()
      }

      const cleanup = (): void => {
        this.socket?.off('data', onData)
        clearTimeout(timer)
      }

      const timer = setTimeout(() => {
        cleanup()
        reject(new Error('Discord IPC read timeout'))
      }, 5000)

      this.socket?.on('data', onData)
      tryRead()
    })
  }

  private enqueue(task: () => Promise<void>): Promise<void> {
    return new Promise((resolve, reject) => {
      this.queue.push(async () => {
        try {
          await task()
          resolve()
        } catch (err) {
          reject(err)
        }
      })
      void this.drainQueue()
    })
  }

  private async drainQueue(): Promise<void> {
    if (this.draining) {
      return
    }
    this.draining = true
    while (this.queue.length > 0) {
      const task = this.queue.shift()
      if (task) {
        try {
          await task()
        } catch {
          this.connected = false
          this.close()
        }
      }
    }
    this.draining = false
  }

  private close(): void {
    this.connected = false
    this.recvBuffer = Buffer.alloc(0)
    if (this.socket) {
      try {
        this.socket.destroy()
      } catch {
        // ignore
      }
      this.socket = null
    }
  }
}
