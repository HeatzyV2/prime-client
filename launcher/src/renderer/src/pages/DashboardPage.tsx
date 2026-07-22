import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { Play, Zap, UserCog, Box, KeyRound } from 'lucide-react'
import { Avatar, Badge, Button, Card } from '@renderer/design-system/components'
import { useAccounts } from '@renderer/context/AccountProvider'
import { useI18n } from '@renderer/context/I18nProvider'
import { LoginModal } from '@renderer/components/LoginModal'
import { UpdateModal } from '@renderer/components/UpdateModal'
import { CrashReportPanel } from '@renderer/components/CrashReportPanel'
import type { UpdateStatusDto } from '@shared/ipc'
import { formatLoader, formatTier } from '@shared/format'
import type { FavoriteServer, GameInstance, NewsItem } from '@shared/types'
import './DashboardPage.css'

interface DashboardPageProps {
  news: NewsItem[]
  servers: FavoriteServer[]
}

function formatPlayTime(minutes: number): string {
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return h > 0 ? `${h}h ${m}m` : `${m}m`
}

export function DashboardPage({ news, servers }: DashboardPageProps) {
  const { t, locale } = useI18n()
  const { prime, activeAccount, profile, launch, launchMessage, launchProgress, clearLaunchMessage } = useAccounts()
  const [showLogin, setShowLogin] = useState(false)
  const [launching, setLaunching] = useState(false)
  const [instance, setInstance] = useState<GameInstance | null>(null)
  const [crashDismissed, setCrashDismissed] = useState(false)
  const [updateStatus, setUpdateStatus] = useState<UpdateStatusDto | null>(null)
  const [showUpdate, setShowUpdate] = useState(false)

  useEffect(() => {
    void (async () => {
      const status = await window.primeLauncher.update.check()
      setUpdateStatus(status)
      if (status.anyUpdateAvailable) {
        const settings = await window.primeLauncher.settings.get()
        const key = `launcher:${status.launcher.latest}|mod:${status.mod.latest}`
        if (settings.dismissedUpdateBanner !== key) {
          setShowUpdate(true)
        }
      }
    })()
  }, [])

  useEffect(() => {
    if (launchProgress?.phase === 'crashed') {
      setCrashDismissed(false)
    }
  }, [launchProgress?.phase, launchProgress?.crash?.title])

  useEffect(() => {
    void (async () => {
      if (profile?.instanceId) {
        const inst = await window.primeLauncher.instance.get(profile.instanceId)
        if (inst) {
          setInstance(inst)
          return
        }
      }
      const fallback = await window.primeLauncher.instance.getDefault()
      setInstance(fallback)
    })()
  }, [profile?.instanceId])

  const mcUsername = activeAccount?.username ?? t('common.guest')
  const playTimeMinutes = profile?.playTimeMinutes ?? 0
  const lastPlayed = profile?.lastPlayed

  async function handlePlay() {
    if (!activeAccount || !instance) {
      setShowLogin(true)
      return
    }
    setLaunching(true)
    clearLaunchMessage()
    await window.primeLauncher.profile.setInstance(instance.id)
    const settings = await window.primeLauncher.settings.get()
    const lastServer =
      typeof settings.lastServerAddress === 'string' && settings.lastServerAddress.trim()
        ? settings.lastServerAddress.trim()
        : undefined
    await launch(instance.id, lastServer)
    setLaunching(false)
  }

  async function handleJoinServer(address: string) {
    if (!activeAccount || !instance) {
      setShowLogin(true)
      return
    }
    setLaunching(true)
    clearLaunchMessage()
    await window.primeLauncher.profile.setInstance(instance.id)
    await window.primeLauncher.settings.update({ lastServerAddress: address })
    await launch(instance.id, address)
    setLaunching(false)
  }

  if (!instance) {
    return null
  }

  return (
    <motion.div
      className="dashboard"
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
    >
      {!activeAccount && (
        <Card glow>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16 }}>
            <div>
              <div className="text-subtitle">{t('dashboard.signInTitle')}</div>
              <p className="text-caption">{t('dashboard.signInHint')}</p>
            </div>
            <Button variant="primary" icon={<KeyRound size={16} />} onClick={() => setShowLogin(true)}>
              {t('dashboard.addAccount')}
            </Button>
          </div>
        </Card>
      )}

      {(launchMessage || launchProgress) && (
        <Card>
          {launchProgress?.phase === 'crashed' && launchProgress.crash && !crashDismissed && (
            <CrashReportPanel crash={launchProgress.crash} onDismiss={() => setCrashDismissed(true)} />
          )}
          {launchProgress && launchProgress.phase !== 'log' && launchProgress.phase !== 'crashed' && (
            <p className="text-caption" style={{ marginBottom: 8 }}>
              {launchProgress.detail}
              {launchProgress.percent !== undefined ? ` · ${launchProgress.percent}%` : ''}
            </p>
          )}
          {launchProgress?.phase === 'stopped' && (
            <p className="text-caption" style={{ color: 'var(--prime-muted)' }}>
              {launchProgress.detail}
            </p>
          )}
          {launchMessage && (
            <p
              className="text-body"
              style={{
                color: launchMessage.toLowerCase().includes('started') || launchMessage.toLowerCase().includes('running')
                  ? 'var(--prime-success)'
                  : 'var(--prime-red-bright)'
              }}
            >
              {launchMessage}
            </p>
          )}
        </Card>
      )}

      <section className="dashboard__hero">
        <div className="dashboard__welcome">
          <span className="text-caption">{t('dashboard.welcomeBack')}</span>
          <h1 className="text-display text-gradient">{mcUsername}</h1>
          <p className="text-body" style={{ color: 'var(--prime-muted)' }}>
            Prime Client {instance.minecraftVersion} · {formatLoader(instance.loader)}
          </p>
        </div>
        <div className="dashboard__play-area">
          <Button
            variant="primary"
            size="xl"
            icon={<Play size={22} fill="currentColor" />}
            disabled={launching}
            onClick={() => void handlePlay()}
          >
            {launching ? t('common.launching') : t('common.play')}
          </Button>
          <span className="dashboard__instance">{instance.name}</span>
          <div className="dashboard__quick-actions">
            <Button variant="secondary" size="sm" icon={<Zap size={14} />} disabled={!activeAccount} onClick={() => void handlePlay()}>
              {t('dashboard.quickLaunch')}
            </Button>
            <Link to="/accounts">
              <Button variant="ghost" size="sm" icon={<UserCog size={14} />}>
                {t('dashboard.profile')}
              </Button>
            </Link>
            <Link to="/instances">
              <Button variant="ghost" size="sm" icon={<Box size={14} />}>
                {t('dashboard.instance')}
              </Button>
            </Link>
          </div>
        </div>
      </section>

      <Card title={t('dashboard.whatsNew.title')}>
        <ul className="dashboard__whats-new">
          <li>{t('dashboard.whatsNew.themes')}</li>
          <li>{t('dashboard.whatsNew.fps')}</li>
          <li>{t('dashboard.whatsNew.chat')}</li>
        </ul>
        <a
          className="dashboard__whats-new-link"
          href="https://github.com/HeatzyV2/prime-client/releases/latest"
          target="_blank"
          rel="noreferrer"
        >
          {t('dashboard.whatsNew.release')}
        </a>
      </Card>

      <div className="dashboard__grid">
        <Card title={t('dashboard.primeAccount')} glow className="dashboard__grid-span-2">
          <div className="dashboard__profile">
            <Avatar alt={mcUsername} uuid={activeAccount?.uuid} size="xl" glow />
            <div className="dashboard__profile-info">
              <h3>{prime?.username ?? t('common.guest')}</h3>
              <Badge variant="prime">
                {formatTier(prime?.tier ?? 'free', locale)} · Lv. {prime?.level ?? 1}
              </Badge>
              <div className="dashboard__stats">
                <div>
                  <div className="dashboard__stat-value">{formatPlayTime(playTimeMinutes)}</div>
                  <div className="dashboard__stat-label">{t('dashboard.timePlayed')}</div>
                </div>
                <div>
                  <div className="dashboard__stat-value">{instance.modCount}</div>
                  <div className="dashboard__stat-label">{t('dashboard.mods')}</div>
                </div>
                <div>
                  <div className="dashboard__stat-value">{instance.ramMb}MB</div>
                  <div className="dashboard__stat-label">{t('dashboard.ram')}</div>
                </div>
              </div>
            </div>
          </div>
        </Card>

        <Card title={t('dashboard.lastSession')}>
          <p className="text-body" style={{ marginBottom: 'var(--space-2)' }}>
            {lastPlayed
              ? new Date(lastPlayed).toLocaleDateString(locale === 'fr' ? 'fr-FR' : 'en-US', { dateStyle: 'medium' })
              : t('common.never')}
          </p>
          <p className="text-caption">
            {t('dashboard.version')} {instance.minecraftVersion}
          </p>
        </Card>

        <Card title={t('dashboard.news')}>
          {news.map((item) => (
            <div key={item.id} className="dashboard__news-item">
              <div className="dashboard__news-title">
                <Badge variant={item.tag === 'update' ? 'red' : 'default'}>{t(`newsTag.${item.tag}`)}</Badge>{' '}
                {item.title}
              </div>
              <p className="dashboard__news-summary">{item.summary}</p>
            </div>
          ))}
        </Card>

        <Card title={t('dashboard.favoriteServers')} className="dashboard__grid-span-2">
          {servers.map((s) => (
            <div key={s.id} className="dashboard__server">
              <div className="dashboard__server-info">
                <span className="dashboard__server-name">{s.name}</span>
                <span className="dashboard__server-meta">
                  {s.address} · {s.players ?? 0}/{s.maxPlayers ?? '?'} · {s.ping ?? '—'}ms
                </span>
              </div>
              <Button variant="secondary" size="sm" disabled={launching} onClick={() => void handleJoinServer(s.address)}>
                {t('common.join')}
              </Button>
            </div>
          ))}
        </Card>
      </div>

      <AnimatePresence>{showLogin && <LoginModal onClose={() => setShowLogin(false)} />}</AnimatePresence>
      <AnimatePresence>
        {showUpdate && updateStatus && (
          <UpdateModal
            status={updateStatus}
            onClose={() => setShowUpdate(false)}
            onUpdated={() => void window.primeLauncher.update.check(true).then(setUpdateStatus)}
          />
        )}
      </AnimatePresence>
    </motion.div>
  )
}
