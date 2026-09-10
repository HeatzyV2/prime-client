import { useEffect, useState, type ReactNode } from 'react'
import { AnimatePresence } from 'framer-motion'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { TitleBar } from './TitleBar'
import { SideRail } from '@renderer/components/navigation/SideRail'
import { Avatar } from '@renderer/design-system/components'
import { IconButton } from '@renderer/components/launcher/IconButton'
import { useAccounts } from '@renderer/context/AccountProvider'
import { useI18n } from '@renderer/context/I18nProvider'
import { OnboardingModal } from '@renderer/components/OnboardingModal'
import { WhatsNewModal, resolveWhatsNew } from '@renderer/components/WhatsNewModal'
import { LoginModal } from '@renderer/components/LoginModal'
import { Ellipsis, FlaskConical } from 'lucide-react'
import './V3Shell.css'

interface V3ShellProps {
  children?: ReactNode
}

export function V3Shell({ children }: V3ShellProps) {
  const { activeAccount } = useAccounts()
  const { t } = useI18n()
  const navigate = useNavigate()
  const location = useLocation()
  const [showOnboarding, setShowOnboarding] = useState(false)
  const [whatsNew, setWhatsNew] = useState<ReturnType<typeof resolveWhatsNew>>(null)
  const [showLogin, setShowLogin] = useState(false)
  const [moreOpen, setMoreOpen] = useState(false)
  const isLab = location.pathname.startsWith('/design-lab')

  useEffect(() => {
    void (async () => {
      const [settings, version] = await Promise.all([
        window.primeLauncher.settings.get(),
        window.primeLauncher.app.getVersion()
      ])
      if (!settings.onboardingDone) setShowOnboarding(true)
      const entry = resolveWhatsNew(version)
      if (entry && settings.lastSeenLauncherVersion !== version) {
        setWhatsNew(entry)
      }
    })()
  }, [])

  useEffect(() => {
    if (!moreOpen) return
    const onPointer = (event: MouseEvent) => {
      const target = event.target as HTMLElement | null
      if (target?.closest('.v3-shell__more')) return
      setMoreOpen(false)
    }
    document.addEventListener('mousedown', onPointer)
    return () => document.removeEventListener('mousedown', onPointer)
  }, [moreOpen])

  return (
    <div className={`v3-shell${isLab ? ' v3-shell--lab' : ''}`}>
      <TitleBar />
      <div className="v3-shell__body">
        {!isLab && <SideRail />}
        <header className="v3-shell__top">
          <div className="v3-shell__top-actions">
            <div className="v3-shell__more">
              <IconButton
                icon={<Ellipsis size={18} strokeWidth={1.75} />}
                label={t('nav.more')}
                active={moreOpen}
                onClick={() => setMoreOpen((v) => !v)}
              />
              {moreOpen && (
                <div className="v3-shell__menu" role="menu">
                  {[
                    ['instances', '/instances'],
                    ['store', '/store'],
                    ['host', '/host'],
                    ['friends', '/friends'],
                    ['library', '/library'],
                    ['downloads', '/downloads'],
                    ['console', '/console']
                  ].map(([key, path]) => (
                    <button
                      key={path}
                      type="button"
                      role="menuitem"
                      className="v3-shell__menu-item"
                      onClick={() => {
                        setMoreOpen(false)
                        navigate(path)
                      }}
                    >
                      {t(`nav.${key}` as 'nav.store')}
                    </button>
                  ))}
                  <button
                    type="button"
                    role="menuitem"
                    className="v3-shell__menu-item"
                    onClick={() => {
                      setMoreOpen(false)
                      navigate('/design-lab')
                    }}
                  >
                    <FlaskConical size={14} strokeWidth={1.75} aria-hidden /> {t('nav.designLab')}
                  </button>
                </div>
              )}
            </div>
            <button
              type="button"
              className="v3-shell__account"
              onClick={() => (activeAccount ? navigate('/settings') : setShowLogin(true))}
              aria-label={activeAccount?.username ?? t('common.signIn')}
            >
              <Avatar
                uuid={activeAccount?.uuid}
                alt={activeAccount?.username ?? t('common.guest')}
                size="sm"
              />
            </button>
          </div>
        </header>

        <main className={`v3-shell__content${isLab ? ' v3-shell__content--lab' : ''}`}>
          {children ?? <Outlet />}
        </main>
      </div>

      <AnimatePresence>
        {showOnboarding && <OnboardingModal onDone={() => setShowOnboarding(false)} />}
      </AnimatePresence>
      <AnimatePresence>
        {!showOnboarding && whatsNew && (
          <WhatsNewModal entry={whatsNew} onClose={() => setWhatsNew(null)} />
        )}
      </AnimatePresence>
      {showLogin && <LoginModal onClose={() => setShowLogin(false)} />}
    </div>
  )
}
