import { Toggle } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import type { SettingsPatch, SettingsState } from '../types'

export function PrivacyPanel({
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
          <div className="settings__label">{t('settings.analytics.label')}</div>
          <div className="settings__hint">{t('settings.analytics.hint')}</div>
        </div>
        <Toggle
          checked={settings.analytics}
          onChange={(v) => void patch({ analytics: v })}
          label={t('settings.analytics.toggle')}
        />
      </div>
      <div className="settings__row">
        <div>
          <div className="settings__label">{t('settings.discordRpc.label')}</div>
          <div className="settings__hint">{t('settings.discordRpc.hint')}</div>
        </div>
        <Toggle
          checked={settings.discordRpc}
          onChange={(v) => void patch({ discordRpc: v })}
          label={t('settings.discordRpc.toggle')}
        />
      </div>
    </>
  )
}
