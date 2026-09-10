import { Select } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import type { SettingsPatch, SettingsState } from '../types'

export function DownloadsPanel({
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
        <div className="settings__label">{t('settings.concurrentDownloads.label')}</div>
      </div>
      <Select
        size="sm"
        className="settings__select"
        value={String(settings.concurrentDownloads)}
        aria-label={t('settings.concurrentDownloads.label')}
        onChange={(v) => void patch({ concurrentDownloads: Number(v) })}
        options={[
          { value: '1', label: '1' },
          { value: '3', label: '3' },
          { value: '5', label: '5' }
        ]}
      />
    </div>
  )
}
