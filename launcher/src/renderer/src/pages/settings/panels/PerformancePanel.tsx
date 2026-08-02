import { Select } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import type { PerformancePreset } from '@shared/content-types'
import type { SettingsPatch, SettingsState } from '../types'

export function PerformancePanel({
  settings,
  patch
}: {
  settings: SettingsState
  patch: SettingsPatch
}) {
  const { t } = useI18n()

  return (
    <div className="settings__row">
      <div>
        <div className="settings__label">{t('settings.performancePreset.label')}</div>
      </div>
      <Select
        size="sm"
        className="settings__select"
        value={settings.performancePreset}
        aria-label={t('settings.performancePreset.label')}
        onChange={(v) => void patch({ performancePreset: v as PerformancePreset })}
        options={[
          { value: 'low', label: t('settings.performancePreset.low') },
          { value: 'balanced', label: t('settings.performancePreset.balanced') },
          { value: 'performance', label: t('settings.performancePreset.performance') },
          { value: 'ultra', label: t('settings.performancePreset.ultra') }
        ]}
      />
    </div>
  )
}
