import { useEffect, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import './Sheet.css'

interface SheetProps {
  open: boolean
  title?: string
  onClose: () => void
  children: ReactNode
  footer?: ReactNode
}

export function Sheet({ open, title, onClose, children, footer }: SheetProps) {
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, onClose])

  if (!open) return null

  return createPortal(
    <div className="v3-sheet" role="presentation" onClick={onClose}>
      <div
        className="v3-sheet__panel v3-fade-in"
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onClick={(e) => e.stopPropagation()}
      >
        {title && <p className="text-label">{title}</p>}
        <div className="v3-sheet__body">{children}</div>
        {footer && <div className="v3-sheet__footer">{footer}</div>}
      </div>
    </div>,
    document.body
  )
}
