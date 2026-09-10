import type { CSSProperties } from 'react'
import { Toggle } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import { useTheme } from '@renderer/context/ThemeProvider'
import { isElevatedTheme } from '@shared/theme'
import type { SettingsPatch, SettingsState } from '../types'

const THEMES = [
  { id: 'prime-crimson' as const, swatch: '#e11d2e' },
  { id: 'prime-midnight' as const, swatch: '#38bdf8' },
  { id: 'prime-aurora' as const, swatch: '#34d399' },
  { id: 'prime-obsidian' as const, swatch: '#f0d78c' },
  { id: 'prime-ember' as const, swatch: '#fdba74' }
] as const

export function AppearancePanel({
  settings,
  patch,
  ownsNebula
}: {
  settings: SettingsState
  patch: SettingsPatch
  ownsNebula: boolean
}) {
  const { t } = useI18n()
  const { refreshTheme } = useTheme()

  return (
    <>
      <div className="settings__row">
        <div>
          <div className="settings__label">{t('settings.theme.label')}</div>
          <div className="settings__hint">{t('settings.theme.hint')}</div>
        </div>
        <div className="settings__theme-picker">
          {THEMES.map((opt) => {
            const elevated = isElevatedTheme(opt.id)
            return (
              <button
                key={opt.id}
                type="button"
                className={`settings__theme-swatch${settings.theme === opt.id ? ' settings__theme-swatch--active' : ''}${elevated ? ' settings__theme-swatch--elevated' : ''}`}
                style={{ '--swatch': opt.swatch } as CSSProperties}
                onClick={() => void patch({ theme: opt.id })}
                title={t(`settings.theme.${opt.id.replace('prime-', '')}`)}
              >
                <span className="settings__theme-swatch-dot" />
                <span>{t(`settings.theme.${opt.id.replace('prime-', '')}`)}</span>
                {elevated && (
                  <span className="settings__theme-elevated">{t('settings.theme.elevated')}</span>
                )}
              </button>
            )
          })}
        </div>
      </div>
      {ownsNebula && (
        <div className="settings__row">
          <div>
            <div className="settings__label">{t('settings.backgroundNebula.label')}</div>
            <div className="settings__hint">{t('settings.backgroundNebula.hint')}</div>
          </div>
          <Toggle
            checked={settings.backgroundNebula}
            onChange={(v) => void patch({ backgroundNebula: v })}
            label={t('settings.backgroundNebula.toggle')}
          />
        </div>
      )}
      <div className="settings__row">
        <div>
          <div className="settings__label">{t('settings.wallpaper.label')}</div>
          <div className="settings__hint">
            {settings.wallpaperPath ? settings.wallpaperPath : t('settings.wallpaper.hint')}
          </div>
        </div>
        <div className="settings__java-picker">
          <button
            type="button"
            className="settings__java-browse"
            onClick={() =>
              void (async () => {
                const result = await window.primeLauncher.settings.browseWallpaper()
                if (result.ok) {
                  await patch({ wallpaperPath: result.path ?? null })
                  await refreshTheme()
                }
              })()
            }
          >
            {t('settings.wallpaper.browse')}
          </button>
          {settings.wallpaperPath && (
            <button
              type="button"
              className="settings__java-browse"
              onClick={() =>
                void (async () => {
                  await window.primeLauncher.settings.clearWallpaper()
                  await patch({ wallpaperPath: null })
                  await refreshTheme()
                })()
              }
            >
              {t('settings.wallpaper.clear')}
            </button>
          )}
        </div>
      </div>
      <div className="settings__row">
        <div>
          <div className="settings__label">{t('settings.accent.label')}</div>
          <div className="settings__hint">{t('settings.accent.hint')}</div>
        </div>
        <div className="settings__java-picker">
          <input
            type="color"
            value={settings.accentColor ?? '#e11d2e'}
            onChange={(e) => void patch({ accentColor: e.target.value })}
            aria-label={t('settings.accent.label')}
          />
          {settings.accentColor && (
            <button
              type="button"
              className="settings__java-browse"
              onClick={() => void patch({ accentColor: null })}
            >
              {t('settings.accent.reset')}
            </button>
          )}
        </div>
      </div>
      <div className="settings__row">
        <div>
          <div className="settings__label">{t('settings.uiSounds.label')}</div>
          <div className="settings__hint">{t('settings.uiSounds.hint')}</div>
        </div>
        <Toggle
          checked={settings.uiSounds}
          onChange={(v) => void patch({ uiSounds: v })}
          label={t('settings.uiSounds.toggle')}
        />
      </div>
      <div className="settings__row">
        <div>
          <div className="settings__label">{t('settings.hardwareAccel.label')}</div>
        </div>
        <Toggle
          checked={settings.hardwareAccel}
          onChange={(v) => void patch({ hardwareAccel: v })}
          label={t('settings.hardwareAccel.toggle')}
        />
      </div>
      <div className="settings__row">
        <div>
          <div className="settings__label">{t('settings.performanceMode.label')}</div>
          <div className="settings__hint">{t('settings.performanceMode.hint')}</div>
        </div>
        <Toggle
          checked={settings.performanceMode}
          onChange={(v) => void patch({ performanceMode: v })}
          label={t('settings.performanceMode.toggle')}
        />
      </div>
    </>
  )
}
