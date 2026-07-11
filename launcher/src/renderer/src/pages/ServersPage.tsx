import { useCallback, useEffect, useState } from 'react'
import { Globe, Plus, RefreshCw, Trash2 } from 'lucide-react'
import type { FavoriteServer } from '@shared/types'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Button } from '@renderer/design-system/components'
import { useAccounts } from '@renderer/context/AccountProvider'
import { useI18n } from '@renderer/context/I18nProvider'
import { LoginModal } from '@renderer/components/LoginModal'
import { AnimatePresence } from 'framer-motion'

function pingColor(ping: number | undefined): string {
  if (ping === undefined || ping === 0) return 'var(--prime-muted)'
  if (ping < 50) return 'var(--prime-success)'
  if (ping < 100) return 'var(--prime-warning)'
  return 'var(--prime-error)'
}

export function ServersPage() {
  const { t } = useI18n()
  const { activeAccount, launch, profile } = useAccounts()
  const [servers, setServers] = useState<FavoriteServer[]>([])
  const [name, setName] = useState('')
  const [address, setAddress] = useState('')
  const [message, setMessage] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [showLogin, setShowLogin] = useState(false)

  const refresh = useCallback(async () => {
    setServers(await window.primeLauncher.servers.refreshAll())
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  async function handleAdd() {
    setBusy(true)
    setMessage(null)
    const result = await window.primeLauncher.servers.add(name, address)
    setBusy(false)
    if (result.ok) {
      setName('')
      setAddress('')
      setMessage(t('servers.added'))
      await refresh()
    } else {
      setMessage(result.error ?? t('servers.addFailed'))
    }
  }

  async function handleRemove(serverId: string) {
    await window.primeLauncher.servers.remove(serverId)
    await refresh()
  }

  async function handleJoin(server: FavoriteServer) {
    if (!activeAccount) {
      setShowLogin(true)
      return
    }
    const instanceId = profile?.instanceId
    if (!instanceId) {
      setMessage(t('servers.joinNeedsAccount'))
      return
    }
    setBusy(true)
    await window.primeLauncher.profile.setInstance(instanceId)
    await launch(instanceId, server.address)
    setBusy(false)
  }

  return (
    <PageShell
      title={t('pages.servers.title')}
      subtitle={t('pages.servers.subtitle')}
      actions={
        <Button variant="secondary" size="sm" icon={<RefreshCw size={14} />} disabled={busy} onClick={() => void refresh()}>
          {t('servers.refresh')}
        </Button>
      }
    >
      <div className="page-grid page-grid--2" style={{ marginBottom: 16 }}>
        <input className="modal__field" placeholder={t('servers.serverName')} value={name} onChange={(e) => setName(e.target.value)} />
        <input className="modal__field" placeholder={t('servers.serverAddress')} value={address} onChange={(e) => setAddress(e.target.value)} />
      </div>
      <Button
        variant="primary"
        icon={<Plus size={16} />}
        disabled={busy || name.trim().length < 1 || address.trim().length < 3}
        style={{ marginBottom: 16 }}
        onClick={() => void handleAdd()}
      >
        {t('servers.addServer')}
      </Button>

      {message && (
        <p className="text-caption" style={{ marginBottom: 12, color: 'var(--prime-muted)' }}>
          {message}
        </p>
      )}

      <div className="page-list">
        {servers.length === 0 ? (
          <p className="text-caption">{t('servers.empty')}</p>
        ) : (
          servers.map((server) => (
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
                  {server.players ?? 0}/{server.maxPlayers ?? '?'}
                </span>
                <span className="text-mono" style={{ color: pingColor(server.ping), fontWeight: 600 }}>
                  {server.ping ? `${server.ping}ms` : '—'}
                </span>
                <Button variant="primary" size="sm" disabled={busy} onClick={() => void handleJoin(server)}>
                  {t('common.join')}
                </Button>
                <Button variant="ghost" size="sm" icon={<Trash2 size={14} />} onClick={() => void handleRemove(server.id)} />
              </div>
            </div>
          ))
        )}
      </div>

      <AnimatePresence>{showLogin && <LoginModal onClose={() => setShowLogin(false)} />}</AnimatePresence>
    </PageShell>
  )
}
