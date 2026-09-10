import { mkdir, readFile, rename, writeFile } from 'fs/promises'
import { dirname } from 'path'

/** Atomic JSON write: temp sibling + rename (matches core ConfigManager). */
export async function atomicWriteJson(filePath: string, data: unknown): Promise<void> {
  await mkdir(dirname(filePath), { recursive: true })
  const tempPath = `${filePath}.tmp`
  await writeFile(tempPath, JSON.stringify(data, null, 2), 'utf8')
  await rename(tempPath, filePath)
}

/** Read JSON; on parse failure optionally quarantine and return null. */
export async function readJsonFile<T>(filePath: string): Promise<T | null> {
  try {
    const raw = await readFile(filePath, 'utf8')
    return JSON.parse(raw) as T
  } catch {
    return null
  }
}

export async function quarantineCorrupt(filePath: string): Promise<void> {
  try {
    await rename(filePath, `${filePath}.corrupt-${Date.now()}`)
  } catch {
    // ignore
  }
}
