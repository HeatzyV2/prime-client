import { NavLink } from 'react-router-dom'
import {
  Home,
  UserCircle,
  Box,
  Puzzle,
  Image,
  Sun,
  ShoppingBag,
  Sparkles,
  Globe,
  Users,
  MessageCircle,
  Newspaper,
  Film,
  Gauge,
  Download,
  Terminal,
  Settings
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { formatTier } from '@shared/format'
import { NAV_ITEMS, type NavSection } from '@shared/types'
import { Avatar, Badge } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import './Sidebar.css'

const ICONS: Record<NavSection, LucideIcon> = {
  dashboard: Home,
  accounts: UserCircle,
  instances: Box,
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

interface SidebarProps {
  username: string
  tier: string
  uuid?: string
}

export function Sidebar({ username, tier, uuid }: SidebarProps) {
  const { t, locale } = useI18n()

  return (
    <aside className="sidebar">
      <nav className="sidebar__nav">
        {NAV_ITEMS.map((item) => {
          const Icon = ICONS[item.id]
          return (
            <NavLink
              key={item.id}
              to={item.id === 'dashboard' ? '/' : `/${item.id}`}
              className={({ isActive }) =>
                ['sidebar__link', isActive ? 'sidebar__link--active' : ''].filter(Boolean).join(' ')
              }
              end={item.id === 'dashboard'}
            >
              <Icon size={18} />
              {t(`nav.${item.id}`)}
            </NavLink>
          )
        })}
      </nav>

      <div className="sidebar__footer">
        <div className="sidebar__user">
          <Avatar alt={username} uuid={uuid} size="sm" glow />
          <div className="sidebar__user-info">
            <div className="sidebar__username">{username}</div>
            <div className="sidebar__tier">
              <Badge variant="prime">{formatTier(tier, locale)}</Badge>
            </div>
          </div>
        </div>
      </div>
    </aside>
  )
}
