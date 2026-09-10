import { useId, useState, type ReactNode } from 'react'
import './Tooltip.css'

interface TooltipProps {
  content: string
  children: ReactNode
  side?: 'top' | 'bottom'
}

export function Tooltip({ content, children, side = 'top' }: TooltipProps) {
  const id = useId()
  const [open, setOpen] = useState(false)

  return (
    <span
      className="v3-tooltip"
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
      onFocus={() => setOpen(true)}
      onBlur={() => setOpen(false)}
    >
      <span aria-describedby={open ? id : undefined}>{children}</span>
      {open && (
        <span id={id} role="tooltip" className={`v3-tooltip__bubble v3-tooltip__bubble--${side}`}>
          {content}
        </span>
      )}
    </span>
  )
}
