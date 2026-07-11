import { execFile } from 'child_process'
import { existsSync } from 'fs'
import { readdir } from 'fs/promises'
import { join } from 'path'
import { promisify } from 'util'

const execFileAsync = promisify(execFile)

const MIN_JAVA_MAJOR = 21

function parseJavaMajor(versionOutput: string): number | null {
  const quoted = versionOutput.match(/version "(\d+)/)
  if (quoted) {
    return Number.parseInt(quoted[1]!, 10)
  }
  const legacy = versionOutput.match(/version "1\.(\d+)/)
  if (legacy) {
    return Number.parseInt(legacy[1]!, 10)
  }
  return null
}

async function probeJava(javaPath: string): Promise<string | null> {
  try {
    const { stdout } = await execFileAsync(javaPath, ['-version'], { timeout: 8000 })
    const output = `${stdout}`
    const major = parseJavaMajor(output)
    if (major !== null && major >= MIN_JAVA_MAJOR) {
      return javaPath
    }
  } catch (err: unknown) {
    const execErr = err as { stderr?: string; stdout?: string }
    const output = `${execErr.stderr ?? ''}${execErr.stdout ?? ''}`
    const major = parseJavaMajor(output)
    if (major !== null && major >= MIN_JAVA_MAJOR) {
      return javaPath
    }
  }
  return null
}

async function discoverJavaOnWindows(): Promise<string[]> {
  const roots = [
    process.env['JAVA_HOME'],
    process.env['JDK_HOME'],
    'C:\\Program Files\\Java',
    'C:\\Program Files\\Eclipse Adoptium',
    'C:\\Program Files\\Microsoft',
    'C:\\Program Files\\Zulu'
  ].filter(Boolean) as string[]

  const candidates: string[] = ['java']

  for (const root of roots) {
    const bin = join(root, 'bin', 'java.exe')
    if (existsSync(bin)) {
      candidates.push(bin)
    }
    if (existsSync(root)) {
      try {
        const entries = await readdir(root, { withFileTypes: true })
        for (const entry of entries) {
          if (!entry.isDirectory()) {
            continue
          }
          const nested = join(root, entry.name, 'bin', 'java.exe')
          if (existsSync(nested)) {
            candidates.push(nested)
          }
        }
      } catch {
        // ignore unreadable directories
      }
    }
  }

  return [...new Set(candidates)]
}

async function discoverJavaCandidates(): Promise<string[]> {
  if (process.platform === 'win32') {
    return discoverJavaOnWindows()
  }
  const roots = [process.env['JAVA_HOME'], process.env['JDK_HOME']].filter(Boolean) as string[]
  const candidates = ['java']
  for (const root of roots) {
    const bin = join(root, 'bin', 'java')
    if (existsSync(bin)) {
      candidates.push(bin)
    }
  }
  return [...new Set(candidates)]
}

/** Finds a Java 21+ binary on the system. No bundled JRE yet. */
export async function resolveJavaPath(): Promise<string> {
  for (const candidate of await discoverJavaCandidates()) {
    const resolved = await probeJava(candidate)
    if (resolved) {
      return resolved
    }
  }
  throw new Error(
    `Java ${MIN_JAVA_MAJOR}+ is required. Install a JDK and ensure java is on PATH, or set JAVA_HOME.`
  )
}
