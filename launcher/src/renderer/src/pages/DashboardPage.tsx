import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { Play, Zap, UserCog, Box, KeyRound } from 'lucide-react'
import { Avatar, Badge, Button, Card } from '@renderer/design-system/components'
import { useAccounts } from '@renderer/context/AccountProvider'
import { LoginModal } from '@renderer/components/LoginModal'
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
  const { prime, activeAccount, profile, launch, launchMessage, launchProgress, clearLaunchMessage } = useAccounts()
  const [showLogin, setShowLogin] = useState(false)
  const [launching, setLaunching] = useState(false)
  const [instance, setInstance] = useState<GameInstance | null>(null)

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

  const mcUsername = activeAccount?.username ?? 'Guest'
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
    await launch(instance.id)
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
              <div className="text-subtitle">Sign in to play</div>
              <p className="text-caption">Connect Microsoft or use offline mode to launch Prime Client.</p>
            </div>
            <Button variant="primary" icon={<KeyRound size={16} />} onClick={() => setShowLogin(true)}>
              Add Account
            </Button>
          </div>
        </Card>
      )}

      {(launchMessage || launchProgress) && (
        <Card>
          {launchProgress && launchProgress.phase !== 'log' && (
            <p className="text-caption" style={{ marginBottom: 8 }}>
              {launchProgress.detail}
              {launchProgress.percent !== undefined ? ` · ${launchProgress.percent}%` : ''}
            </p>
          )}
          {launchMessage && (
            <p
              className="text-body"
              style={{
                color: launchMessage.includes('started') ? 'var(--prime-success)' : 'var(--prime-muted)'
              }}
            >
              {launchMessage}
            </p>
          )}
        </Card>
      )}

      <section className="dashboard__hero">
        <div className="dashboard__welcome">
          <span className="text-caption">Welcome back</span>
          <h1 className="text-display text-gradient">{mcUsername}</h1>
          <p className="text-body" style={{ color: 'var(--prime-muted)' }}>
            Prime Client {instance.minecraftVersion} · {instance.loader}
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
            {launching ? 'LAUNCHING...' : 'PLAY'}
          </Button>
          <span className="dashboard__instance">{instance.name}</span>
          <div className="dashboard__quick-actions">
            <Button variant="secondary" size="sm" icon={<Zap size={14} />} disabled={!activeAccount} onClick={() => void handlePlay()}>
              Quick Launch
            </Button>
            <Link to="/accounts">
              <Button variant="ghost" size="sm" icon={<UserCog size={14} />}>
                Profile
              </Button>
            </Link>
            <Link to="/instances">
              <Button variant="ghost" size="sm" icon={<Box size={14} />}>
                Instance
              </Button>
            </Link>
          </div>
        </div>
      </section>

      <div className="dashboard__grid">
        <Card title="Prime Account" glow className="dashboard__grid-span-2">
          <div className="dashboard__profile">
            <Avatar alt={mcUsername} size="xl" glow src={activeAccount?.skinUrl} />
            <div className="dashboard__profile-info">
              <h3>{prime?.username ?? 'Guest'}</h3>
              <Badge variant="prime">
                {(prime?.tier ?? 'free').toUpperCase()} · Lv.{prime?.level ?? 1}
              </Badge>
              <div className="dashboard__stats">
                <div>
                  <div className="dashboard__stat-value">{formatPlayTime(playTimeMinutes)}</div>
                  <div className="dashboard__stat-label">Time played</div>
                </div>
                <div>
                  <div className="dashboard__stat-value">{instance.modCount}</div>
                  <div className="dashboard__stat-label">Mods</div>
                </div>
                <div>
                  <div className="dashboard__stat-value">{instance.ramMb}MB</div>
                  <div className="dashboard__stat-label">RAM</div>
                </div>
              </div>
            </div>
          </div>
        </Card>

        <Card title="Last Session">
          <p className="text-body" style={{ marginBottom: 'var(--space-2)' }}>
            {lastPlayed ? new Date(lastPlayed).toLocaleDateString(undefined, { dateStyle: 'medium' }) : 'Never'}
          </p>
          <p className="text-caption">Version {instance.minecraftVersion}</p>
        </Card>

        <Card title="News">
          {news.map((item) => (
            <div key={item.id} className="dashboard__news-item">
              <div className="dashboard__news-title">
                <Badge variant={item.tag === 'update' ? 'red' : 'default'}>{item.tag}</Badge>{' '}
                {item.title}
              </div>
              <p className="dashboard__news-summary">{item.summary}</p>
            </div>
          ))}
        </Card>

        <Card title="Favorite Servers" className="dashboard__grid-span-2">
          {servers.map((s) => (
            <div key={s.id} className="dashboard__server">
              <div className="dashboard__server-info">
                <span className="dashboard__server-name">{s.name}</span>
                <span className="dashboard__server-meta">
                  {s.address} · {s.players ?? 0}/{s.maxPlayers ?? '?'} · {s.ping ?? '—'}ms
                </span>
              </div>
              <Button variant="secondary" size="sm">
                JOIN
              </Button>
            </div>
          ))}
        </Card>
      </div>

      <AnimatePresence>{showLogin && <LoginModal onClose={() => setShowLogin(false)} />}</AnimatePresence>
    </motion.div>
  )
}
