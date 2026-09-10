import { Suspense, lazy, useCallback, useEffect, useState, type ComponentType, type ReactNode } from 'react'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { useI18n } from '@renderer/context/I18nProvider'
import { useTheme } from '@renderer/context/ThemeProvider'
import { useToast } from '@renderer/design-system/components'
import type { StoreItem } from '@shared/content-types'
import type {
  UpdateProgressDto,
  UpdateStatusDto,
  JavaInstallationDto,
  SettingsUpdateDto
} from '@shared/ipc'
import { SettingsSkeleton } from './settings/SettingsSkeleton'
import {
  SECTION_IDS,
  type SettingsSectionId,
  type SettingsState
} from './settings/types'
import './SettingsPage.css'

function lazyNamed<T extends Record<string, ComponentType<any>>>(
  loader: () => Promise<T>,
  exportName: keyof T
) {
  return lazy(async () => {
    const mod = await loader()
    return { default: mod[exportName] as ComponentType<any> }
  })
}

const GeneralPanel = lazyNamed(() => import('./settings/panels/GeneralPanel'), 'GeneralPanel')
const AppearancePanel = lazyNamed(() => import('./settings/panels/AppearancePanel'), 'AppearancePanel')
const MinecraftPanel = lazyNamed(() => import('./settings/panels/MinecraftPanel'), 'MinecraftPanel')
const PerformancePanel = lazyNamed(() => import('./settings/panels/PerformancePanel'), 'PerformancePanel')
const AccountsPanel = lazyNamed(() => import('./settings/panels/AccountsPanel'), 'AccountsPanel')
const PrivacyPanel = lazyNamed(() => import('./settings/panels/PrivacyPanel'), 'PrivacyPanel')
const DownloadsPanel = lazyNamed(() => import('./settings/panels/DownloadsPanel'), 'DownloadsPanel')
const UpdatesPanel = lazyNamed(() => import('./settings/panels/UpdatesPanel'), 'UpdatesPanel')
const AdvancedPanel = lazyNamed(() => import('./settings/panels/AdvancedPanel'), 'AdvancedPanel')

function PanelFallback(): ReactNode {
  return (
    <div className="settings__panel" aria-busy="true">
      <div className="settings__skel-row" />
      <div className="settings__skel-row" />
    </div>
  )
}

/** After first paint — avoids janking route transitions with a burst of IPC. */
function afterFirstPaint(cb: () => void): () => void {
  let cancelled = false
  const run = () => {
    if (cancelled) return
    cb()
  }
  const ric = (window as Window & { requestIdleCallback?: (fn: () => void, opts?: { timeout: number }) => number })
    .requestIdleCallback
  if (typeof ric === 'function') {
    const id = ric(run, { timeout: 120 })
    return () => {
      cancelled = true
      window.cancelIdleCallback?.(id)
    }
  }
  const raf = window.requestAnimationFrame(() => {
    window.setTimeout(run, 0)
  })
  return () => {
    cancelled = true
    window.cancelAnimationFrame(raf)
  }
}

