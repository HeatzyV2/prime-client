import { GITHUB_REPO_SLUG } from './github'

export interface GitHubReleaseAsset {
  name: string
  browser_download_url: string
  size: number
}

export interface GitHubRelease {
  tag_name: string
  html_url: string
  body?: string
  assets?: GitHubReleaseAsset[]
}

const GITHUB_API_HEADERS = {
  Accept: 'application/vnd.github+json',
  'User-Agent': 'Prime-Launcher'
} as const

export function isPrimeModJarAsset(name: string, prefix: string): boolean {
  return (
    name.startsWith(prefix) &&
    name.endsWith('.jar') &&
    !name.includes('-sources') &&
    !name.includes('-dev')
  )
}

export function pickPrimeModAsset(release: GitHubRelease, prefix: string): GitHubReleaseAsset | undefined {
  const assets = release.assets ?? []
  return assets
    .filter((a) => isPrimeModJarAsset(a.name, prefix))
    .sort((a, b) => a.name.localeCompare(b.name))
    .at(-1)
}

export async function fetchLatestGitHubRelease(): Promise<GitHubRelease | null> {
  const response = await fetch(`https://api.github.com/repos/${GITHUB_REPO_SLUG}/releases/latest`, {
    headers: GITHUB_API_HEADERS
  })
  if (!response.ok) {
    return null
  }
  return (await response.json()) as GitHubRelease
}
