import { useState } from 'react'
import { Button, Toggle } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import type { SettingsPatch, SettingsState } from '../types'

export function AdvancedPanel({
  settings,
  patch,
  groqStatus,
  setGroqStatus,
  onSaved
}: {
  settings: SettingsState
  patch: SettingsPatch
  groqStatus: { hasKey: boolean; maskedKey: string | null; viaProxy: boolean }
  setGroqStatus: (v: { hasKey: boolean; maskedKey: string | null; viaProxy: boolean }) => void
  onSaved: () => void
}) {
  const { t } = useI18n()
  const [groqDraft, setGroqDraft] = useState('')

  return (
    <>
      <div className="settings__row">
        <div>
          <div className="settings__label">{t('settings.groqKey.label')}</div>
          <div className="settings__hint">{t('settings.groqKey.hint')}</div>
          <div className="settings__hint" style={{ marginTop: 6 }}>
            {groqStatus.viaProxy
              ? t('settings.groqKey.statusProxy')
              : groqStatus.maskedKey
                ? t('settings.groqKey.statusOk', { masked: groqStatus.maskedKey })
                : groqStatus.hasKey
                  ? t('settings.groqKey.statusMissing')
                  : t('settings.groqKey.statusDown')}
          </div>
        </div>
      </div>
      <div
        style={{
          display: 'flex',
          gap: 8,
          margin: '0 16px 16px',
          alignItems: 'center'
        }}
      >
        <input
          className="settings__input"
          style={{ flex: 1 }}
          type="password"
          value={groqDraft}
          placeholder={t('settings.groqKey.placeholder')}
          onChange={(e) => setGroqDraft(e.target.value)}
        />
        <Button
          variant="secondary"
          size="sm"
          onClick={() => {
            void window.primeLauncher.ai.setKey(groqDraft.trim()).then((status) => {
              setGroqStatus(status)
              setGroqDraft('')
              onSaved()
            })
          }}
        >
          {t('settings.groqKey.save')}
        </Button>
        <Button
          variant="ghost"
          size="sm"
          onClick={() => {
            void window.primeLauncher.ai.clearKey().then((status) => {
              setGroqStatus(status)
            })
          }}
        >
          {t('settings.groqKey.clear')}
        </Button>
      </div>
      <div className="settings__row">
        <div>
          <div className="settings__label">{t('settings.jvmArgs.label')}</div>
          <div className="settings__hint">{t('settings.jvmArgs.hint')}</div>
        </div>
      </div>
      <textarea
        className="settings__input"
        style={{ width: '100%', minHeight: 80, margin: '0 16px 16px', fontFamily: 'var(--font-mono)' }}
        value={settings.jvmArgs}
        onChange={(e) => void patch({ jvmArgs: e.target.value })}
      />
      <div className="settings__row">
        <div>
          <div className="settings__label">{t('settings.developerMode.label')}</div>
        </div>
        <Toggle
          checked={settings.developerMode}
          onChange={(v) => void patch({ developerMode: v })}
          label={t('settings.developerMode.toggle')}
        />
      </div>
    </>
  )
}
