import { Select } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import type { JavaInstallationDto } from '@shared/ipc'
import type { SettingsPatch, SettingsState } from '../types'

export function MinecraftPanel({
  settings,
  patch,
  javaInstalls,
  setJavaInstalls
}: {
  settings: SettingsState
  patch: SettingsPatch
  javaInstalls: JavaInstallationDto[]
  setJavaInstalls: (v: JavaInstallationDto[]) => void
}) {
  const { t } = useI18n()

  return (
    <>
      <div className="settings__row">
        <div>
          <div className="settings__label">{t('settings.javaPath.label')}</div>
          <div className="settings__hint">{t('settings.javaPath.hint')}</div>
        </div>
        <div className="settings__java-picker">
          <Select
            size="sm"
            className="settings__select"
            value={settings.defaultJavaPath ?? 'auto'}
            aria-label={t('settings.javaPath.label')}
            onChange={(v) =>
              void patch({
                defaultJavaPath: v === 'auto' ? null : v
              })
            }
            options={[
              { value: 'auto', label: t('common.automatic') },
              ...javaInstalls.map((java) => ({ value: java.path, label: java.label }))
            ]}
          />
          <button
            className="settings__input settings__java-browse"
            style={{ cursor: 'pointer' }}
            onClick={() =>
              void (async () => {
                const result = await window.primeLauncher.settings.browseJava()
                if (!result.ok || !result.install) {
                  if (result.error && result.error !== 'Cancelled.') {
                    window.alert(result.error)
                  }
                  return
                }
                const added = await window.primeLauncher.settings.addJavaPath(result.install.path)
                if (!added.ok || !added.install) {
                  window.alert(added.error ?? t('settings.javaPath.browseFailed'))
                  return
                }
                setJavaInstalls(await window.primeLauncher.settings.listJava())
                await patch({ defaultJavaPath: added.install.path })
              })()
            }
          >
            {t('settings.javaPath.addPath')}
          </button>
        </div>
      </div>
      <div className="settings__row">
        <div>
          <div className="settings__label">{t('settings.defaultRam.label')}</div>
        </div>
        <Select
          size="sm"
          className="settings__select"
          value={String(settings.defaultRamMb)}
          aria-label={t('settings.defaultRam.label')}
          onChange={(v) => void patch({ defaultRamMb: Number(v) })}
          options={[
            { value: '2048', label: '2048 MB' },
            { value: '4096', label: '4096 MB' },
            { value: '6144', label: '6144 MB' },
            { value: '8192', label: '8192 MB' }
          ]}
        />
      </div>
      <div className="settings__row">
        <div>
          <div className="settings__label">{t('settings.gameResolution.label')}</div>
          <div className="settings__hint">{t('settings.gameResolution.hint')}</div>
        </div>
        <div className="settings__resolution">
          <input
            type="number"
            className="settings__input settings__resolution-input"
            min={320}
            max={7680}
            value={settings.gameWidth}
            onChange={(e) => void patch({ gameWidth: Number(e.target.value) || 854 })}
          />
          <span className="settings__resolution-sep">×</span>
          <input
            type="number"
            className="settings__input settings__resolution-input"
            min={240}
            max={4320}
            value={settings.gameHeight}
            onChange={(e) => void patch({ gameHeight: Number(e.target.value) || 480 })}
          />
        </div>
      </div>
      <div className="settings__row">
        <div>
          <div className="settings__label">{t('settings.gameDisplayMode.label')}</div>
          <div className="settings__hint">{t('settings.gameDisplayMode.hint')}</div>
        </div>
        <Select
          size="sm"
          className="settings__select"
          value={settings.gameDisplayMode}
          aria-label={t('settings.gameDisplayMode.label')}
          onChange={(v) =>
            void patch({
              gameDisplayMode: v as SettingsState['gameDisplayMode']
            })
          }
          options={[
            { value: 'windowed', label: t('settings.gameDisplayMode.windowed') },
            { value: 'borderless', label: t('settings.gameDisplayMode.borderless') },
            { value: 'fullscreen', label: t('settings.gameDisplayMode.fullscreen') }
          ]}
        />
      </div>
    </>
  )
}
