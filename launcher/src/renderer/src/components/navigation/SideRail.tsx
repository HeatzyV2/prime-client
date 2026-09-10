import { NavLink } from 'react-router-dom'
import { Home, Layers, Server, Shirt, Settings } from 'lucide-react'
import { useI18n } from '@renderer/context/I18nProvider'
import './SideRail.css'

const ITEMS = [
  { to: '/', id: 'home', icon: Home, end: true, labelKey: 'nav.dashboard' as const },
  { to: '/instances', id: 'instances', icon: Layers, end: false, labelKey: 'nav.instances' as const },
  { to: '/servers', id: 'servers', icon: Server, end: false, labelKey: 'nav.servers' as const },
  { to: '/cosmetics', id: 'cosmetics', icon: Shirt, end: false, labelKey: 'nav.cosmetics' as const },
  { to: '/settings', id: 'settings', icon: Settings, end: false, labelKey: 'nav.settings' as const }
]

export function SideRail() {
  const { t } = useI18n()

  return (
    <nav className="side-rail" aria-label={t('nav.primary')}>
      <div className="side-rail__inner">
        {ITEMS.map((item) => {
          const Icon = item.icon
          return (
            <NavLink
              key={item.id}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `side-rail__item${isActive ? ' side-rail__item--active' : ''}`
              }
              title={t(item.labelKey)}
              aria-label={t(item.labelKey)}
            >
              <Icon size={17} strokeWidth={1.6} aria-hidden />
            </NavLink>
          )
        })}
      </div>
    </nav>
  )
}
