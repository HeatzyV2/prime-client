import { Button } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import './LaunchOverlay.css'

export type LaunchPhase =
  | 'start'
  | 'fabric'
  | 'download'
  | 'mods'
  | 'launch'
  | 'running'
  | 'error'
  | 'crashed'
  | string

interface LaunchOverlayProps {
  open: boolean
  title?: string
  phase?: LaunchPhase | null
  percent?: number | null
  detail?: string | null
  error?: boolean
  onCancel?: () => void
  onRetry?: () => void
  onDismiss?: () => void
}

const PHASE_KEYS: Record<string, string> = {
  start: 'dashboard.launchStatus.preparing',
  fabric: 'dashboard.launchStatus.fabric',
  download: 'dashboard.launchStatus.download',
  mods: 'dashboard.launchStatus.mods',
  launch: 'dashboard.launchStatus.starting',
  running: 'dashboard.launchStatus.running',
  error: 'dashboard.launchStatus.error',
  crashed: 'dashboard.launchStatus.crashed'
}

export function LaunchOverlay({
  open,
  title,
  phase,
  percent,
  detail,
  error,
  onCancel,
  onRetry,
  onDismiss
}: LaunchOverlayProps) {
  const { t } = useI18n()

  if (!open) return null

  const known = percent != null && percent >= 0 && percent <= 100
  const phaseKey = phase ? PHASE_KEYS[phase] : null
  const label = (phaseKey ? t(phaseKey) : null) || detail || t('dashboard.launchStatus.working')
  const resolvedTitle = title ?? t('dashboard.launchStatus.title')

  return (
    <div className="launch-overlay v3-fade-in" role="status" aria-live="polite">
      <div className="launch-overlay__panel">
        <p className="text-label">
          {error ? t('dashboard.launchStatus.errorLabel') : t('dashboard.launchStatus.label')}
        </p>
        <h2 className="launch-overlay__title">
          {error ? t('dashboard.launchStatus.unable') : resolvedTitle}
        </h2>
        <p className="launch-overlay__phase">{error ? detail || label : label}</p>

        {!error && (
          <div
            className={`launch-overlay__track${known ? '' : ' launch-overlay__track--indeterminate'}`}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={known ? percent! : undefined}
            role="progressbar"
          >
            <div
              className="launch-overlay__bar"
              style={known ? { width: `${percent}%` } : undefined}
            />
          </div>
        )}

        <div className="launch-overlay__actions">
          {error ? (
            <>
              {onRetry && (
                <Button variant="primary" onClick={onRetry}>
                  {t('dashboard.launchStatus.tryAgain')}
                </Button>
              )}
              {onDismiss && (
                <Button variant="ghost" onClick={onDismiss}>
                  {t('crash.dismiss')}
                </Button>
              )}
            </>
          ) : (
            onCancel && (
              <Button variant="ghost" onClick={onCancel}>
                {t('actions.cancel')}
              </Button>
            )
          )}
        </div>
      </div>
    </div>
  )
}
