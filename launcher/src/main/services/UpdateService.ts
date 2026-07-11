import { app, shell } from 'electron'
import { GITHUB_REPO_SLUG, GITHUB_RELEASES_URL } from '../../shared/github'
import { settingsStore } from '../storage/SettingsStore'

export interface UpdateCheckResult {
  current: string
  latest: string
  updateAvailable: boolean
  notes: string
  checkedAt: string
  releaseUrl?: string
  downloadUrl?: string
}

interface GitHubRelease {
  tag_name: string
  html_url: string
  body?: string
  assets?: Array<{ name: string; browser_download_url: string }>
}

function compareSemver(a: string, b: string): number {
  const clean = (v: string) => v.replace(/^v/i, '')
  const pa = clean(a).split('.').map(Number)
  const pb = clean(b).split('.').map(Number)
  for (let i = 0; i < 3; i++) {
    const diff = (pa[i] ?? 0) - (pb[i] ?? 0)
    if (diff !== 0) {
      return diff
    }
  }
  return 0
}

function pickWindowsAsset(release: GitHubRelease): string | undefined {
  const assets = release.assets ?? []
  const exe =
    assets.find((a) => /setup.*\.exe$/i.test(a.name)) ??
    assets.find((a) => /\.exe$/i.test(a.name))
  return exe?.browser_download_url
}

/** Checks GitHub Releases API for a newer launcher build. */
export class UpdateService {
  async check(): Promise<UpdateCheckResult> {
    const current = app.getVersion()
    const checkedAt = new Date().toISOString()

    try {
      const response = await fetch(`https://api.github.com/repos/${GITHUB_REPO_SLUG}/releases/latest`, {
        headers: { Accept: 'application/vnd.github+json', 'User-Agent': 'Prime-Launcher' }
      })

      if (!response.ok) {
        await settingsStore.mutate((s) => {
          s.lastUpdateCheck = checkedAt
        })
        return {
          current,
          latest: current,
          updateAvailable: false,
          notes: response.status === 404
            ? 'No GitHub release yet. Check back after the first tag is published.'
            : `Could not reach GitHub (${response.status}).`,
          checkedAt,
          releaseUrl: GITHUB_RELEASES_URL
        }
      }

      const release = (await response.json()) as GitHubRelease
      const latest = release.tag_name.replace(/^v/i, '')
      const updateAvailable = compareSemver(current, latest) < 0
      const downloadUrl = pickWindowsAsset(release)

      await settingsStore.mutate((s) => {
        s.lastUpdateCheck = checkedAt
      })

      return {
        current,
        latest,
        updateAvailable,
        notes: updateAvailable
          ? (release.body?.split('\n')[0]?.trim() || `Version ${latest} is available on GitHub.`)
          : 'You are on the latest GitHub release.',
        checkedAt,
        releaseUrl: release.html_url,
        downloadUrl
      }
    } catch {
      await settingsStore.mutate((s) => {
        s.lastUpdateCheck = checkedAt
      })
      return {
        current,
        latest: current,
        updateAvailable: false,
        notes: 'Offline — could not check GitHub Releases.',
        checkedAt,
        releaseUrl: GITHUB_RELEASES_URL
      }
    }
  }

  async openReleasePage(url?: string): Promise<void> {
    await shell.openExternal(url ?? GITHUB_RELEASES_URL)
  }
}

export const updateService = new UpdateService()
