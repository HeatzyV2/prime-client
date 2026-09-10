import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { AnimatePresence } from 'framer-motion'
import { ArrowRight } from 'lucide-react'
import { PlayButton, type PlayButtonState } from '@renderer/components/launcher/PlayButton'
import { LaunchOverlay } from '@renderer/components/launcher/LaunchOverlay'
import { Sheet, Button } from '@renderer/design-system/components'
import { useAccounts } from '@renderer/context/AccountProvider'
import { useI18n } from '@renderer/context/I18nProvider'
import { LoginModal } from '@renderer/components/LoginModal'
import { CrashReportPanel } from '@renderer/components/CrashReportPanel'
import { InstanceModal, type InstancePreset } from '@renderer/components/InstanceModal'
import { formatLoader } from '@shared/format'
import type { FavoriteServer, GameInstance, NewsItem } from '@shared/types'
import { playUiSound } from '@renderer/lib/uiSounds'
import './HomePage.css'

interface HomePageProps {
  servers: FavoriteServer[]
}

function WorldArt() {
  return (
    <div className="home-world" aria-hidden>
      <div className="home-world__sky" />
      <div className="home-world__nebula home-world__nebula--a" />
      <div className="home-world__nebula home-world__nebula--b" />
      <div className="home-world__nebula home-world__nebula--c" />
      <div className="home-world__haze" />
      <div className="home-world__haze home-world__stars--near" />
      <div className="home-world__haze" />
      <div className="home-world__dust" />
      <div className="home-world__horizon" />
      <div className="home-world__glow-floor" />
      <div className="home-world__vignette" />
      <div className="home-world__scrim" />
    </div>
  )
}

function formatShortDate(value?: string): string | null {
  if (!value) return null
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return null
  return date.toLocaleDateString(undefined, { day: 'numeric', month: 'short' })
}

