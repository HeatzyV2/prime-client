import { mkdir } from 'fs/promises'
import { Client } from 'minecraft-launcher-core'
import type { ChildProcessWithoutNullStreams } from 'child_process'
import { fabricVersionId, type InstanceLaunchConfig } from './constants'
import { resolveJavaPath } from './JavaService'
import { installInstanceMods, primeClientBuildHint } from './ModPackService'
import { resolveLaunchAuthorization } from './LaunchAuth'
import { emitLaunchProgress } from './launchProgress'
import { getInstanceGameDir, getRuntimeRoot } from './paths'
import { accountService } from '../services/AccountService'
import { instanceService } from '../services/InstanceService'

let activeProcess: ChildProcessWithoutNullStreams | null = null

async function ensureFabricProfile(config: InstanceLaunchConfig): Promise<string> {
  const runtimeRoot = getRuntimeRoot()
  await mkdir(runtimeRoot, { recursive: true })

  const versionId = fabricVersionId(config)
  emitLaunchProgress({
    phase: 'fabric',
    detail: `Installing Fabric ${config.fabricLoaderVersion} for ${config.minecraftVersion}…`,
    percent: 10
  })

  const { installFabric } = await import('@xmcl/installer')
  await installFabric({
    minecraftVersion: config.minecraftVersion,
    version: config.fabricLoaderVersion,
    minecraft: runtimeRoot,
    side: 'client'
  })

  return versionId
}

function attachClientEvents(client: Client): void {
  client.on('progress', (progress) => {
    emitLaunchProgress({
      phase: 'download',
      detail: typeof progress === 'string' ? progress : 'Downloading Minecraft files…'
    })
  })

  client.on('data', (line) => {
    emitLaunchProgress({ phase: 'log', detail: String(line) })
  })
}

async function resolveVersionId(config: InstanceLaunchConfig): Promise<string> {
  if (config.loader === 'vanilla') {
    emitLaunchProgress({
      phase: 'fabric',
      detail: `Preparing Minecraft ${config.minecraftVersion}…`,
      percent: 10
    })
    return config.minecraftVersion
  }

  if (config.loader === 'fabric') {
    return ensureFabricProfile(config)
  }

  throw new Error('Unsupported loader. Use Vanilla or Fabric.')
}

export class MinecraftEngine {
  isRunning(): boolean {
    return activeProcess !== null && !activeProcess.killed
  }

  async launchInstance(instanceId: string): Promise<{ username: string; primeModInstalled: boolean }> {
    const stored = await instanceService.getStoredById(instanceId)
    if (!stored) {
      throw new Error(`Unknown instance "${instanceId}".`)
    }

    const config = instanceService.toLaunchConfig(stored)
    const account = await accountService.getStoredActiveAccount()
    if (!account) {
      throw new Error('No account selected.')
    }

    emitLaunchProgress({ phase: 'start', detail: 'Preparing launch…', percent: 0 })

    const authorization = await resolveLaunchAuthorization(account)
    const versionId = await resolveVersionId(config)

    let primeModInstalled = false
    if (config.loader === 'fabric' && config.includePrimeMod) {
      const { primeJar } = await installInstanceMods(instanceId, config)
      if (!primeJar) {
        throw new Error(`Prime Client mod not found. ${primeClientBuildHint()}`)
      }
      primeModInstalled = true
    }

    const runtimeRoot = getRuntimeRoot()
    const gameDirectory = getInstanceGameDir(instanceId)
    await mkdir(gameDirectory, { recursive: true })

    const javaPath = config.javaPath?.trim() || (await resolveJavaPath())

    emitLaunchProgress({
      phase: 'launch',
      detail: 'Starting Minecraft…',
      percent: 85
    })

    const client = new Client()
    attachClientEvents(client)

    const process = await client.launch({
      authorization,
      root: runtimeRoot,
      javaPath,
      version: {
        number: versionId,
        type: 'release'
      },
      memory: {
        max: `${config.ramMb}M`,
        min: '512M'
      },
      customArgs: config.jvmArgs,
      overrides: {
        gameDirectory,
        detached: true
      }
    })

    if (!process) {
      throw new Error('Minecraft process did not start.')
    }

    activeProcess = process
    process.on('exit', () => {
      if (activeProcess === process) {
        activeProcess = null
      }
    })

    await instanceService.touchLastPlayed(instanceId)

    emitLaunchProgress({
      phase: 'running',
      detail: `Minecraft running as ${account.username}`,
      percent: 100
    })

    return { username: account.username, primeModInstalled }
  }
}

export const minecraftEngine = new MinecraftEngine()
