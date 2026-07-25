import { useEffect, useRef, useState } from 'react'
import { NavLink, Link } from 'react-router-dom'
import {
  Home,
  UserCircle,
  Box,
  Puzzle,
  Image,
  Sun,
  ShoppingBag,
  Sparkles,
  Shirt,
  Library,
  Globe,
  Users,
  MessageCircle,
  Newspaper,
  Film,
  Gauge,
  Download,
  Terminal,
  Settings,
  MoreHorizontal
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { formatTier } from '@shared/format'
import { PRIMARY_NAV, SECONDARY_NAV, type NavSection } from '@shared/types'
import { Avatar, Badge } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import './Sidebar.css'

const ICONS: Record<NavSection, LucideIcon> = {
  dashboard: Home,
  accounts: UserCircle,
  instances: Box,
  skins: Shirt,
  library: Library,
  mods: Puzzle,
  resources: Image,
  shaders: Sun,
  store: ShoppingBag,
  cosmetics: Sparkles,
  servers: Globe,
  friends: Users,
  chat: MessageCircle,
  news: Newspaper,
  media: Film,
  performance: Gauge,
  downloads: Download,
  console: Terminal,
  settings: Settings
}

function pathFor(id: NavSection): string {
  if (id === 'dashboard') return '/'
  return `/${id}`
}

interface SidebarProps {
  username: string
  tier: string
  uuid?: string
}

export function Sidebar({ username, tier, uuid }: SidebarProps) {
  const { t, locale } = useI18n()
  const [moreOpen, setMoreOpen] = useState(false)
  const moreRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!moreOpen) return
    function onPointerDown(e: PointerEvent) {
      if (moreRef.current && !moreRef.current.contains(e.target as Node)) {
        setMoreOpen(false)
      }
    }
    document.addEventListener('pointerdown', onPointerDown)
    return () => document.removeEventListener('pointerdown', onPointerDown)
  }, [moreOpen])

  return (
    <aside className="sidebar">
      <nav className="sidebar__nav" aria-label={t('nav.primary')}>
        {PRIMARY_NAV.map((id) => {
          const Icon = ICONS[id]
          return (
            <NavLink
              key={id}
              to={pathFor(id)}
              title={t(`nav.${id}`)}
              className={({ isActive }) =>
                ['sidebar__link', isActive ? 'sidebar__link--active' : ''].filter(Boolean).join(' ')
              }
              end={id === 'dashboard'}
            >
              <Icon size={20} strokeWidth={1.75} />
              <span className="sidebar__tooltip">{t(`nav.${id}`)}</span>
            </NavLink>
          )
        })}
      </nav>

      <div className="sidebar__bottom">
        <div className="sidebar__more" ref={moreRef}>
          <button
            type="button"
            className={`sidebar__link sidebar__link--button${moreOpen ? ' is-open' : ''}`}
            title={t('nav.more')}
            aria-expanded={moreOpen}
            onClick={() => setMoreOpen((v) => !v)}
          >
            <MoreHorizontal size={20} strokeWidth={1.75} />
            <span className="sidebar__tooltip">{t('nav.more')}</span>
          </button>
          {moreOpen && (
            <div className="sidebar__more-menu" role="menu">
              {SECONDARY_NAV.map((id) => {
                const Icon = ICONS[id]
                return (
                  <Link
                    key={id}
                    to={pathFor(id)}
                    role="menuitem"
                    className="sidebar__more-item"
                    onClick={() => setMoreOpen(false)}
                  >
                    <Icon size={16} />
                    {t(`nav.${id}`)}
                  </Link>
                )
              })}
            </div>
          )}
        </div>

        <NavLink
          to="/settings"
          title={t('nav.settings')}
          className={({ isActive }) =>
            ['sidebar__link', isActive ? 'sidebar__link--active' : ''].filter(Boolean).join(' ')
          }
        >
          <Settings size={20} strokeWidth={1.75} />
          <span className="sidebar__tooltip">{t('nav.settings')}</span>
        </NavLink>

        <Link to="/accounts" className="sidebar__user" title={username}>
          <Avatar alt={username} uuid={uuid} size="sm" glow />
          <div className="sidebar__user-flyout">
            <div className="sidebar__username">{username}</div>
            <Badge variant="prime">{formatTier(tier, locale)}</Badge>
          </div>
        </Link>
      </div>
    </aside>
  )
}
