import { BUNDLED_NEWS } from '../../shared/ecosystem-catalog'
import { GITHUB_REPO_SLUG } from '../../shared/github'
import type { NewsItem } from '../../shared/types'

interface GitHubRelease {
  tag_name: string
  name: string
  body: string
  published_at: string
  prerelease: boolean
}

function summarize(body: string): string {
  const line = body.split('\n').find((l) => l.trim().length > 0)
  return (line ?? 'See release notes on GitHub.').slice(0, 180)
}

/** Bundled news + latest GitHub Release when available. */
export class NewsService {
  async getNews(): Promise<NewsItem[]> {
    const items = [...BUNDLED_NEWS]

    try {
      const response = await fetch(`https://api.github.com/repos/${GITHUB_REPO_SLUG}/releases?per_page=3`, {
        headers: { Accept: 'application/vnd.github+json', 'User-Agent': 'Prime-Launcher' }
      })
      if (response.ok) {
        const releases = (await response.json()) as GitHubRelease[]
        for (const release of releases.filter((r) => !r.prerelease).slice(0, 2)) {
          const id = `gh-${release.tag_name}`
          if (items.some((n) => n.id === id)) {
            continue
          }
          items.unshift({
            id,
            title: release.name || release.tag_name,
            summary: summarize(release.body ?? ''),
            date: release.published_at.slice(0, 10),
            tag: 'update'
          })
        }
      }
    } catch {
      // offline — bundled news only
    }

    return items.sort((a, b) => b.date.localeCompare(a.date))
  }
}

export const newsService = new NewsService()
