import { useCallback, useEffect, useState } from 'react'
import { Plus, RefreshCw, Trash2 } from 'lucide-react'
import type { FavoriteServer } from '@shared/types'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Button } from '@renderer/design-system/components'
import { useAccounts } from '@renderer/context/AccountProvider'
import { useI18n } from '@renderer/context/I18nProvider'
import { LoginModal } from '@renderer/components/LoginModal'
import { PlayButton } from '@renderer/components/launcher/PlayButton'
import './ServersPage.css'

function partnerFirst(list: FavoriteServer[]): FavoriteServer[] {
  return [...list].sort((a, b) => Number(Boolean(b.partner)) - Number(Boolean(a.partner)))
}

export function ServersPage() {
  const { t } = useI18n()
  const { activeAccount, launch, profile } = useAccounts()
  const [servers, setServers] = useState<FavoriteServer[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [name, setName] = useState('')
  const [address, setAddress] = useState('')
  const [adding, setAdding] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [showLogin, setShowLogin] = useState(false)

  const refresh = useCallback(async () => {
    setBusy(true)
    try {
      const list = partnerFirst(await window.primeLauncher.servers.refreshAll())
      setServers(list)
      setSelectedId((prev) => prev ?? list[0]?.id ?? null)
    } finally {
      setBusy(false)
    }
  }, [])

  useEffect(() => {
    void (async () => {
      const list = partnerFirst(await window.primeLauncher.servers.list())
      setServers(list)
      setSelectedId(list[0]?.id ?? null)
      setServers(partnerFirst(await window.primeLauncher.servers.refreshAll()))
    })()
  }, [])

  const selected = servers.find((s) => s.id === selectedId) ?? servers[0] ?? null

  async function handleAdd() {
    setBusy(true)
    setMessage(null)
    const result = await window.primeLauncher.servers.add(name, address)
    setBusy(false)
    if (result.ok) {
      setName('')
      setAddress('')
      setAdding(false)
      setMessage(t('servers.added'))
      await refresh()
    } else {
      setMessage(result.error ?? t('servers.addFailed'))
    }
  }

  async function handleRemove(serverId: string) {
    const result = await window.primeLauncher.servers.remove(serverId)
    if (!result.ok) {
      setMessage(result.error ?? t('servers.removeFailed'))
      return
    }
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
    await window.primeLauncher.settings.update({ lastServerAddress: server.address })
    await window.primeLauncher.profile.setInstance(instanceId)
    await launch(instanceId, server.address)
    setBusy(false)
  }

  return (
    <PageShell
      title={t('pages.servers.title')}
      subtitle={t('pages.servers.subtitle')}
      actions={
        <div className="servers-v3__actions">
          <Button
            variant="ghost"
            size="sm"
            icon={<RefreshCw size={14} strokeWidth={1.75} />}
            disabled={busy}
            onClick={() => void refresh()}
          >
            {t('servers.refresh')}
          </Button>
          <Button
            variant="secondary"
            size="sm"
            icon={<Plus size={14} strokeWidth={1.75} />}
            onClick={() => setAdding((v) => !v)}
          >
            {t('servers.addServer')}
          </Button>
        </div>
      }
    >
      {adding && (
        <div className="servers-v3__add">
          <input
            className="servers-v3__input"
            placeholder={t('servers.serverName')}
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          <input
            className="servers-v3__input"
            placeholder={t('servers.serverAddress')}
            value={address}
            onChange={(e) => setAddress(e.target.value)}
          />
          <Button
            variant="primary"
            size="sm"
            disabled={busy || name.trim().length < 1 || address.trim().length < 3}
            onClick={() => void handleAdd()}
          >
            {t('common.add')}
          </Button>
        </div>
      )}

      {message && <p className="servers-v3__message">{message}</p>}

      {servers.length === 0 ? (
        <div className="servers-v3__empty">
          <h2>No servers yet</h2>
          <p>Add a server to get started.</p>
          <Button variant="primary" onClick={() => setAdding(true)}>
            {t('servers.addServer')}
          </Button>
        </div>
      ) : (
        <div className="servers-v3">
          <div className="servers-v3__list" role="listbox" aria-label="Servers">
            {servers.map((server) => {
              const active = server.id === selected?.id
              return (
                <button
                  key={server.id}
                  type="button"
                  role="option"
                  aria-selected={active}
                  className={`servers-v3__row${active ? ' servers-v3__row--active' : ''}`}
                  onClick={() => setSelectedId(server.id)}
                >
                  <span
                    className={`servers-v3__status${server.online ? ' servers-v3__status--on' : ''}`}
                    aria-hidden
                  />
                  <span className="servers-v3__name">{server.name}</span>
                  <span className="servers-v3__meta">
                    {server.ping != null ? `${server.ping} ms` : server.online ? 'Online' : 'Offline'}
                  </span>
                </button>
              )
            })}
          </div>

          {selected && (
            <div className="servers-v3__detail v3-fade-in">
              <p className="text-label">{selected.partner ? t('servers.partner') : 'Server'}</p>
              <h2 className="servers-v3__detail-title">{selected.name}</h2>
              <p className="servers-v3__detail-address">{selected.address}</p>
              <p className="servers-v3__detail-desc">
                {selected.description || selected.motd || t('servers.noDescription')}
              </p>

              <div className="servers-v3__stats">
                <div>
                  <span className="text-label">Players</span>
                  <strong>
                    {selected.players ?? '—'}
                    {selected.maxPlayers != null ? ` / ${selected.maxPlayers}` : ''}
                  </strong>
                </div>
                <div>
                  <span className="text-label">Ping</span>
                  <strong>{selected.ping != null ? `${selected.ping} ms` : '—'}</strong>
                </div>
                <div>
                  <span className="text-label">Version</span>
                  <strong>{selected.version || '—'}</strong>
                </div>
              </div>

              <div className="servers-v3__detail-actions">
                <PlayButton
                  state={busy ? 'launching' : 'idle'}
                  label={t('common.play')}
                  onClick={() => void handleJoin(selected)}
                />
                {!selected.partner && (
                  <Button
                    variant="ghost"
                    size="sm"
                    icon={<Trash2 size={14} strokeWidth={1.75} />}
                    onClick={() => void handleRemove(selected.id)}
                  >
                    Remove
                  </Button>
                )}
              </div>
            </div>
          )}
        </div>
      )}

      {showLogin && <LoginModal onClose={() => setShowLogin(false)} />}
    </PageShell>
  )
}
