import type { CSSProperties, InputHTMLAttributes } from 'react'
import './Slider.css'

interface SliderProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type' | 'onChange'> {
  value: number
  min: number
  max: number
  step?: number
  label?: string
  valueLabel?: string
  onValueChange: (value: number) => void
}

export function Slider({
  value,
  min,
  max,
  step = 1,
  label,
  valueLabel,
  onValueChange,
  className = '',
  disabled,
  ...props
}: SliderProps) {
  const pct = max === min ? 0 : ((value - min) / (max - min)) * 100

  return (
    <label className={`v3-slider ${className}`.trim()}>
      {(label || valueLabel) && (
        <span className="v3-slider__head">
          {label && <span>{label}</span>}
          {valueLabel && <span className="v3-slider__value">{valueLabel}</span>}
        </span>
      )}
      <input
        type="range"
        className="v3-slider__input"
        min={min}
        max={max}
        step={step}
        value={value}
        disabled={disabled}
        style={{ '--slider-pct': `${pct}%` } as CSSProperties}
        onChange={(e) => onValueChange(Number(e.target.value))}
        {...props}
      />
    </label>
  )
}
