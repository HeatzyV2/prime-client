import { mkdir, readFile, writeFile } from 'fs/promises'
import { dirname } from 'path'
import { getOptionsPath } from './paths'
import type { GameDisplayMode } from '../storage/SettingsStore'
import {
  getOptionValue,
  mergePresetGameOptions,
  presetGameOptions,
  setOptionValue,
  setOptionValueIfAbsent,
  type PresetGameOptions
} from './optionsMerge'

export {
  getOptionValue,
  mergePresetGameOptions,
  presetGameOptions,
  setOptionValue,
  setOptionValueIfAbsent
}
export type { PresetGameOptions }

export async function readOptionsLines(instanceId: string): Promise<string[]> {
  try {
    const raw = await readFile(getOptionsPath(instanceId), 'utf8')
    return raw.split(/\r?\n/)
  } catch (err) {
    // Missing file is fine (new instance). Any other error must not look like "empty"
    // or callers that rewrite options.txt would wipe FPS / keybinds.
    if (isNotFound(err)) {
      return []
    }
    throw err
  }
}

export async function writeOptionsLines(instanceId: string, lines: string[]): Promise<void> {
  const path = getOptionsPath(instanceId)
  await mkdir(dirname(path), { recursive: true })
  await writeFile(path, lines.join('\n'), 'utf8')
}

export function parseResourcePackList(raw: string | undefined): string[] {
  if (!raw) {
    return ['vanilla']
  }
  try {
    const parsed = JSON.parse(raw) as string[]
    return Array.isArray(parsed) ? parsed : ['vanilla']
  } catch {
    return ['vanilla']
  }
}

export async function getActiveResourcePackFile(instanceId: string): Promise<string | null> {
  const lines = await readOptionsLines(instanceId)
  const packs = parseResourcePackList(getOptionValue(lines, 'resourcePacks'))
  const last = packs.at(-1)
  if (!last || last === 'vanilla') {
    return null
  }
  if (last.startsWith('file/')) {
    return last.slice('file/'.length)
  }
  return last
}

export async function setActiveResourcePack(instanceId: string, fileName: string | null): Promise<void> {
  let lines = await readOptionsLines(instanceId)
  const current = parseResourcePackList(getOptionValue(lines, 'resourcePacks'))
  const withoutFiles = current.filter((p) => p === 'vanilla' || !p.startsWith('file/'))

  if (fileName) {
    withoutFiles.push(`file/${fileName}`)
  }

  lines = setOptionValue(lines, 'resourcePacks', JSON.stringify(withoutFiles))
  await writeOptionsLines(instanceId, lines)
}

export async function getActiveShaderPack(instanceId: string): Promise<string | null> {
  const lines = await readOptionsLines(instanceId)
  const value = getOptionValue(lines, 'shaderPack')
  if (!value || value === 'OFF' || value === '""') {
    return null
  }
  return value.replace(/^"(.*)"$/, '$1')
}

export async function setActiveShaderPack(instanceId: string, fileName: string | null): Promise<void> {
  let lines = await readOptionsLines(instanceId)
  const value = fileName ? `"${fileName}"` : 'OFF'
  lines = setOptionValue(lines, 'shaderPack', value)
  await writeOptionsLines(instanceId, lines)
}

/** Maps launcher locale to Minecraft options.txt language code. */
export function launcherLocaleToMinecraftLang(locale: 'en' | 'fr'): string {
  return locale === 'fr' ? 'fr_fr' : 'en_us'
}

/** Writes the Minecraft language key into options.txt for an instance. */
export async function syncMinecraftLanguage(instanceId: string, locale: 'en' | 'fr'): Promise<void> {
  let lines = await readOptionsLines(instanceId)
  lines = setOptionValue(lines, 'lang', launcherLocaleToMinecraftLang(locale))
  await writeOptionsLines(instanceId, lines)
}

/** Syncs resolution and display mode into options.txt before launch. */
export async function syncMinecraftDisplaySettings(
  instanceId: string,
  width: number,
  height: number,
  mode: GameDisplayMode
): Promise<void> {
  let lines = await readOptionsLines(instanceId)
  if (width > 0) {
    lines = setOptionValue(lines, 'overrideWidth', String(width))
  }
  if (height > 0) {
    lines = setOptionValue(lines, 'overrideHeight', String(height))
  }
  lines = setOptionValue(lines, 'fullscreen', mode === 'fullscreen' ? 'true' : 'false')
  await writeOptionsLines(instanceId, lines)
}

function isNotFound(err: unknown): boolean {
  return typeof err === 'object' && err !== null && 'code' in err && (err as { code: string }).code === 'ENOENT'
}
