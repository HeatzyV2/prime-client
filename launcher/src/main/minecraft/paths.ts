import { app } from 'electron'
import { existsSync } from 'fs'
import { join } from 'path'

/** Shared Minecraft runtime (versions, libraries, assets). */
export function getRuntimeRoot(): string {
  return join(app.getPath('userData'), 'runtime', 'minecraft')
}

/** Per-instance game directory (saves, mods, options). */
export function getInstanceGameDir(instanceId: string): string {
  return join(app.getPath('userData'), 'instances', instanceId, 'game')
}

export function getInstanceModsDir(instanceId: string): string {
  return join(getInstanceGameDir(instanceId), 'mods')
}

/** Monorepo root when developing from the parent project. */
export function getRepoRoot(): string {
  const appPath = app.getAppPath()
  const candidates = [
    join(appPath, '..', '..'),
    join(appPath, '..'),
    join(process.cwd(), '..'),
    process.cwd()
  ]

  for (const root of candidates) {
    if (existsSync(join(root, 'mc-1.21.11', 'build.gradle'))) {
      return root
    }
  }

  return join(appPath, '..', '..')
}
