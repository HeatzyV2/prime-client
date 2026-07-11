import { Globe, Plus } from 'lucide-react'
import { MOCK_SERVERS } from '@shared/mock-data'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Button } from '@renderer/design-system/components'

function pingColor(ping: number): string {
  if (ping === 0) return 'var(--prime-muted)'
  if (ping < 50) return 'var(--prime-success)'
  if (ping < 100) return 'var(--prime-warning)'
  return 'var(--prime-error)'
}

export function ServersPage() {
  return (
    <PageShell
      title="Server Hub"
      subtitle="Favorite servers with live player count, ping, and quick join."
      actions={
        <Button variant="primary" icon={<Plus size={16} />}>
          Add Server
        </Button>
      }
    >
      <div className="page-list">
        {MOCK_SERVERS.map((server) => (
          <div key={server.id} className="list-row">
            <div className="list-row__icon">
              <Globe size={18} />
            </div>
            <div className="list-row__body">
              <div className="list-row__title">{server.name}</div>
              <div className="list-row__desc">{server.address}</div>
            </div>
            <div className="list-row__meta">
              <span className="text-mono" style={{ color: 'var(--prime-muted)' }}>
                {server.players?.toLocaleString()}/{server.maxPlayers?.toLocaleString()}
              </span>
              <span className="text-mono" style={{ color: pingColor(server.ping ?? 0), fontWeight: 600 }}>
                {server.ping ?? '—'}ms
              </span>
              <Button variant="primary" size="sm">JOIN</Button>
            </div>
          </div>
        ))}
      </div>
    </PageShell>
  )
}
