import { useI18n } from '@renderer/context/I18nProvider'
import type { UpdateProgressDto, UpdateStatusDto } from '@shared/ipc'

export function UpdatesPanel({
  updateInfo,
  updateBusy,
  updateProgress,
  updateError,
  onCheck,
  onInstall
}: {
  updateInfo: UpdateStatusDto | null
  updateBusy: 'launcher' | 'mod' | 'check' | null
  updateProgress: UpdateProgressDto | null
  updateError: string | null
  onCheck: (force?: boolean) => void
  onInstall: (target: 'launcher' | 'mod') => void
}) {
  const { t } = useI18n()

  return (
    <>
      <div className="settings__row">
        <div>
          <div className="settings__label">{t('settings.checkUpdates.label')}</div>
          <div className="settings__hint">{t('settings.checkUpdates.hint')}</div>
        </div>
        <button
          className="settings__input"
          style={{ cursor: 'pointer' }}
          disabled={updateBusy !== null}
          onClick={() => onCheck(true)}
        >
          {updateBusy === 'check' ? t('updates.checking') : t('common.checkNow')}
        </button>
      </div>

      {updateInfo && (
        <>
          <div className="settings__row">
            <div>
              <div className="settings__label">{t('updates.launcher.label')}</div>
              <div className="settings__hint">
                {t('updates.versionLine', {
                  current: updateInfo.launcher.current,
                  latest: updateInfo.launcher.latest
                })}
              </div>
            </div>
            {updateInfo.launcher.updateAvailable ? (
              <button
                className="settings__input"
                style={{ cursor: 'pointer' }}
                disabled={updateBusy !== null}
                onClick={() => onInstall('launcher')}
              >
                {updateBusy === 'launcher' ? t('updates.installing') : t('updates.installLauncher')}
              </button>
            ) : (
              <span className="text-caption" style={{ color: 'var(--prime-success)', padding: '0 16px' }}>
                {t('updates.upToDate')}
              </span>
            )}
          </div>

          <div className="settings__row">
            <div>
              <div className="settings__label">{t('updates.mod.label')}</div>
              <div className="settings__hint">
                {t('updates.versionLine', {
                  current: updateInfo.mod.current,
                  latest: updateInfo.mod.latest
                })}
              </div>
            </div>
            {updateInfo.mod.updateAvailable ? (
              <button
                className="settings__input"
                style={{ cursor: 'pointer' }}
                disabled={updateBusy !== null}
                onClick={() => onInstall('mod')}
              >
                {updateBusy === 'mod' ? t('updates.installing') : t('updates.installMod')}
              </button>
            ) : (
              <span className="text-caption" style={{ color: 'var(--prime-success)', padding: '0 16px' }}>
                {t('updates.upToDate')}
              </span>
            )}
          </div>

          <div style={{ padding: '0 16px 16px' }}>
            <p className="text-caption" style={{ color: 'var(--prime-muted)' }}>
              {updateInfo.notes}
            </p>
            {updateProgress && (
              <p className="text-caption" style={{ color: 'var(--prime-muted)', marginTop: 8 }}>
                {updateProgress.detail ?? t(`updates.phase.${updateProgress.phase}`)}
                {updateProgress.percent > 0 ? ` · ${updateProgress.percent}%` : ''}
              </p>
            )}
            {updateError && (
              <p className="text-caption" style={{ color: 'var(--prime-error)', marginTop: 8 }}>
                {updateError}
              </p>
            )}
          </div>
        </>
      )}
    </>
  )
}
