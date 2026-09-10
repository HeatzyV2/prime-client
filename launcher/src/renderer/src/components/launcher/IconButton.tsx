import type { ButtonHTMLAttributes, ReactNode } from 'react'
import './IconButton.css'

interface IconButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  icon: ReactNode
  label: string
  active?: boolean
  size?: 'sm' | 'md'
}

export function IconButton({
  icon,
  label,
  active,
  size = 'md',
  className = '',
  ...props
}: IconButtonProps) {
  return (
    <button
      type="button"
      className={`icon-btn icon-btn--${size} ${active ? 'icon-btn--active' : ''} ${className}`.trim()}
      aria-label={label}
      title={label}
      {...props}
    >
      {icon}
    </button>
  )
}
