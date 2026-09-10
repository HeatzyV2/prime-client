import type { InputHTMLAttributes } from 'react'
import './Input.css'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  hint?: string
}

export function Input({ label, hint, className = '', id, ...props }: InputProps) {
  const inputId = id ?? (label ? `input-${label.replace(/\s+/g, '-').toLowerCase()}` : undefined)

  return (
    <label className={`v3-input ${className}`.trim()} htmlFor={inputId}>
      {label && <span className="v3-input__label">{label}</span>}
      <input id={inputId} className="v3-input__field" {...props} />
      {hint && <span className="v3-input__hint">{hint}</span>}
    </label>
  )
}
