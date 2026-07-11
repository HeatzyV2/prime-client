import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Toggle } from '@renderer/design-system/components'
import { useAccounts } from '@renderer/context/AccountProvider'
import type { PerformancePreset } from '@shared/content-types'
import type { UpdateCheckDto } from '@shared/ipc'
import './SettingsPage.css'

const SECTIONS = [
  { id: 'general', label: 'General' },
  { id: 'appearance', label: 'Appearance' },
  { id: 'minecraft', label: 'Minecraft' },
  { id: 'performance', label: 'Performance' },
  { id: 'accounts', label: 'Accounts' },
  { id: 'privacy', label: 'Privacy' },
  { id: 'downloads', label: 'Downloads' },
  { id: 'advanced', label: 'Advanced' }
]

interface SettingsState {
  language: 'en' | 'fr'
  closeOnLaunch: boolean
  autoUpdate: boolean
  theme: 'prime-dark' | 'prime-crimson'
  hardwareAccel: boolean
  defaultRamMb: number
  performancePreset: PerformancePreset
  analytics: boolean
  discordRpc: boolean
  concurrentDownloads: number
  developerMode: boolean
  jvmArgs: string
}

export function SettingsPage() {
  const { accounts, activeAccount, loginMicrosoft } = useAccounts()
  const [section, setSection] = useState('general')
  const [settings, setSettings] = useState<SettingsState | null>(null)
  const [updateInfo, setUpdateInfo] = useState<UpdateCheckDto | null>(null)
  const [saved, setSaved] = useState(false)

  const load = useCallback(async () => {
    const s = await window.primeLauncher.settings.get()
    setSettings({
      language: s.language,
      closeOnLaunch: s.closeOnLaunch,
      autoUpdate: s.autoUpdate,
      theme: s.theme,
      hardwareAccel: s.hardwareAccel,
      defaultRamMb: s.defaultRamMb,
      performancePreset: s.performancePreset,
      analytics: s.analytics,
      discordRpc: s.discordRpc,
      concurrentDownloads: s.concurrentDownloads,
      developerMode: s.developerMode,
      jvmArgs: s.jvmArgs.join('\n')
    })
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
    await window.primeLauncher.settings.update({
      language: next.language,
      closeOnLaunch: next.closeOnLaunch,
      autoUpdate: next.autoUpdate,
      theme: next.theme,
      hardwareAccel: next.hardwareAccel,
      defaultRamMb: next.defaultRamMb,
      performancePreset: next.performancePreset,
      analytics: next.analytics,
      discordRpc: next.discordRpc,
      concurrentDownloads: next.concurrentDownloads,
      developerMode: next.developerMode,
      jvmArgs: next.jvmArgs
        .split('\n')
        .map((l) => l.trim())
        .filter(Boolean)
    })
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
      title="Settings"
      subtitle="Saved to AppData — no account or cloud required."
      actions={saved ? <span className="text-caption">Saved</span> : undefined}
    >
      <div className="settings">
        <nav className="settings__nav">
          {SECTIONS.map((s) => (
            <button
              key={s.id}
              className={`settings__nav-item${section === s.id ? ' settings__nav-item--active' : ''}`}
              onClick={() => setSection(s.id)}
            >
              {s.label}
            </button>
          ))}
        </nav>

        <div className="settings__panel">
          {section === 'general' && (
            <>
              <div className="settings__row">
                <div>
                  <div className="settings__label">Language</div>
                  <div className="settings__hint">Launcher display language</div>
                </div>
                <select
                  className="settings__select"
                  value={settings.language}
                  onChange={(e) => void patch({ language: e.target.value as 'en' | 'fr' })}
                >
                  <option value="en">English</option>
                  <option value="fr">Français</option>
                </select>
              </div>
              <div className="settings__row">
                <div>
                  <div className="settings__label">Close on game launch</div>
                  <div className="settings__hint">Minimize launcher when Minecraft starts</div>
                </div>
                <Toggle
                  checked={settings.closeOnLaunch}
                  onChange={(v) => void patch({ closeOnLaunch: v })}
                  label="Close on launch"
                />
              </div>
              <div className="settings__row">
                <div>
                  <div className="settings__label">Check for updates</div>
                  <div className="settings__hint">Compare with latest GitHub Release</div>
                </div>
                <button className="settings__select" style={{ cursor: 'pointer' }} onClick={() => void handleCheckUpdate()}>
                  Check now
                </button>
              </div>
              {updateInfo && (
                <div style={{ padding: '0 16px 16px' }}>
                  <p className="text-caption" style={{ color: 'var(--prime-muted)', marginBottom: 8 }}>
                    v{updateInfo.current} → latest v{updateInfo.latest} — {updateInfo.notes}
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
                      Download update
                    </button>
                  )}
                </div>
              )}
              <div className="settings__row">
                <div>
                  <div className="settings__label">Auto-update launcher</div>
                </div>
                <Toggle checked={settings.autoUpdate} onChange={(v) => void patch({ autoUpdate: v })} label="Auto update" />
              </div>
            </>
          )}

          {section === 'appearance' && (
            <>
              <div className="settings__row">
                <div>
                  <div className="settings__label">Theme</div>
                </div>
                <select
                  className="settings__select"
                  value={settings.theme}
                  onChange={(e) => void patch({ theme: e.target.value as 'prime-dark' | 'prime-crimson' })}
                >
                  <option value="prime-dark">Prime Dark</option>
                  <option value="prime-crimson">Crimson (Store unlock)</option>
                </select>
              </div>
              <div className="settings__row">
                <div>
                  <div className="settings__label">Hardware acceleration</div>
                </div>
                <Toggle
                  checked={settings.hardwareAccel}
                  onChange={(v) => void patch({ hardwareAccel: v })}
                  label="Hardware acceleration"
                />
              </div>
            </>
          )}

          {section === 'minecraft' && (
            <>
              <div className="settings__row">
                <div>
                  <div className="settings__label">Default Java path</div>
                  <div className="settings__hint">Auto-detected JDK 21+ at launch</div>
                </div>
                <select className="settings__select" defaultValue="auto" disabled>
                  <option value="auto">Automatic</option>
                </select>
              </div>
              <div className="settings__row">
                <div>
                  <div className="settings__label">Default RAM allocation</div>
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
                <div className="settings__label">Performance preset on launch</div>
              </div>
              <select
                className="settings__select"
                value={settings.performancePreset}
                onChange={(e) => void patch({ performancePreset: e.target.value as PerformancePreset })}
              >
                <option value="low">Low PC</option>
                <option value="balanced">Balanced</option>
                <option value="performance">Performance</option>
                <option value="ultra">Ultra</option>
              </select>
            </div>
          )}

          {section === 'accounts' && (
            <>
              <div className="settings__row">
                <div>
                  <div className="settings__label">Active account</div>
                  <div className="settings__hint">
                    {activeAccount
                      ? `${activeAccount.username} (${activeAccount.type})`
                      : 'No account — add one to play'}
                  </div>
                </div>
                <Link to="/accounts">
                  <button className="settings__select" style={{ cursor: 'pointer' }}>
                    Manage ({accounts.length})
                  </button>
                </Link>
              </div>
              <div className="settings__row">
                <div>
                  <div className="settings__label">Microsoft account</div>
                </div>
                <button className="settings__select" style={{ cursor: 'pointer' }} onClick={() => void loginMicrosoft()}>
                  Sign in
                </button>
              </div>
            </>
          )}

          {section === 'privacy' && (
            <>
              <div className="settings__row">
                <div>
                  <div className="settings__label">Analytics</div>
                  <div className="settings__hint">Disabled by default — nothing is sent without a server</div>
                </div>
                <Toggle checked={settings.analytics} onChange={(v) => void patch({ analytics: v })} label="Analytics" />
              </div>
              <div className="settings__row">
                <div>
                  <div className="settings__label">Discord Rich Presence</div>
                  <div className="settings__hint">Handled by Prime Client mod in-game</div>
                </div>
                <Toggle checked={settings.discordRpc} onChange={(v) => void patch({ discordRpc: v })} label="Discord RPC" />
              </div>
            </>
          )}

          {section === 'downloads' && (
            <div className="settings__row">
              <div>
                <div className="settings__label">Concurrent downloads</div>
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
                  <div className="settings__label">JVM arguments</div>
                  <div className="settings__hint">One per line — applied via Performance presets too</div>
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
                  <div className="settings__label">Developer mode</div>
                </div>
                <Toggle
                  checked={settings.developerMode}
                  onChange={(v) => void patch({ developerMode: v })}
                  label="Developer mode"
                />
              </div>
            </>
          )}
        </div>
      </div>
    </PageShell>
  )
}
