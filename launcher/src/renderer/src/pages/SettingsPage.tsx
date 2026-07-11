import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Toggle } from '@renderer/design-system/components'
import { useAccounts } from '@renderer/context/AccountProvider'
import { useI18n } from '@renderer/context/I18nProvider'
import { useTheme } from '@renderer/context/ThemeProvider'
import { LOCALES } from '@shared/i18n'
import type { PerformancePreset } from '@shared/content-types'
import type { StoreItem } from '@shared/content-types'
import type { UpdateCheckDto, JavaInstallationDto, SettingsUpdateDto } from '@shared/ipc'
import './SettingsPage.css'

const SECTION_IDS = [
  'general',
  'appearance',
  'minecraft',
  'performance',
  'accounts',
  'privacy',
  'downloads',
  'advanced'
] as const

interface SettingsState {
  language: 'en' | 'fr'
  closeOnLaunch: boolean
  autoUpdate: boolean
  theme: 'prime-dark' | 'prime-crimson'
  backgroundNebula: boolean
  hardwareAccel: boolean
  defaultRamMb: number
  defaultJavaPath: string | null
  performancePreset: PerformancePreset
  analytics: boolean
  discordRpc: boolean
  concurrentDownloads: number
  developerMode: boolean
  jvmArgs: string
}

