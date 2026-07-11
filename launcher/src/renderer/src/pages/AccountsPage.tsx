import { useState } from 'react'
import '@renderer/components/LoginModal.css'
import { Link } from 'react-router-dom'
import { Cloud, KeyRound, RefreshCw, Trash2, UserPlus } from 'lucide-react'
import { useI18n } from '@renderer/context/I18nProvider'
import { formatTier } from '@shared/format'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Avatar, Badge, Button, Card } from '@renderer/design-system/components'
import { useAccounts } from '@renderer/context/AccountProvider'
import { LoginModal } from '@renderer/components/LoginModal'
import { AnimatePresence } from 'framer-motion'

export function AccountsPage() {
  const { t, locale } = useI18n()
  const {
    prime,
    accounts,
    activeAccount,
    loginMicrosoft,
    addOffline,
    removeAccount,
    setActive,
    syncPrime,
    refresh
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

  async function handleRefreshMicrosoft(accountId: string) {
    setBusy(accountId)
    const result = await window.primeLauncher.account.refreshMicrosoft(accountId)
    setMessage(result.ok ? t('accounts.refreshSuccess') : result.error ?? t('accounts.refreshFailed'))
    if (result.ok) {
      await refresh()
    }
    setBusy(null)
  }

  async function handleRemove(id: string) {
    setBusy(id)
    await removeAccount(id)
    setBusy(null)
  }

  return (
    <PageShell
      title={t('pages.accounts.title')}
      subtitle={t('pages.accounts.subtitle')}
      actions={
        <Button variant="primary" icon={<UserPlus size={16} />} onClick={() => setShowLogin(true)}>
          {t('accounts.addAccount')}
        </Button>
      }
    >
      <div className="page-grid page-grid--2">
        <Card title={t('accounts.primeAccount')} glow>
          {prime && (
            <div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
              <Avatar
                alt={activeAccount?.username ?? prime.username}
                uuid={activeAccount?.uuid}
                size="lg"
                glow
              />
              <div>
                <div className="text-subtitle">{prime.username}</div>
                <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
                  <Badge variant="prime">{formatTier(prime.tier, locale)}</Badge>
                  <Badge variant="default">{t('accounts.level', { level: prime.level })}</Badge>
                </div>
                <p className="text-caption" style={{ marginTop: 12 }}>
                  {t('accounts.syncDescription')}
                </p>
                <Button
                  variant="secondary"
                  size="sm"
                  icon={<Cloud size={14} />}
                  style={{ marginTop: 12 }}
                  disabled={busy === 'sync'}
                  onClick={() => void handleSync()}
                >
                  {t('accounts.syncButton')}
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

        <Card title={t('accounts.quickAdd')}>
          <p className="text-caption" style={{ marginBottom: 16 }}>
            {t('accounts.quickAddHint')}
          </p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <Button
              variant="primary"
              icon={<KeyRound size={16} />}
              disabled={!!busy}
              onClick={() => void loginMicrosoft().then((r) => r.error && setMessage(r.error))}
            >
              {t('accounts.signInMicrosoft')}
            </Button>
            <input
              className="modal__field"
              placeholder={t('accounts.offlinePlaceholder')}
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
              {t('accounts.addOffline')}
            </Button>
          </div>
        </Card>
      </div>

      <Card title={t('accounts.minecraftAccounts')}>
        {accounts.length === 0 ? (
          <p className="text-body" style={{ color: 'var(--prime-muted)' }}>
            {t('accounts.noAccounts')}{' '}
            <Link to="#" onClick={() => setShowLogin(true)} style={{ color: 'var(--prime-red-bright)' }}>
              {t('accounts.addOne')}
            </Link>{' '}
            {t('accounts.toPlay')}
          </p>
        ) : (
          <div className="page-list">
            {accounts.map((acc) => (
              <div key={acc.id} className="list-row">
                <Avatar alt={acc.username} uuid={acc.uuid} size="sm" glow={acc.id === activeAccount?.id} />
                <div className="list-row__body">
                  <div className="list-row__title">{acc.username}</div>
                  <div className="list-row__desc">
                    {acc.type === 'microsoft' ? t('accounts.microsoft') : t('accounts.offline')} · {acc.uuid}
                  </div>
                </div>
                <div className="list-row__meta">
                  {acc.id === activeAccount?.id ? (
                    <Badge variant="prime">{t('common.active')}</Badge>
                  ) : (
                    <Button variant="ghost" size="sm" onClick={() => void setActive(acc.id)}>
                      {t('common.use')}
                    </Button>
                  )}
                  {acc.type === 'microsoft' && (
                    <Button
                      variant="ghost"
                      size="sm"
                      disabled={busy === acc.id}
                      onClick={() => void handleRefreshMicrosoft(acc.id)}
                      title={t('accounts.refreshMicrosoft')}
                    >
                      <RefreshCw size={14} />
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
