import { Minus, Square, X } from 'lucide-react'
import { PrimeLogo } from '@renderer/design-system/components'
import './TitleBar.css'

export function TitleBar() {
  return (
    <header className="titlebar">
      <div className="titlebar__left">
        <div className="titlebar__brand" aria-label="Prime">
          <PrimeLogo size={22} compact />
        </div>
      </div>
      <div className="titlebar__drag" aria-hidden />
      <div className="titlebar__controls">
        <button
          className="titlebar__btn"
          onClick={() => window.primeLauncher.window.minimize()}
          aria-label="Minimize"
        >
          <Minus size={14} strokeWidth={1.75} />
        </button>
        <button
          className="titlebar__btn"
          onClick={() => window.primeLauncher.window.maximize()}
          aria-label="Maximize"
        >
          <Square size={12} strokeWidth={1.75} />
        </button>
        <button
          className="titlebar__btn titlebar__btn--close"
          onClick={() => window.primeLauncher.window.close()}
          aria-label="Close"
        >
          <X size={14} strokeWidth={1.75} />
        </button>
      </div>
    </header>
  )
}
