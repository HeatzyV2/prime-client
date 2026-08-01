import type { ReactNode } from 'react'
import './page-shell.css'

interface PageShellProps {
  title: string
  subtitle?: string
  actions?: ReactNode
  children: ReactNode
}

/** CSS-only page enter fade — avoids Framer Motion cost on every route. */
export function PageShell({ title, subtitle, actions, children }: PageShellProps) {
  return (
    <div className="page-shell">
      <header className="page-shell__header">
        <div>
          <h1 className="page-shell__title">{title}</h1>
          {subtitle && <p className="page-shell__subtitle">{subtitle}</p>}
        </div>
        {actions && <div className="page-shell__actions">{actions}</div>}
      </header>
      {children}
    </div>
  )
}
