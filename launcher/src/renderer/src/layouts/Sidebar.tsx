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
  Newspaper,
  Film,
  Gauge,
  Download,
  Settings
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { NAV_ITEMS, type NavSection } from '@shared/types'
import { Avatar, Badge } from '@renderer/design-system/components'
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
  news: Newspaper,
  media: Film,
  performance: Gauge,
  downloads: Download,
  settings: Settings
}

interface SidebarProps {
  username: string
  tier: string
  skinUrl?: string
}

export function Sidebar({ username, tier, skinUrl }: SidebarProps) {
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
              {item.label}
            </NavLink>
          )
        })}
      </nav>

      <div className="sidebar__footer">
        <div className="sidebar__user">
          <Avatar alt={username} size="sm" glow src={skinUrl} />
          <div className="sidebar__user-info">
            <div className="sidebar__username">{username}</div>
            <div className="sidebar__tier">
              <Badge variant="prime">{tier}</Badge>
            </div>
          </div>
        </div>
      </div>
    </aside>
  )
}