export function SettingsPage() {
  const { t, setLocale } = useI18n()
  const { refreshTheme } = useTheme()
  const { accounts, activeAccount, loginMicrosoft } = useAccounts()
  const [section, setSection] = useState<(typeof SECTION_IDS)[number]>('general')
  const [settings, setSettings] = useState<SettingsState | null>(null)
  const [updateInfo, setUpdateInfo] = useState<UpdateCheckDto | null>(null)
  const [saved, setSaved] = useState(false)
  const [ownsCrimson, setOwnsCrimson] = useState(false)
  const [ownsNebula, setOwnsNebula] = useState(false)
  const [javaInstalls, setJavaInstalls] = useState<JavaInstallationDto[]>([])
  const [restartRequired, setRestartRequired] = useState(false)

  const load = useCallback(async () => {
    const s = await window.primeLauncher.settings.get()
    setSettings({
      language: s.language,
      closeOnLaunch: s.closeOnLaunch,
      autoUpdate: s.autoUpdate,
      theme: s.theme,
      backgroundNebula: s.backgroundNebula,
      hardwareAccel: s.hardwareAccel,
      defaultRamMb: s.defaultRamMb,
      defaultJavaPath: s.defaultJavaPath,
      performancePreset: s.performancePreset,
      analytics: s.analytics,
      discordRpc: s.discordRpc,
      concurrentDownloads: s.concurrentDownloads,
      developerMode: s.developerMode,
      jvmArgs: s.jvmArgs.join('\n')
    })
    const catalog = await window.primeLauncher.store.catalog()
    setOwnsCrimson(catalog.some((item: StoreItem) => item.id === 'theme-crimson' && item.owned))
    setOwnsNebula(catalog.some((item: StoreItem) => item.id === 'bg-nebula' && item.owned))
    setJavaInstalls(await window.primeLauncher.settings.listJava())
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  async function patch(partial: Partial<SettingsState>) {
    if (!settings) {
      return
    }
    const next = { ...settings, ...partial }
    setSettings(next)

    if (partial.language) {
      setLocale(partial.language)
    }
    if (partial.theme !== undefined || partial.backgroundNebula !== undefined) {
      void refreshTheme()
    }

    const result = (await window.primeLauncher.settings.update({
      language: next.language,
      closeOnLaunch: next.closeOnLaunch,
      autoUpdate: next.autoUpdate,
      theme: next.theme,
      backgroundNebula: next.backgroundNebula,
      hardwareAccel: next.hardwareAccel,
      defaultRamMb: next.defaultRamMb,
      defaultJavaPath: next.defaultJavaPath,
      performancePreset: next.performancePreset,
      analytics: next.analytics,
      discordRpc: next.discordRpc,
      concurrentDownloads: next.concurrentDownloads,
      developerMode: next.developerMode,
      jvmArgs: next.jvmArgs
        .split('\n')
        .map((l) => l.trim())
        .filter(Boolean)
    })) as SettingsUpdateDto

    setRestartRequired(Boolean(result.restartRequired))
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  async function handleCheckUpdate() {
    const info = await window.primeLauncher.update.check()
    setUpdateInfo(info)
  }

  if (!settings) {
    return null
  }

  return (
    <PageShell
      title={t('settings.title')}
      subtitle={t('settings.subtitle')}
      actions={saved ? <span className="text-caption">{t('common.saved')}</span> : undefined}
    >
      {restartRequired && (
        <p className="text-caption" style={{ marginBottom: 12, color: 'var(--prime-muted)' }}>
          {t('settings.restartRequired')}{' '}
          <button className="settings__select" style={{ cursor: 'pointer' }} onClick={() => void window.primeLauncher.app.restart()}>
            {t('settings.restartNow')}
          </button>
        </p>
      )}
      <div className="settings">
        <nav className="settings__nav">
          {SECTION_IDS.map((id) => (
            <button
              key={id}
              className={`settings__nav-item${section === id ? ' settings__nav-item--active' : ''}`}
              onClick={() => setSection(id)}
            >
              {t(`settings.sections.${id}`)}
            </button>
          ))}
        </nav>

        <div className="settings__panel">
          {section === 'general' && (
            <>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.language.label')}</div>
                  <div className="settings__hint">{t('settings.language.hint')}</div>
                </div>
                <select
                  className="settings__select"
                  value={settings.language}
                  onChange={(e) => void patch({ language: e.target.value as 'en' | 'fr' })}
                >
                  {LOCALES.map((lang) => (
                    <option key={lang.id} value={lang.id}>
                      {lang.label}
                    </option>
                  ))}
                </select>
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
                  <div className="settings__label">{t('settings.checkUpdates.label')}</div>
                  <div className="settings__hint">{t('settings.checkUpdates.hint')}</div>
                </div>
                <button className="settings__select" style={{ cursor: 'pointer' }} onClick={() => void handleCheckUpdate()}>
                  {t('common.checkNow')}
                </button>
              </div>
              {updateInfo && (
                <div style={{ padding: '0 16px 16px' }}>
                  <p className="text-caption" style={{ color: 'var(--prime-muted)', marginBottom: 8 }}>
                    {t('settings.updateNotes', {
                      current: updateInfo.current,
                      latest: updateInfo.latest,
                      notes: updateInfo.notes
                    })}
                  </p>
                  {updateInfo.updateAvailable && (
                    <button
                      className="settings__select"
                      style={{ cursor: 'pointer' }}
                      onClick={() =>
                        void window.primeLauncher.update.openRelease(
                          updateInfo.downloadUrl ?? updateInfo.releaseUrl
                        )
                      }
                    >
                      {t('common.downloadUpdate')}
                    </button>
                  )}
                </div>
              )}
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.autoUpdate.label')}</div>
                </div>
                <Toggle
                  checked={settings.autoUpdate}
                  onChange={(v) => void patch({ autoUpdate: v })}
                  label={t('settings.autoUpdate.toggle')}
                />
              </div>
            </>
          )}

          {section === 'appearance' && (
            <>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.theme.label')}</div>
                </div>
                <select
                  className="settings__select"
                  value={settings.theme}
                  onChange={(e) => void patch({ theme: e.target.value as 'prime-dark' | 'prime-crimson' })}
                >
                  <option value="prime-dark">{t('settings.theme.dark')}</option>
                  {ownsCrimson && <option value="prime-crimson">{t('settings.theme.crimson')}</option>}
                </select>
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
                  <div className="settings__label">{t('settings.hardwareAccel.label')}</div>
                </div>
                <Toggle
                  checked={settings.hardwareAccel}
                  onChange={(v) => void patch({ hardwareAccel: v })}
                  label={t('settings.hardwareAccel.toggle')}
                />
              </div>
            </>
          )}

          {section === 'minecraft' && (
            <>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.javaPath.label')}</div>
                  <div className="settings__hint">{t('settings.javaPath.hint')}</div>
                </div>
                <select
                  className="settings__select"
                  value={settings.defaultJavaPath ?? 'auto'}
                  onChange={(e) =>
                    void patch({
                      defaultJavaPath: e.target.value === 'auto' ? null : e.target.value
                    })
                  }
                >
                  <option value="auto">{t('common.automatic')}</option>
                  {javaInstalls.map((java) => (
                    <option key={java.path} value={java.path}>
                      {java.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.defaultRam.label')}</div>
                </div>
                <select
                  className="settings__select"
                  value={String(settings.defaultRamMb)}
                  onChange={(e) => void patch({ defaultRamMb: Number(e.target.value) })}
                >
                  <option value="2048">2048 MB</option>
                  <option value="4096">4096 MB</option>
                  <option value="6144">6144 MB</option>
                  <option value="8192">8192 MB</option>
                </select>
              </div>
            </>
          )}

          {section === 'performance' && (
            <div className="settings__row">
              <div>
                <div className="settings__label">{t('settings.performancePreset.label')}</div>
              </div>
              <select
                className="settings__select"
                value={settings.performancePreset}
                onChange={(e) => void patch({ performancePreset: e.target.value as PerformancePreset })}
              >
                <option value="low">{t('settings.performancePreset.low')}</option>
                <option value="balanced">{t('settings.performancePreset.balanced')}</option>
                <option value="performance">{t('settings.performancePreset.performance')}</option>
                <option value="ultra">{t('settings.performancePreset.ultra')}</option>
              </select>
            </div>
          )}

          {section === 'accounts' && (
            <>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.activeAccount.label')}</div>
                  <div className="settings__hint">
                    {activeAccount
                      ? `${activeAccount.username} (${activeAccount.type})`
                      : t('settings.activeAccount.none')}
                  </div>
                </div>
                <Link to="/accounts">
                  <button className="settings__select" style={{ cursor: 'pointer' }}>
                    {t('common.manage')} ({accounts.length})
                  </button>
                </Link>
              </div>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.microsoftAccount.label')}</div>
                </div>
                <button className="settings__select" style={{ cursor: 'pointer' }} onClick={() => void loginMicrosoft()}>
                  {t('common.signIn')}
                </button>
              </div>
            </>
          )}

          {section === 'privacy' && (
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
          )}

          {section === 'downloads' && (
            <div className="settings__row">
              <div>
                <div className="settings__label">{t('settings.concurrentDownloads.label')}</div>
              </div>
              <select
                className="settings__select"
                value={String(settings.concurrentDownloads)}
                onChange={(e) => void patch({ concurrentDownloads: Number(e.target.value) })}
              >
                <option value="1">1</option>
                <option value="3">3</option>
                <option value="5">5</option>
              </select>
            </div>
          )}

          {section === 'advanced' && (
            <>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.jvmArgs.label')}</div>
                  <div className="settings__hint">{t('settings.jvmArgs.hint')}</div>
                </div>
              </div>
              <textarea
                className="settings__select"
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
          )}
        </div>
      </div>
    </PageShell>
  )
}