export function HomePage({ servers }: HomePageProps) {
  const { t } = useI18n()
  const {
    accounts,
    activeAccount,
    profile,
    launch,
    launchMessage,
    launchProgress,
    gameRunning,
    clearLaunchMessage,
    clearLaunchProgress,
    setActive
  } = useAccounts()

  const [showLogin, setShowLogin] = useState(false)
  const [launching, setLaunching] = useState(false)
  const [instances, setInstances] = useState<GameInstance[]>([])
  const [instance, setInstance] = useState<GameInstance | null>(null)
  const [selectedServer, setSelectedServer] = useState('')
  const [sheet, setSheet] = useState<'account' | 'instance' | 'server' | null>(null)
  const [createOpen, setCreateOpen] = useState(false)
  const [crashDismissed, setCrashDismissed] = useState(false)
  const [overlayDismissed, setOverlayDismissed] = useState(false)
  const [headline, setHeadline] = useState<NewsItem | null>(null)
  const [appVersion, setAppVersion] = useState('')

  async function refreshInstances() {
    const list = (await window.primeLauncher.instance.list()) as GameInstance[]
    setInstances(list)
    let current: GameInstance | null = null
    if (profile?.instanceId) {
      current = list.find((i) => i.id === profile.instanceId) ?? null
    }
    if (!current) {
      current = (await window.primeLauncher.instance.getDefault()) ?? list[0] ?? null
    }
    setInstance(current)
  }

  useEffect(() => {
    void refreshInstances()
  }, [profile?.instanceId])

  useEffect(() => {
    void (async () => {
      const settings = await window.primeLauncher.settings.get()
      const last =
        typeof settings.lastServerAddress === 'string' ? settings.lastServerAddress.trim() : ''
      if (last) {
        setSelectedServer(last)
        return
      }
      if (servers[0]?.address) setSelectedServer(servers[0].address)
    })()
  }, [servers])

  useEffect(() => {
    void window.primeLauncher.news.list().then((items) => {
      setHeadline(items[0] ?? null)
    })
    void window.primeLauncher.app.getVersion().then(setAppVersion)
  }, [])

  useEffect(() => {
    if (launchProgress?.phase === 'crashed') setCrashDismissed(false)
    if (launchProgress?.phase === 'error' || launchProgress?.phase === 'start') {
      setOverlayDismissed(false)
    }
  }, [launchProgress?.phase])

  const selectedServerName = useMemo(() => {
    if (!selectedServer) return t('dashboard.playHub.singleplayer')
    return servers.find((s) => s.address === selectedServer)?.name ?? selectedServer
  }, [selectedServer, servers, t])

  const dockServers = useMemo(() => servers.slice(0, 3), [servers])
  const lastPlayed = formatShortDate(instance?.lastPlayed ?? profile?.lastPlayed)
  const newsTitle = headline?.title ?? t('whatsNew.title')
  const newsBlurb = headline?.summary ?? t('dashboard.whatsNew.themes')

  const preparing =
    launching ||
    (launchProgress != null &&
      ['start', 'fabric', 'download', 'mods', 'launch'].includes(launchProgress.phase))

  const launchError = launchProgress?.phase === 'error'
  const showOverlay = !overlayDismissed && (preparing || launchError)

  const playState: PlayButtonState = !instance
    ? 'disabled'
    : launchError
      ? 'error'
      : gameRunning
        ? 'running'
        : preparing
          ? 'launching'
          : 'idle'

  async function handlePlay() {
    if (!activeAccount || !instance) {
      setShowLogin(true)
      return
    }
    if (gameRunning) return
    setLaunching(true)
    setOverlayDismissed(false)
    playUiSound('click')
    clearLaunchMessage()
    try {
      await window.primeLauncher.profile.setInstance(instance.id)
      const server = selectedServer.trim() || undefined
      if (server) {
        await window.primeLauncher.settings.update({ lastServerAddress: server })
      }
      await launch(instance.id, server)
    } finally {
      setLaunching(false)
    }
  }

  async function handleInstanceChange(id: string) {
    const next = instances.find((i) => i.id === id)
    if (!next) return
    setInstance(next)
    await window.primeLauncher.profile.setInstance(id)
    playUiSound('click')
    setSheet(null)
  }

  async function handleAccountChange(id: string) {
    await setActive(id)
    playUiSound('click')
    setSheet(null)
  }

  function handleServerChange(address: string) {
    setSelectedServer(address)
    playUiSound('click')
    setSheet(null)
  }

  function pickServer(address: string) {
    setSelectedServer(address)
    playUiSound('click')
    void window.primeLauncher.settings.update({ lastServerAddress: address })
  }

  const ramLabel = instance ? `${Math.round(instance.ramMb / 1024)} GB` : '—'
  const greeting = activeAccount
    ? `${t('dashboard.welcomeBack')}, ${activeAccount.username}`
    : t('dashboard.welcomeGuest')

  return (
    <div className="home-hero">
      <WorldArt />

      {launchProgress?.phase === 'crashed' && launchProgress.crash && !crashDismissed && (
        <div className="home-hero__crash">
          <CrashReportPanel crash={launchProgress.crash} onDismiss={() => setCrashDismissed(true)} />
        </div>
      )}

      <div className="home-hero__brand">
        <div className="home-hero__identity">
          <span className="home-hero__wordmark">Prime</span>
          <span className="home-hero__product">Client</span>
        </div>
      </div>

      <div className="home-hero__stage">
        <p className="home-hero__greeting">{greeting}</p>

        {!instance && <p className="home-hero__mc">{t('dashboard.signInHint')}</p>}

        <div className="home-hero__core">
          <div className="home-hero__play-wrap">
            <div className="home-hero__play-light" aria-hidden />
            <PlayButton
              state={playState}
              label={t('common.play')}
              onClick={() => void handlePlay()}
              disabled={!instance && accounts.length === 0}
            />
          </div>

          <button type="button" className="home-hero__instance" onClick={() => setSheet('instance')}>
            <span className="home-hero__instance-kicker">{t('dashboard.playHub.instance')}</span>
            <strong className="home-hero__instance-name">{instance?.name ?? '—'}</strong>
            <span className="home-hero__instance-version">
              {instance
                ? `Minecraft ${instance.minecraftVersion} · ${formatLoader(instance.loader)}`
                : t('dashboard.signInHint')}
            </span>
            <span className="home-hero__instance-meta">
              {instance
                ? [
                    t('instances.modsBadge', { count: instance.modCount }),
                    lastPlayed ? t('instances.lastPlayed', { date: lastPlayed }) : null
                  ]
                    .filter(Boolean)
                    .join(' · ')
                : '—'}
            </span>
            <span className="home-hero__instance-change">{t('dashboard.playHub.change')}</span>
          </button>

          <div className="home-hero__cluster" role="group" aria-label={t('dashboard.playHub.quickSettings')}>
            <button type="button" onClick={() => setSheet('server')}>
              {selectedServerName}
            </button>
            <span aria-hidden>·</span>
            <span>
              {ramLabel} {t('dashboard.ram')}
            </span>
            <span aria-hidden>·</span>
            <button
              type="button"
              onClick={() => (accounts.length ? setSheet('account') : setShowLogin(true))}
            >
              {activeAccount?.username ?? t('common.guest')}
            </button>
          </div>
        </div>
      </div>

      <div className="home-hero__dock">
        <section className="home-hero__dock-servers" aria-label={t('dashboard.favoriteServers')}>
          <div className="home-hero__dock-head">
            <span className="text-label">{t('dashboard.jumpIn')}</span>
            <Link to="/servers">{t('dashboard.openServers')}</Link>
          </div>
          {dockServers.length > 0 ? (
            <div className="home-hero__server-row">
              {dockServers.map((s) => {
                const active = s.address === selectedServer
                const offline = s.online === false
                const playersLabel =
                  s.players != null && s.maxPlayers != null
                    ? `${s.players}/${s.maxPlayers}`
                    : null
                return (
                  <button
                    key={s.id}
                    type="button"
                    className={`home-hero__server-card${active ? ' is-active' : ''}`}
                    onClick={() => pickServer(s.address)}
                  >
                    <div className="home-hero__server-card-top">
                      <span
                        className={`home-hero__server-live${offline ? ' is-offline' : ' is-online'}`}
                      >
                        <span className="home-hero__server-dot" aria-hidden />
                        {offline ? t('servers.offline') : t('servers.online')}
                      </span>
                      <span className="home-hero__server-ping">
                        {offline ? '—' : s.ping != null ? `${s.ping} ms` : '…'}
                      </span>
                    </div>
                    <strong className="home-hero__server-name">{s.name}</strong>
                    {playersLabel && (
                      <span className="home-hero__server-players">{playersLabel}</span>
                    )}
                  </button>
                )
              })}
            </div>
          ) : (
            <p className="home-hero__dock-empty">
              {t('dashboard.noServersHint')}{' '}
              <Link to="/servers">{t('dashboard.addServer')}</Link>
            </p>
          )}
        </section>

        <Link to="/news" className="home-hero__dock-news">
          <div className="home-hero__dock-head">
            <span className="text-label">{t('dashboard.openNews')}</span>
            {appVersion ? <span className="home-hero__dock-version">{appVersion}</span> : null}
          </div>
          <strong className="home-hero__dock-news-title">{newsTitle}</strong>
          <span className="home-hero__dock-news-blurb">{newsBlurb}</span>
          <span className="home-hero__dock-news-cta">
            {t('dashboard.seeWhatsNew')}
            <ArrowRight size={12} strokeWidth={2} aria-hidden />
          </span>
        </Link>
      </div>

      <LaunchOverlay
        open={showOverlay}
        phase={launchProgress?.phase}
        percent={launchProgress?.percent}
        detail={launchMessage || launchProgress?.detail}
        error={launchError}
        onRetry={() => {
          clearLaunchProgress()
          clearLaunchMessage()
          setOverlayDismissed(false)
          void handlePlay()
        }}
        onDismiss={() => {
          setOverlayDismissed(true)
          clearLaunchMessage()
          clearLaunchProgress()
        }}
      />

      <Sheet
        open={sheet != null}
        title={
          sheet === 'account'
            ? t('dashboard.playHub.account')
            : sheet === 'instance'
              ? t('dashboard.playHub.instance')
              : sheet === 'server'
                ? t('dashboard.playHub.server')
                : undefined
        }
        onClose={() => setSheet(null)}
        footer={
          sheet === 'instance' ? (
            <div className="home-hero__sheet-footer">
              <Button
                variant="primary"
                onClick={() => {
                  setSheet(null)
                  setCreateOpen(true)
                }}
              >
                + {t('dashboard.playHub.createInstance')}
              </Button>
              <Link
                to="/instances"
                className="home-hero__manage"
                onClick={() => setSheet(null)}
              >
                {t('dashboard.playHub.manageAll')}
              </Link>
            </div>
          ) : (
            <button type="button" className="home-hero__close" onClick={() => setSheet(null)}>
              {t('common.close')}
            </button>
          )
        }
      >
        <div className="home-hero__list">
          {sheet === 'account' &&
            accounts.map((a) => (
              <button
                key={a.id}
                type="button"
                className={`home-hero__item${a.id === activeAccount?.id ? ' is-active' : ''}`}
                onClick={() => void handleAccountChange(a.id)}
              >
                {a.username}
                <span>{a.type}</span>
              </button>
            ))}
          {sheet === 'instance' &&
            instances.map((i) => (
              <button
                key={i.id}
                type="button"
                className={`home-hero__item${i.id === instance?.id ? ' is-active' : ''}`}
                onClick={() => void handleInstanceChange(i.id)}
              >
                <span className="home-hero__item-main">
                  {i.name}
                  <strong>
                    Minecraft {i.minecraftVersion} · {formatLoader(i.loader)}
                  </strong>
                </span>
                {i.id === instance?.id && <em>✓</em>}
              </button>
            ))}
          {sheet === 'server' && (
            <>
              <button
                type="button"
                className={`home-hero__item${!selectedServer ? ' is-active' : ''}`}
                onClick={() => handleServerChange('')}
              >
                {t('dashboard.playHub.singleplayer')}
                <span>{t('dashboard.playHub.noServer')}</span>
              </button>
              {servers.map((s) => (
                <button
                  key={s.id}
                  type="button"
                  className={`home-hero__item${s.address === selectedServer ? ' is-active' : ''}`}
                  onClick={() => handleServerChange(s.address)}
                >
                  {s.name}
                  <span>
                    {s.online === false
                      ? t('servers.offline')
                      : s.ping != null
                        ? `${s.ping} ms`
                        : t('servers.online')}
                  </span>
                </button>
              ))}
            </>
          )}
        </div>
      </Sheet>

      <AnimatePresence>
        {createOpen && (
          <InstanceModal
            mode="create"
            preset={'prime' as InstancePreset}
            onClose={() => setCreateOpen(false)}
            onSaved={() => void refreshInstances()}
          />
        )}
      </AnimatePresence>

      {showLogin && <LoginModal onClose={() => setShowLogin(false)} />}
    </div>
  )
}
