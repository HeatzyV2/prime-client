import { Minus, Square, X } from 'lucide-react'
import { PrimeLogo } from '@renderer/design-system/components'
import './TitleBar.css'

export function TitleBar() {
  return (
    <header className="titlebar">
      <div className="titlebar__left">
        <div className="titlebar__brand">
          <PrimeLogo size={28} />
          <span className="titlebar__name">Prime Launcher</span>
        </div>
      </div>
      <div className="titlebar__controls">
        <button className="titlebar__btn" onClick={() => window.primeLauncher.window.minimize()} aria-label="Minimize">
          <Minus size={16} />
        </button>
        <button className="titlebar__btn" onClick={() => window.primeLauncher.window.maximize()} aria-label="Maximize">
          <Square size={14} />
        </button>
        <button
          className="titlebar__btn titlebar__btn--close"
          onClick={() => window.primeLauncher.window.close()}
          aria-label="Close"
        >
          <X size={16} />
        </button>
      </div>
    </header>
  )
}
