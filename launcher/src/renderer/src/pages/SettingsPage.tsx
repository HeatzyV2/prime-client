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
import type {
  UpdateProgressDto,
  UpdateStatusDto,
  JavaInstallationDto,
  SettingsUpdateDto,
  PrimeThemeId
} from '@shared/ipc'
import './SettingsPage.css'

const SECTION_IDS = [
  'general',
  'appearance',
  'minecraft',
  'performance',
  'accounts',
  'privacy',
  'downloads',
  'updates',
  'advanced'
] as const

interface SettingsState {
  language: 'en' | 'fr'
  closeOnLaunch: boolean
  autoUpdate: boolean
  theme: PrimeThemeId
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
  gameWidth: number
  gameHeight: number
  gameDisplayMode: 'windowed' | 'borderless' | 'fullscreen'
}

export function SettingsPage() {
  const { t, setLocale } = useI18n()
  const { refreshTheme } = useTheme()
  const { accounts, activeAccount, loginMicrosoft } = useAccounts()
  const [section, setSection] = useState<(typeof SECTION_IDS)[number]>('general')
  const [settings, setSettings] = useState<SettingsState | null>(null)
  const [updateInfo, setUpdateInfo] = useState<UpdateStatusDto | null>(null)
  const [updateBusy, setUpdateBusy] = useState<'launcher' | 'mod' | 'check' | null>(null)
  const [updateProgress, setUpdateProgress] = useState<UpdateProgressDto | null>(null)
  const [updateError, setUpdateError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)
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
      jvmArgs: s.jvmArgs.join('\n'),
      gameWidth: s.gameWidth,
      gameHeight: s.gameHeight,
      gameDisplayMode: s.gameDisplayMode
    })
    const catalog = await window.primeLauncher.store.catalog()
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
        .filter(Boolean),
      gameWidth: next.gameWidth,
      gameHeight: next.gameHeight,
      gameDisplayMode: next.gameDisplayMode
    })) as SettingsUpdateDto

    setRestartRequired(Boolean(result.restartRequired))
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  async function handleCheckUpdate(force = true) {
    setUpdateBusy('check')
    setUpdateError(null)
    const info = await window.primeLauncher.update.check(force)
    setUpdateInfo(info)
    setUpdateBusy(null)
  }

  useEffect(() => {
    if (section === 'updates' && !updateInfo) {
      void handleCheckUpdate(false)
    }
  }, [section])

  async function handleInstallUpdate(target: 'launcher' | 'mod') {
    setUpdateBusy(target)
    setUpdateError(null)
    setUpdateProgress(null)

    const unsub = window.primeLauncher.update.onProgress((payload) => {
      if (payload.target === target) {
        setUpdateProgress(payload)
      }
    })

    const result =
      target === 'launcher'
        ? await window.primeLauncher.update.installLauncher()
        : await window.primeLauncher.update.installMod()

    unsub()
    setUpdateBusy(null)

    if (!result.ok) {
      const key = result.errorKey ? `updates.errors.${result.errorKey}` : null
      setUpdateError(key ? t(key) : (result.error ?? t('updates.errors.unknown')))
      return
    }

    if (target === 'mod') {
      await handleCheckUpdate(true)
    }
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
                  onChange={(e) => void patch({ theme: e.target.value as PrimeThemeId })}
                >
                  <option value="prime-crimson">{t('settings.theme.crimson')}</option>
                  <option value="prime-midnight">{t('settings.theme.midnight')}</option>
                  <option value="prime-aurora">{t('settings.theme.aurora')}</option>
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
                <div className="settings__java-picker">
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
                  <button
                    className="settings__select settings__java-browse"
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
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.gameResolution.label')}</div>
                  <div className="settings__hint">{t('settings.gameResolution.hint')}</div>
                </div>
                <div className="settings__resolution">
                  <input
                    type="number"
                    className="settings__select settings__resolution-input"
                    min={320}
                    max={7680}
                    value={settings.gameWidth}
                    onChange={(e) => void patch({ gameWidth: Number(e.target.value) || 854 })}
                  />
                  <span className="settings__resolution-sep">×</span>
                  <input
                    type="number"
                    className="settings__select settings__resolution-input"
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
                <select
                  className="settings__select"
                  value={settings.gameDisplayMode}
                  onChange={(e) =>
                    void patch({
                      gameDisplayMode: e.target.value as SettingsState['gameDisplayMode']
                    })
                  }
                >
                  <option value="windowed">{t('settings.gameDisplayMode.windowed')}</option>
                  <option value="borderless">{t('settings.gameDisplayMode.borderless')}</option>
                  <option value="fullscreen">{t('settings.gameDisplayMode.fullscreen')}</option>
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

          {section === 'updates' && (
            <>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.checkUpdates.label')}</div>
                  <div className="settings__hint">{t('settings.checkUpdates.hint')}</div>
                </div>
                <button
                  className="settings__select"
                  style={{ cursor: 'pointer' }}
                  disabled={updateBusy !== null}
                  onClick={() => void handleCheckUpdate(true)}
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
                        className="settings__select"
                        style={{ cursor: 'pointer' }}
                        disabled={updateBusy !== null}
                        onClick={() => void handleInstallUpdate('launcher')}
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
                        className="settings__select"
                        style={{ cursor: 'pointer' }}
                        disabled={updateBusy !== null}
                        onClick={() => void handleInstallUpdate('mod')}
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
