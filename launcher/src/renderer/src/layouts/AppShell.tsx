import { Outlet } from 'react-router-dom'
import { TitleBar } from './TitleBar'
import { Sidebar } from './Sidebar'
import { useAccounts } from '@renderer/context/AccountProvider'
import { useI18n } from '@renderer/context/I18nProvider'
import './AppShell.css'

interface AppShellProps {
  children?: React.ReactNode
}

export function AppShell({ children }: AppShellProps) {
  const { activeAccount, prime } = useAccounts()
  const { t } = useI18n()
  const username = activeAccount?.username ?? t('common.guest')
  const tier = prime?.tier ?? 'free'

  return (
    <div className="app-shell">
      <TitleBar />
      <div className="app-shell__body">
        <Sidebar username={username} tier={tier} uuid={activeAccount?.uuid} />
        <main className="app-shell__content">
          <div className="app-shell__content-inner">{children ?? <Outlet />}</div>
        </main>
      </div>
    </div>
  )
}
