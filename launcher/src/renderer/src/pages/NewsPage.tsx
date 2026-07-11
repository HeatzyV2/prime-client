import { useEffect, useState } from 'react'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Card } from '@renderer/design-system/components'
import type { NewsItem } from '@shared/types'

export function NewsPage() {
  const [news, setNews] = useState<NewsItem[]>([])

  useEffect(() => {
    void window.primeLauncher.news.list().then(setNews)
  }, [])

  return (
    <PageShell title="News" subtitle="Bundled announcements — no remote news server.">
      <div className="page-list">
        {news.map((item) => (
          <Card key={item.id} hover>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
              <Badge variant={item.tag === 'update' ? 'red' : item.tag === 'event' ? 'success' : 'default'}>
                {item.tag}
              </Badge>
              <span className="text-mono" style={{ color: 'var(--prime-muted-2)' }}>
                {item.date}
              </span>
            </div>
            <h3 className="text-subtitle" style={{ marginBottom: 8 }}>
              {item.title}
            </h3>
            <p className="text-body" style={{ color: 'var(--prime-muted)' }}>
              {item.summary}
            </p>
          </Card>
        ))}
      </div>
    </PageShell>
  )
}
