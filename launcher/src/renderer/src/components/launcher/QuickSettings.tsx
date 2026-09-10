import { useI18n } from '@renderer/context/I18nProvider'
import './QuickSettings.css'

export interface QuickSettingChip {
  id: string
  label: string
  value: string
  onClick?: () => void
}

interface QuickSettingsProps {
  chips: QuickSettingChip[]
}

export function QuickSettings({ chips }: QuickSettingsProps) {
  const { t } = useI18n()

  return (
    <div className="quick-settings" role="group" aria-label={t('dashboard.playHub.quickSettings')}>
      {chips.map((chip, index) => (
        <span key={chip.id} className="quick-settings__wrap">
          {index > 0 && <span className="quick-settings__dot" aria-hidden />}
          <button
            type="button"
            className="quick-settings__chip"
            onClick={chip.onClick}
            disabled={!chip.onClick}
          >
            <span className="quick-settings__label">{chip.label}</span>
            <span className="quick-settings__value">{chip.value}</span>
          </button>
        </span>
      ))}
    </div>
  )
}
