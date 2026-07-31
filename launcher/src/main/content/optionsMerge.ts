import type { PerformancePreset } from '../../shared/content-types'

export function getOptionValue(lines: string[], key: string): string | undefined {
  const prefix = `${key}:`
  const line = lines.find((l) => l.startsWith(prefix))
  return line ? line.slice(prefix.length) : undefined
}

export function setOptionValue(lines: string[], key: string, value: string): string[] {
  const prefix = `${key}:`
  let found = false
  const next = lines.map((line) => {
    if (line.startsWith(prefix)) {
      found = true
      return `${prefix}${value}`
    }
    return line
  })
  if (!found) {
    next.push(`${prefix}${value}`)
  }
  return next
}

/** Sets a key only when it is missing — never clobbers an existing user value. */
export function setOptionValueIfAbsent(lines: string[], key: string, value: string): string[] {
  if (getOptionValue(lines, key) !== undefined) {
    return lines
  }
  return setOptionValue(lines, key, value)
}

export type PresetGameOptions = {
  renderDistance: string
  simulationDistance: string
  maxFps: string
  graphicsMode: string
}

/** Vanilla options.txt keys written by a performance preset. */
export function presetGameOptions(
  presetId: PerformancePreset,
  renderDistance: number
): PresetGameOptions {
  return {
    renderDistance: String(renderDistance),
    simulationDistance: String(Math.min(renderDistance, 12)),
    maxFps: presetId === 'ultra' ? '260' : presetId === 'performance' ? '240' : '120',
    graphicsMode: presetId === 'low' ? '0' : presetId === 'ultra' ? '2' : '1'
  }
}

/**
 * Merges performance-preset game options into options.txt lines.
 * - `overwrite`: user explicitly applied a preset (Performance page / settings).
 * - `fill-absent`: first-run / new instance only — keep any value the user (or MC) already set.
 */
export function mergePresetGameOptions(
  lines: string[],
  options: PresetGameOptions,
  mode: 'overwrite' | 'fill-absent'
): string[] {
  const set = mode === 'overwrite' ? setOptionValue : setOptionValueIfAbsent
  let next = lines
  next = set(next, 'renderDistance', options.renderDistance)
  next = set(next, 'simulationDistance', options.simulationDistance)
  next = set(next, 'maxFps', options.maxFps)
  next = set(next, 'graphicsMode', options.graphicsMode)
  return next
}
