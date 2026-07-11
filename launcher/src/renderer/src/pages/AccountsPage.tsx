import { useState } from 'react'
import '@renderer/components/LoginModal.css'
import { Link } from 'react-router-dom'
import { Cloud, KeyRound, Trash2, UserPlus } from 'lucide-react'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Avatar, Badge, Button, Card } from '@renderer/design-system/components'
import { useAccounts } from '@renderer/context/AccountProvider'
import { LoginModal } from '@renderer/components/LoginModal'
import { AnimatePresence } from 'framer-motion'

export function AccountsPage() {
  const {
    prime,
    accounts,
    activeAccount,
    loginMicrosoft,
    addOffline,
    removeAccount,
    setActive,
    syncPrime
  } = useAccounts()
  const [showLogin, setShowLogin] = useState(false)
  const [busy, setBusy] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [offlineName, setOfflineName] = useState('')

  async function handleSync() {
    setBusy('sync')
    const result = await syncPrime()
    setMessage(result.message)
    setBusy(null)
  }

  async function handleRemove(id: string) {
    setBusy(id)
    await removeAccount(id)
    setBusy(null)
  }

  return (
    <PageShell
      title="Accounts"
      subtitle="Microsoft authentication, offline profiles, and Prime Account sync."
      actions={
        <Button variant="primary" icon={<UserPlus size={16} />} onClick={() => setShowLogin(true)}>
          Add Account
        </Button>
      }
    >
      <div className="page-grid page-grid--2">
        <Card title="Prime Account" glow>
          {prime && (
            <div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
              <Avatar
                alt={activeAccount?.username ?? prime.username}
                size="lg"
                glow
                src={activeAccount?.skinUrl}
              />
              <div>
                <div className="text-subtitle">{prime.username}</div>
                <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
                  <Badge variant="prime">{prime.tier.toUpperCase()}</Badge>
                  <Badge variant="default">Level {prime.level}</Badge>
                </div>
                <p className="text-caption" style={{ marginTop: 12 }}>
                  Sync configs, HUD, cosmetics, and stats across devices.
                </p>
                <Button
                  variant="secondary"
                  size="sm"
                  icon={<Cloud size={14} />}
                  style={{ marginTop: 12 }}
                  disabled={busy === 'sync'}
                  onClick={() => void handleSync()}
                >
                  Sync Prime Profile
                </Button>
                {message && (
                  <p className="text-caption" style={{ marginTop: 8, color: 'var(--prime-success)' }}>
                    {message}
                  </p>
                )}
              </div>
            </div>
          )}
        </Card>

        <Card title="Quick Add">
          <p className="text-caption" style={{ marginBottom: 16 }}>
            Microsoft opens a secure login window (Xbox Live → Minecraft Services).
          </p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <Button
              variant="primary"
              icon={<KeyRound size={16} />}
              disabled={!!busy}
              onClick={() => void loginMicrosoft().then((r) => r.error && setMessage(r.error))}
            >
              Sign in with Microsoft
            </Button>
            <input
              className="modal__field"
              placeholder="Offline username"
              value={offlineName}
              maxLength={16}
              onChange={(e) => setOfflineName(e.target.value)}
            />
            <Button
              variant="secondary"
              disabled={offlineName.trim().length < 3 || !!busy}
              onClick={() =>
                void addOffline(offlineName).then((r) => {
                  if (r.ok) setOfflineName('')
                  else setMessage(r.error ?? null)
                })
              }
            >
              Add Offline Account
            </Button>
          </div>
        </Card>
      </div>

      <Card title="Minecraft Accounts">
        {accounts.length === 0 ? (
          <p className="text-body" style={{ color: 'var(--prime-muted)' }}>
            No accounts yet.{' '}
            <Link to="#" onClick={() => setShowLogin(true)} style={{ color: 'var(--prime-red-bright)' }}>
              Add one
            </Link>{' '}
            to play.
          </p>
        ) : (
          <div className="page-list">
            {accounts.map((acc) => (
              <div key={acc.id} className="list-row">
                <Avatar alt={acc.username} size="sm" src={acc.skinUrl} glow={acc.id === activeAccount?.id} />
                <div className="list-row__body">
                  <div className="list-row__title">{acc.username}</div>
                  <div className="list-row__desc">
                    {acc.type === 'microsoft' ? 'Microsoft' : 'Offline'} · {acc.uuid}
                  </div>
                </div>
                <div className="list-row__meta">
                  {acc.id === activeAccount?.id ? (
                    <Badge variant="prime">Active</Badge>
                  ) : (
                    <Button variant="ghost" size="sm" onClick={() => void setActive(acc.id)}>
                      Use
                    </Button>
                  )}
                  <Button
                    variant="ghost"
                    size="sm"
                    disabled={busy === acc.id}
                    onClick={() => void handleRemove(acc.id)}
                  >
                    <Trash2 size={14} />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>

      <AnimatePresence>{showLogin && <LoginModal onClose={() => setShowLogin(false)} />}</AnimatePresence>
    </PageShell>
  )
}
