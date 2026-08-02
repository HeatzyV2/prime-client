import { Select, Toggle } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import { LOCALES } from '@shared/i18n'
import type { SettingsPatch, SettingsState } from '../types'

export function GeneralPanel({
  settings,
  patch
}: {
  settings: SettingsState
  patch: SettingsPatch
}) {
  const { t } = useI18n()

  return (
    <>
      <div className="settings__row">
        <div>
          <div className="settings__label">{t('settings.language.label')}</div>
          <div className="settings__hint">{t('settings.language.hint')}</div>
        </div>
        <Select
          size="sm"
          className="settings__select"
          value={settings.language}
          aria-label={t('settings.language.label')}
          onChange={(v) => void patch({ language: v as 'en' | 'fr' })}
          options={LOCALES.map((lang) => ({ value: lang.id, label: lang.label }))}
        />
      </div>
      <div className="settings__row">
        <div>
          <div className="settings__label">{t('settings.closeOnLaunch.label')}</div>
          <div className="settings__hint">{t('settings.closeOnLaunch.hint')}</div>
        </div>
        <Toggle
          checked={settings.closeOnLaunch}
          onChange={(v) => void patch({ closeOnLaunch: v })}
          label={t('settings.closeOnLaunch.toggle')}
        />
      </div>
      <div className="settings__row">
        <div>
          <div className="settings__label">{t('settings.autoUpdate.label')}</div>
          <div className="settings__hint">{t('settings.autoUpdate.hint')}</div>
        </div>
        <Toggle
          checked={settings.autoUpdate}
          onChange={(v) => void patch({ autoUpdate: v })}
          label={t('settings.autoUpdate.toggle')}
        />
      </div>
    </>
  )
}
