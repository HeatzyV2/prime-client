import type { ButtonHTMLAttributes } from 'react'
import { useI18n } from '@renderer/context/I18nProvider'
import './PlayButton.css'

export type PlayButtonState =
  | 'idle'
  | 'hover'
  | 'pressed'
  | 'launching'
  | 'loading'
  | 'disabled'
  | 'error'
  | 'running'

interface PlayButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  state?: PlayButtonState
  label?: string
  sublabel?: string
}

export function PlayButton({
  state = 'idle',
  label,
  sublabel,
  className = '',
  disabled,
  ...props
}: PlayButtonProps) {
  const { t } = useI18n()
  const isBusy = state === 'launching' || state === 'loading'
  const isDisabled = disabled || state === 'disabled'
  const idleLabel = label ?? t('common.play')
  const displayLabel =
    state === 'running'
      ? t('common.running')
      : state === 'error'
        ? t('common.retry')
        : isBusy
          ? t('common.launching')
          : idleLabel

  return (
    <button
      type="button"
      className={`play-btn play-btn--${state} ${className}`.trim()}
      disabled={isDisabled || isBusy}
      aria-busy={isBusy}
      data-state={state}
      {...props}
    >
      <span className="play-btn__ring" aria-hidden />
      <span className="play-btn__fill">
        {isBusy && <span className="play-btn__spinner" aria-hidden />}
        <span className="play-btn__label">{displayLabel}</span>
        {sublabel && !isBusy && <span className="play-btn__sub">{sublabel}</span>}
      </span>
    </button>
  )
}
