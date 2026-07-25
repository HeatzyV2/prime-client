import './Skeleton.css'

interface SkeletonProps {
  width?: string | number
  height?: string | number
  radius?: number
  className?: string
}

export function Skeleton({ width = '100%', height = 16, radius = 8, className = '' }: SkeletonProps) {
  return (
    <div
      className={`skeleton ${className}`.trim()}
      style={{ width, height, borderRadius: radius }}
      aria-hidden
    />
  )
}

export function LaunchStrip({
  detail,
  percent,
  phase
}: {
  detail?: string
  percent?: number
  phase?: string
}) {
  const pct = typeof percent === 'number' ? Math.max(0, Math.min(100, percent)) : null
  return (
    <div className={`launch-strip${phase === 'crashed' ? ' is-error' : ''}`}>
      <div className="launch-strip__row">
        <span className="launch-strip__label">{detail ?? 'Preparing…'}</span>
        {pct !== null && <span className="launch-strip__pct">{pct}%</span>}
      </div>
      <div className="launch-strip__track">
        <div
          className="launch-strip__fill"
          style={{ width: pct !== null ? `${pct}%` : '35%' }}
          data-indeterminate={pct === null ? 'true' : undefined}
        />
      </div>
    </div>
  )
}
