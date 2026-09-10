import { spawn, type ChildProcessWithoutNullStreams } from 'child_process'
import { BrowserWindow } from 'electron'
import { createInterface } from 'readline'
import { IPC } from '../../../shared/ipc'
import type { HostConsoleLineDto, HostServerRuntimeStatus, HostStatusEventDto } from '../../../shared/host-types'

const STOP_TIMEOUT_MS = 15_000

type ActiveProcess = {
  serverId: string
  child: ChildProcessWithoutNullStreams
  status: HostServerRuntimeStatus
  stopTimer: NodeJS.Timeout | null
}

function broadcastConsole(payload: HostConsoleLineDto): void {
  for (const win of BrowserWindow.getAllWindows()) {
    win.webContents.send(IPC.HOST_CONSOLE, payload)
  }
}

function broadcastStatus(payload: HostStatusEventDto): void {
  for (const win of BrowserWindow.getAllWindows()) {
    win.webContents.send(IPC.HOST_STATUS, payload)
  }
}

/**
 * Manages at most one Minecraft server process (v1 constraint).
 * Graceful stop: stdin `stop` then force-kill after timeout.
 */
export class ServerProcessManager {
  private active: ActiveProcess | null = null

  getActiveServerId(): string | null {
    return this.active?.serverId ?? null
  }

  getStatus(serverId: string): HostServerRuntimeStatus {
    if (!this.active || this.active.serverId !== serverId) {
      return 'stopped'
    }
    return this.active.status
  }

  isRunning(serverId?: string): boolean {
    if (!this.active) return false
    if (serverId && this.active.serverId !== serverId) return false
    return this.active.status === 'starting' || this.active.status === 'online' || this.active.status === 'stopping'
  }

  private setStatus(status: HostServerRuntimeStatus, detail?: string, exitCode?: number | null): void {
    if (!this.active) return
    this.active.status = status
    broadcastStatus({
      serverId: this.active.serverId,
      status,
      detail,
      exitCode
    })
  }

  private emitLine(serverId: string, stream: HostConsoleLineDto['stream'], line: string): void {
    const trimmed = line.replace(/\r$/, '')
    if (!trimmed) return
    broadcastConsole({
      serverId,
      stream,
      line: trimmed,
      timestamp: new Date().toISOString()
    })
  }

  async start(options: {
    serverId: string
    cwd: string
    javaPath: string
    jarPath: string
    ramMb: number
    jvmArgs?: string[]
  }): Promise<void> {
    if (this.active) {
      if (this.active.serverId === options.serverId) {
        throw new Error('This server is already running.')
      }
      throw new Error(
        `Another local server is already running (${this.active.serverId}). Stop it before starting another.`
      )
    }

    const xmx = Math.max(512, Math.floor(options.ramMb))
    const args = [
      ...(options.jvmArgs ?? []),
      `-Xms${Math.min(1024, xmx)}M`,
      `-Xmx${xmx}M`,
      '-jar',
      options.jarPath,
      'nogui'
    ]

    this.emitLine(options.serverId, 'system', `Starting: ${options.javaPath} ${args.join(' ')}`)

    let child: ChildProcessWithoutNullStreams
    try {
      child = spawn(options.javaPath, args, {
        cwd: options.cwd,
        env: process.env,
        windowsHide: true
      })
    } catch (err) {
      const detail = err instanceof Error ? err.message : String(err)
      throw new Error(`Failed to spawn Java process: ${detail}`)
    }

    this.active = {
      serverId: options.serverId,
      child,
      status: 'starting',
      stopTimer: null
    }
    this.setStatus('starting')

    const stdout = createInterface({ input: child.stdout })
    const stderr = createInterface({ input: child.stderr })

    stdout.on('line', (line) => {
      this.emitLine(options.serverId, 'stdout', line)
      if (this.active?.serverId === options.serverId && this.active.status === 'starting') {
        if (/Done\s*\(/.test(line) || /For help, type "help"/.test(line)) {
          this.setStatus('online')
        }
      }
    })

    stderr.on('line', (line) => {
      this.emitLine(options.serverId, 'stderr', line)
    })

    child.on('error', (err) => {
      this.emitLine(options.serverId, 'system', `Process error: ${err.message}`)
      if (this.active?.serverId === options.serverId) {
        this.setStatus('crashed', err.message)
        this.clearActive()
      }
    })

    child.on('exit', (code, signal) => {
      if (this.active?.serverId !== options.serverId) return
      if (this.active.stopTimer) {
        clearTimeout(this.active.stopTimer)
        this.active.stopTimer = null
      }
      const wasStopping = this.active.status === 'stopping'
      this.emitLine(
        options.serverId,
        'system',
        `Process exited (code=${code ?? 'null'}, signal=${signal ?? 'null'}).`
      )
      if (wasStopping || code === 0) {
        this.setStatus('stopped', undefined, code)
      } else {
        this.setStatus('crashed', `Exit code ${code}`, code)
      }
      this.clearActive()
    })
  }

  sendCommand(serverId: string, command: string): void {
    if (!this.active || this.active.serverId !== serverId) {
      throw new Error('Server is not running.')
    }
    const cleaned = command.replace(/[\r\n]+/g, ' ').trim()
    if (!cleaned) return
    this.active.child.stdin.write(`${cleaned}\n`)
    this.emitLine(serverId, 'system', `> ${cleaned}`)
  }

  async stop(serverId: string): Promise<void> {
    if (!this.active || this.active.serverId !== serverId) {
      return
    }
    if (this.active.status === 'stopping') {
      return
    }

    this.setStatus('stopping')
    this.emitLine(serverId, 'system', 'Sending stop…')

    try {
      this.active.child.stdin.write('stop\n')
    } catch {
      // stdin may already be closed
    }

    const child = this.active.child
    this.active.stopTimer = setTimeout(() => {
      if (!this.active || this.active.serverId !== serverId) return
      this.emitLine(serverId, 'system', `Stop timed out after ${STOP_TIMEOUT_MS / 1000}s — killing process.`)
      try {
        child.kill('SIGKILL')
      } catch {
        // ignore
      }
    }, STOP_TIMEOUT_MS)
  }

  async restart(options: {
    serverId: string
    cwd: string
    javaPath: string
    jarPath: string
    ramMb: number
    jvmArgs?: string[]
  }): Promise<void> {
    if (this.isRunning(options.serverId)) {
      await this.stop(options.serverId)
      await this.waitUntilStopped(options.serverId, STOP_TIMEOUT_MS + 5000)
    }
    await this.start(options)
  }

  private waitUntilStopped(serverId: string, timeoutMs: number): Promise<void> {
    return new Promise((resolve, reject) => {
      const started = Date.now()
      const tick = (): void => {
        if (!this.isRunning(serverId)) {
          resolve()
          return
        }
        if (Date.now() - started > timeoutMs) {
          reject(new Error('Timed out waiting for server to stop.'))
          return
        }
        setTimeout(tick, 200)
      }
      tick()
    })
  }

  private clearActive(): void {
    if (this.active?.stopTimer) {
      clearTimeout(this.active.stopTimer)
    }
    this.active = null
  }
}

export const serverProcessManager = new ServerProcessManager()