export function SettingsPage() {
  const { t, setLocale } = useI18n()
  const { refreshTheme, applyThemeId } = useTheme()
  const { toast } = useToast()
  const [section, setSection] = useState<SettingsSectionId>('general')
  const [settings, setSettings] = useState<SettingsState | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [updateInfo, setUpdateInfo] = useState<UpdateStatusDto | null>(null)
  const [updateBusy, setUpdateBusy] = useState<'launcher' | 'mod' | 'check' | null>(null)
  const [updateProgress, setUpdateProgress] = useState<UpdateProgressDto | null>(null)
  const [updateError, setUpdateError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)
  const [javaInstalls, setJavaInstalls] = useState<JavaInstallationDto[]>([])
  const [restartRequired, setRestartRequired] = useState(false)
  const [groqStatus, setGroqStatus] = useState<{
    hasKey: boolean
    maskedKey: string | null
    viaProxy: boolean
  }>({
    hasKey: false,
    maskedKey: null,
    viaProxy: false
  })

  const load = useCallback(async () => {
    try {
      const s = await window.primeLauncher.settings.get()
      setSettings({
        language: s.language,
        closeOnLaunch: s.closeOnLaunch,
        autoUpdate: s.autoUpdate,
        theme: s.theme,
        backgroundNebula: s.backgroundNebula,
        hardwareAccel: s.hardwareAccel,
        performanceMode: Boolean(s.performanceMode),
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
        gameDisplayMode: s.gameDisplayMode,
        wallpaperPath: s.wallpaperPath ?? null,
        accentColor: s.accentColor ?? null,
        uiSounds: s.uiSounds !== false
      })
      setLoadError(null)

      // Secondary IPC after core settings are on screen.
      void (async () => {
        try {
          const [keyStatus, javas] = await Promise.all([
            window.primeLauncher.ai.keyStatus(),
            window.primeLauncher.settings.listJava()
          ])
          setGroqStatus(keyStatus)
          setJavaInstalls(javas)
        } catch {
          // Non-fatal — panels degrade gracefully.
        }
      })()
    } catch (e) {
      setLoadError(e instanceof Error ? e.message : 'Failed to load settings')
    }
  }, [])

  useEffect(() => {
    return afterFirstPaint(() => {
      void load()
    })
  }, [load])

  async function patch(partial: Partial<SettingsState>) {
    if (!settings) {
      return
    }
    const clearedAccent =
      partial.theme !== undefined && partial.theme !== settings.theme
        ? { accentColor: null as string | null }
        : {}
    const next = { ...settings, ...partial, ...clearedAccent }
    setSettings(next)

    if (partial.language) {
      setLocale(partial.language)
    }

    // Apply theme chrome immediately — do not wait for IPC.
    if (partial.theme !== undefined) {
      applyThemeId(partial.theme)
    }
    if (partial.backgroundNebula !== undefined || partial.performanceMode !== undefined) {
      document.documentElement.dataset.background =
        (partial.backgroundNebula ?? next.backgroundNebula) &&
        !(partial.performanceMode ?? next.performanceMode)
          ? 'nebula'
          : 'default'
    }

    const result = (await window.primeLauncher.settings.update({
      language: next.language,
      closeOnLaunch: next.closeOnLaunch,
      autoUpdate: next.autoUpdate,
      theme: next.theme,
      backgroundNebula: next.backgroundNebula,
      hardwareAccel: next.hardwareAccel,
      performanceMode: next.performanceMode,
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
      gameDisplayMode: next.gameDisplayMode,
      wallpaperPath: next.wallpaperPath,
      accentColor: next.accentColor,
      uiSounds: next.uiSounds
    })) as SettingsUpdateDto

    if (
      partial.theme !== undefined ||
      partial.backgroundNebula !== undefined ||
      partial.wallpaperPath !== undefined ||
      partial.accentColor !== undefined ||
      clearedAccent.accentColor !== undefined ||
      partial.uiSounds !== undefined ||
      partial.performanceMode !== undefined
    ) {
      await refreshTheme()
    }

    setRestartRequired(Boolean(result.restartRequired))
    setSaved(true)
    toast(t('common.saved'), 'success')
    window.setTimeout(() => setSaved(false), 2000)
  }

  async function handleCheckUpdate(force = true) {
    setUpdateBusy('check')
    setUpdateError(null)
    try {
      const info = await window.primeLauncher.update.check(force)
      setUpdateInfo(info)
    } catch (e) {
      setUpdateError(e instanceof Error ? e.message : t('updates.errors.unknown'))
    } finally {
      setUpdateBusy(null)
    }
  }

  useEffect(() => {
    if (section === 'launcher' && !updateInfo && settings) {
      void handleCheckUpdate(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- only when entering launcher tab
  }, [section, settings])

  async function handleInstallUpdate(target: 'launcher' | 'mod') {
    setUpdateBusy(target)
    setUpdateError(null)
    setUpdateProgress(null)

    const unsub = window.primeLauncher.update.onProgress((payload) => {
      if (payload.target === target) {
        setUpdateProgress(payload)
      }
    })

    try {
      const result =
        target === 'launcher'
          ? await window.primeLauncher.update.installLauncher()
          : await window.primeLauncher.update.installMod()

      if (!result.ok) {
        const key = result.errorKey ? `updates.errors.${result.errorKey}` : null
        setUpdateError(key ? t(key) : (result.error ?? t('updates.errors.unknown')))
        return
      }

      if (target === 'mod') {
        await handleCheckUpdate(true)
      }
    } finally {
      unsub()
      setUpdateBusy(null)
    }
  }

  function markSaved() {
    setSaved(true)
    toast(t('common.saved'), 'success')
    window.setTimeout(() => setSaved(false), 2000)
  }

  function renderPanel() {
    if (!settings) {
      return null
    }
    switch (section) {
      case 'general':
        return <GeneralPanel settings={settings} patch={patch} />
      case 'appearance':
        return <AppearancePanel settings={settings} patch={patch} />
      case 'minecraft':
        return (
          <>
            <MinecraftPanel
              settings={settings}
              patch={patch}
              javaInstalls={javaInstalls}
              setJavaInstalls={setJavaInstalls}
            />
            <div className="settings__divider" />
            <p className="text-label">{t('settings.sections.performance')}</p>
            <PerformancePanel settings={settings} patch={patch} />
          </>
        )
      case 'launcher':
        return (
          <>
            <DownloadsPanel settings={settings} patch={patch} />
            <div className="settings__divider" />
            <p className="text-label">{t('settings.sections.updates')}</p>
            <UpdatesPanel
              updateInfo={updateInfo}
              updateBusy={updateBusy}
              updateProgress={updateProgress}
              updateError={updateError}
              onCheck={(force?: boolean) => void handleCheckUpdate(force)}
              onInstall={(target: 'launcher' | 'mod') => void handleInstallUpdate(target)}
            />
          </>
        )
      case 'accounts':
        return <AccountsPanel />
      case 'privacy':
        return <PrivacyPanel settings={settings} patch={patch} />
      case 'advanced':
        return (
          <AdvancedPanel
            settings={settings}
            patch={patch}
            groqStatus={groqStatus}
            setGroqStatus={setGroqStatus}
            onSaved={markSaved}
          />
        )
      default:
        return null
    }
  }

  return (
    <PageShell
      title={t('settings.title')}
      subtitle={t('settings.subtitle')}
      actions={saved ? <span className="settings__saved">{t('common.saved')}</span> : undefined}
    >
      {restartRequired && (
        <p className="settings__banner">
          {t('settings.restartRequired')}{' '}
          <button type="button" className="settings__link" onClick={() => void window.primeLauncher.app.restart()}>
            {t('settings.restartNow')}
          </button>
        </p>
      )}
      {loadError && <p className="settings__error">{loadError}</p>}
      {!settings ? (
        <SettingsSkeleton />
      ) : (
        <div className="settings">
          <nav className="settings__nav" aria-label={t('settings.title')}>
            {SECTION_IDS.map((id) => (
              <button
                key={id}
                type="button"
                className={`settings__nav-item${section === id ? ' settings__nav-item--active' : ''}`}
                onClick={() => setSection(id)}
              >
                {t(`settings.sections.${id}`)}
              </button>
            ))}
          </nav>

          <div className="settings__panel">
            <Suspense fallback={<PanelFallback />}>{renderPanel()}</Suspense>
          </div>
        </div>
      )}
    </PageShell>
  )
}
