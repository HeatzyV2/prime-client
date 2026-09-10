import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  ArrowLeft,
  FolderOpen,
  Play,
  Plus,
  RefreshCw,
  Square,
  Terminal,
  Trash2,
  Puzzle,
  Settings2,
  Globe2,
  Files
} from 'lucide-react'
import {
  HOST_EDITABLE_PROPERTIES,
  HOST_SOFTWARE_LABELS,
  type CreateHostServerDto,
  type HostConsoleLineDto,
  type HostEditablePropertyKey,
  type HostPluginHitDto,
  type HostServerRuntimeStatus,
  type HostServerView,
  type HostSoftware,
  type InstalledHostPluginDto,
  type HostWorldEntryDto
} from '@shared/host-types'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Button } from '@renderer/design-system/components'
import { useAccounts } from '@renderer/context/AccountProvider'
import { useActiveInstance } from '@renderer/hooks/useActiveInstance'
import { useI18n } from '@renderer/context/I18nProvider'
import { LoginModal } from '@renderer/components/LoginModal'
import './HostPage.css'

type TabId = 'console' | 'settings' | 'plugins' | 'worlds' | 'files'
type ViewMode = 'list' | 'create' | 'detail'

const SOFTWARE_OPTIONS: HostSoftware[] = ['paper', 'purpur', 'folia', 'leaf', 'canvas']

function statusVariant(status: HostServerRuntimeStatus): 'success' | 'red' | 'default' | 'prime' {
  switch (status) {
    case 'online':
      return 'success'
    case 'starting':
    case 'stopping':
      return 'prime'
    case 'crashed':
      return 'red'
    default:
      return 'default'
  }
}

function statusLabel(status: HostServerRuntimeStatus, t: (k: string) => string): string {
  return t(`host.status.${status}`)
}

export function HostPage() {
  const { t } = useI18n()
  const { activeAccount } = useAccounts()
  const { instance, refresh: refreshInstance } = useActiveInstance()
  const [servers, setServers] = useState<HostServerView[]>([])
  const [view, setView] = useState<ViewMode>('list')
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [tab, setTab] = useState<TabId>('console')
  const [message, setMessage] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [loginOpen, setLoginOpen] = useState(false)
  const [downloadPct, setDownloadPct] = useState<number | null>(null)
  const [consoleLines, setConsoleLines] = useState<HostConsoleLineDto[]>([])
  const [command, setCommand] = useState('')
  const consoleEndRef = useRef<HTMLDivElement>(null)

  // Create form
  const [createName, setCreateName] = useState('Mon serveur')
  const [createSoftware, setCreateSoftware] = useState<HostSoftware>('paper')
  const [createVersion, setCreateVersion] = useState('')
  const [createBuild, setCreateBuild] = useState('latest')
  const [createRam, setCreateRam] = useState(2048)
  const [createPort, setCreatePort] = useState(25565)
  const [createEula, setCreateEula] = useState(false)
  const [versions, setVersions] = useState<string[]>([])
  const [builds, setBuilds] = useState<{ id: string }[]>([])
  const [catalogError, setCatalogError] = useState<string | null>(null)

  // Detail extras
  const [propsMap, setPropsMap] = useState<Partial<Record<HostEditablePropertyKey, string>>>({})
  const [plugins, setPlugins] = useState<InstalledHostPluginDto[]>([])
  const [pluginQuery, setPluginQuery] = useState('')
  const [pluginHits, setPluginHits] = useState<HostPluginHitDto[]>([])
  const [worlds, setWorlds] = useState<HostWorldEntryDto[]>([])

  const selected = useMemo(
    () => servers.find((s) => s.id === selectedId) ?? null,
    [servers, selectedId]
  )

  const refreshList = useCallback(async () => {
    const list = await window.primeLauncher.host.list()
    setServers(list)
    return list
  }, [])

  useEffect(() => {
    void refreshList()
  }, [refreshList])

  useEffect(() => {
    const offConsole = window.primeLauncher.host.onConsole((line) => {
      if (selectedId && line.serverId !== selectedId) return
      setConsoleLines((prev) => [...prev.slice(-500), line])
    })
    const offStatus = window.primeLauncher.host.onStatus((ev) => {
      setServers((prev) =>
        prev.map((s) => (s.id === ev.serverId ? { ...s, status: ev.status } : s))
      )
    })
    const offDl = window.primeLauncher.host.onDownloadProgress((p) => {
      if (p.phase === 'download' || p.phase === 'start') {
        setDownloadPct(p.percent)
        setMessage(p.detail + (p.speed ? ` (${p.speed})` : ''))
      } else if (p.phase === 'done') {
        setDownloadPct(null)
        setMessage(p.detail)
        void refreshList()
      } else if (p.phase === 'error') {
        setDownloadPct(null)
        setMessage(p.detail)
      }
    })
    return () => {
      offConsole()
      offStatus()
      offDl()
    }
  }, [selectedId, refreshList])

  useEffect(() => {
    consoleEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [consoleLines])

  useEffect(() => {
    if (view !== 'create') return
    let cancelled = false
    setCatalogError(null)
    void (async () => {
      try {
        const list = await window.primeLauncher.host.listVersions(createSoftware)
        if (cancelled) return
        setVersions(list)
        const preferred = list.find((v) => v === '1.21.8' || v === '1.21.11') ?? list[0] ?? ''
        setCreateVersion(preferred)
      } catch (err) {
        if (!cancelled) {
          setVersions([])
          setCatalogError(err instanceof Error ? err.message : String(err))
        }
      }
    })()
    return () => {
      cancelled = true
    }
  }, [view, createSoftware])

  useEffect(() => {
    if (view !== 'create' || !createVersion) return
    let cancelled = false
    void (async () => {
      try {
        const list = await window.primeLauncher.host.listBuilds(createSoftware, createVersion)
        if (cancelled) return
        setBuilds(list)
        setCreateBuild('latest')
      } catch (err) {
        if (!cancelled) {
          setBuilds([])
          setCatalogError(err instanceof Error ? err.message : String(err))
        }
      }
    })()
    return () => {
      cancelled = true
    }
  }, [view, createSoftware, createVersion])

  const openDetail = async (id: string) => {
    setSelectedId(id)
    setView('detail')
    setTab('console')
    setConsoleLines([])
    setMessage(null)
    const server = await window.primeLauncher.host.get(id)
    if (server) {
      setServers((prev) => {
        const others = prev.filter((s) => s.id !== id)
        return [server, ...others]
      })
    }
  }

  const loadSettingsTab = async (id: string) => {
    const props = await window.primeLauncher.host.getProperties(id)
    setPropsMap(props?.editable ?? {})
  }

  const loadPluginsTab = async (id: string) => {
    setPlugins(await window.primeLauncher.host.listPlugins(id))
  }

  const loadWorldsTab = async (id: string) => {
    setWorlds(await window.primeLauncher.host.listWorlds(id))
  }

  useEffect(() => {
    if (view !== 'detail' || !selectedId) return
    if (tab === 'settings') void loadSettingsTab(selectedId)
    if (tab === 'plugins') void loadPluginsTab(selectedId)
    if (tab === 'worlds') void loadWorldsTab(selectedId)
  }, [view, selectedId, tab])

  const handleCreate = async () => {
    if (!createEula) {
      setMessage(t('host.eulaRequired'))
      return
    }
    setBusy(true)
    setMessage(null)
    const input: CreateHostServerDto = {
      name: createName,
      software: createSoftware,
      mcVersion: createVersion,
      build: createBuild,
      ramMb: createRam,
      port: createPort,
      acceptEula: true
    }
    const result = await window.primeLauncher.host.create(input)
    setBusy(false)
    if (!result.ok) {
      setMessage(result.error ?? t('host.createFailed'))
      return
    }
    await refreshList()
    if (result.server) {
      await openDetail(result.server.id)
    } else {
      setView('list')
    }
  }

  const handleStart = async () => {
    if (!selectedId) return
    setBusy(true)
    const result = await window.primeLauncher.host.start(selectedId)
    setBusy(false)
    if (!result.ok) setMessage(result.error ?? t('host.startFailed'))
    await refreshList()
  }

  const handleStop = async () => {
    if (!selectedId) return
    setBusy(true)
    await window.primeLauncher.host.stop(selectedId)
    setBusy(false)
    await refreshList()
  }

  const handleRestart = async () => {
    if (!selectedId) return
    setBusy(true)
    const result = await window.primeLauncher.host.restart(selectedId)
    setBusy(false)
    if (!result.ok) setMessage(result.error ?? t('host.startFailed'))
    await refreshList()
  }

  const handleDelete = async () => {
    if (!selectedId) return
    if (!window.confirm(t('host.confirmDelete'))) return
    setBusy(true)
    const result = await window.primeLauncher.host.remove(selectedId)
    setBusy(false)
    if (!result.ok) {
      setMessage(result.error ?? t('host.deleteFailed'))
      return
    }
    setSelectedId(null)
    setView('list')
    await refreshList()
  }

  const handleJoin = async () => {
    if (!selected) return
    if (!activeAccount) {
      setLoginOpen(true)
      return
    }
    await refreshInstance()
    const inst = instance ?? (await window.primeLauncher.instance.getDefault())
    if (!inst) {
      setMessage(t('host.noInstance'))
      return
    }
    setBusy(true)
    const address = `localhost:${selected.port}`
    const result = await window.primeLauncher.launch.game(inst.id, address)
    setBusy(false)
    setMessage(result.ok ? result.message : result.error ?? result.message)
  }

  const sendCommand = async () => {
    if (!selectedId || !command.trim()) return
    const result = await window.primeLauncher.host.sendCommand(selectedId, command)
    if (!result.ok) setMessage(result.error ?? t('host.commandFailed'))
    setCommand('')
  }

  const saveProperties = async () => {
    if (!selectedId) return
    setBusy(true)
    const result = await window.primeLauncher.host.updateProperties(selectedId, propsMap)
    setBusy(false)
    if (!result.ok) setMessage(result.error ?? t('host.saveFailed'))
    else setMessage(t('common.saved'))
    await refreshList()
  }

  const searchPlugins = async () => {
    if (!selected) return
    setBusy(true)
    try {
      const hits = await window.primeLauncher.host.searchPlugins(pluginQuery, undefined, selected.mcVersion)
      setPluginHits(hits)
      setMessage(null)
    } catch (err) {
      setPluginHits([])
      setMessage(err instanceof Error ? err.message : String(err))
    }
    setBusy(false)
  }

  const installPlugin = async (hit: HostPluginHitDto) => {
    if (!selectedId) return
    setBusy(true)
    const result = await window.primeLauncher.host.installPlugin(selectedId, hit.source, hit.id)
    setBusy(false)
    if (!result.ok) setMessage(result.error ?? t('host.pluginInstallFailed'))
    else {
      setMessage(t('host.pluginInstalled'))
      await loadPluginsTab(selectedId)
    }
  }

  if (view === 'create') {
    return (
      <PageShell
        title={t('host.createTitle')}
        subtitle={t('host.createSubtitle')}
        actions={
          <Button variant="ghost" onClick={() => setView('list')}>
            <ArrowLeft size={16} /> {t('host.back')}
          </Button>
        }
      >
        <div className="host-form">
          {catalogError && <p className="host-message host-message--error">{catalogError}</p>}
          {message && <p className="host-message">{message}</p>}
          {downloadPct !== null && (
            <div className="host-progress">
              <div className="host-progress__bar" style={{ width: `${downloadPct}%` }} />
            </div>
          )}

          <label className="host-field">
            <span>{t('host.field.name')}</span>
            <input value={createName} onChange={(e) => setCreateName(e.target.value)} />
          </label>

          <label className="host-field">
            <span>{t('host.field.software')}</span>
            <select
              value={createSoftware}
              onChange={(e) => setCreateSoftware(e.target.value as HostSoftware)}
            >
              {SOFTWARE_OPTIONS.map((s) => (
                <option key={s} value={s}>
                  {HOST_SOFTWARE_LABELS[s]}
                </option>
              ))}
            </select>
          </label>

          <label className="host-field">
            <span>{t('host.field.version')}</span>
            <select value={createVersion} onChange={(e) => setCreateVersion(e.target.value)}>
              {versions.map((v) => (
                <option key={v} value={v}>
                  {v}
                </option>
              ))}
            </select>
          </label>

          <label className="host-field">
            <span>{t('host.field.build')}</span>
            <select value={createBuild} onChange={(e) => setCreateBuild(e.target.value)}>
              <option value="latest">{t('host.latestBuild')}</option>
              {builds.map((b) => (
                <option key={b.id} value={b.id}>
                  #{b.id}
                </option>
              ))}
            </select>
          </label>

          <div className="host-form__row">
            <label className="host-field">
              <span>{t('host.field.ram')}</span>
              <input
                type="number"
                min={512}
                step={256}
                value={createRam}
                onChange={(e) => setCreateRam(Number(e.target.value))}
              />
            </label>
            <label className="host-field">
              <span>{t('host.field.port')}</span>
              <input
                type="number"
                min={1}
                max={65535}
                value={createPort}
                onChange={(e) => setCreatePort(Number(e.target.value))}
              />
            </label>
          </div>

          <label className="host-eula">
            <input
              type="checkbox"
              checked={createEula}
              onChange={(e) => setCreateEula(e.target.checked)}
            />
            <span>
              {t('host.eulaAccept')}{' '}
              <a href="https://aka.ms/MinecraftEULA" target="_blank" rel="noreferrer">
                Minecraft EULA
              </a>
            </span>
          </label>

          <Button variant="primary" disabled={busy || !createVersion} onClick={() => void handleCreate()}>
            <Plus size={16} /> {busy ? t('host.creating') : t('host.create')}
          </Button>
        </div>
      </PageShell>
    )
  }

  if (view === 'detail' && selected) {
    const running =
      selected.status === 'online' || selected.status === 'starting' || selected.status === 'stopping'
    return (
      <PageShell
        title={selected.name}
        subtitle={`${HOST_SOFTWARE_LABELS[selected.software]} ${selected.mcVersion} · build ${selected.build} · :${selected.port}`}
        actions={
          <div className="host-actions">
            <Button variant="ghost" onClick={() => setView('list')}>
              <ArrowLeft size={16} /> {t('host.back')}
            </Button>
            <Badge variant={statusVariant(selected.status)}>{statusLabel(selected.status, t)}</Badge>
          </div>
        }
      >
        {message && <p className="host-message">{message}</p>}

        <div className="host-toolbar">
          {!running ? (
            <Button variant="primary" disabled={busy} onClick={() => void handleStart()}>
              <Play size={16} /> {t('host.start')}
            </Button>
          ) : (
            <Button variant="danger" disabled={busy} onClick={() => void handleStop()}>
              <Square size={16} /> {t('host.stop')}
            </Button>
          )}
          <Button variant="ghost" disabled={busy} onClick={() => void handleRestart()}>
            <RefreshCw size={16} /> {t('host.restart')}
          </Button>
          <Button variant="primary" disabled={busy} onClick={() => void handleJoin()}>
            <Play size={16} /> {t('common.join')}
          </Button>
          <Button variant="ghost" onClick={() => void window.primeLauncher.host.openFolder(selected.id)}>
            <FolderOpen size={16} /> {t('host.openFolder')}
          </Button>
            <Button variant="ghost" disabled={busy || running} onClick={() => void handleDelete()}>
            <Trash2 size={16} /> {t('host.delete')}
          </Button>
        </div>

        {!selected.eulaAccepted && (
          <div className="host-eula-banner">
            <p>{t('host.eulaBanner')}</p>
            <Button
              variant="primary"
              onClick={async () => {
                await window.primeLauncher.host.acceptEula(selected.id)
                await refreshList()
              }}
            >
              {t('host.acceptEula')}
            </Button>
          </div>
        )}

        <div className="host-tabs" role="tablist">
          {(
            [
              ['console', Terminal, t('host.tabs.console')],
              ['settings', Settings2, t('host.tabs.settings')],
              ['plugins', Puzzle, t('host.tabs.plugins')],
              ['worlds', Globe2, t('host.tabs.worlds')],
              ['files', Files, t('host.tabs.files')]
            ] as const
          ).map(([id, Icon, label]) => (
            <button
              key={id}
              type="button"
              role="tab"
              className={`host-tabs__btn${tab === id ? ' is-active' : ''}`}
              onClick={() => setTab(id)}
            >
              <Icon size={15} /> {label}
            </button>
          ))}
        </div>

        {tab === 'console' && (
          <div className="host-console">
            <div className="host-console__log">
              {consoleLines.length === 0 && (
                <p className="host-console__empty">{t('host.consoleEmpty')}</p>
              )}
              {consoleLines.map((line, i) => (
                <div key={`${line.timestamp}-${i}`} className={`host-console__line host-console__line--${line.stream}`}>
                  {line.line}
                </div>
              ))}
              <div ref={consoleEndRef} />
            </div>
            <form
              className="host-console__input"
              onSubmit={(e) => {
                e.preventDefault()
                void sendCommand()
              }}
            >
              <input
                value={command}
                onChange={(e) => setCommand(e.target.value)}
                placeholder={t('host.commandPlaceholder')}
                disabled={!running}
              />
              <Button type="submit" variant="primary" disabled={!running || !command.trim()}>
                {t('host.send')}
              </Button>
            </form>
          </div>
        )}

        {tab === 'settings' && (
          <div className="host-settings">
            <div className="host-settings__grid">
              {HOST_EDITABLE_PROPERTIES.map((key) => (
                <label key={key} className="host-field">
                  <span>{key}</span>
                  <input
                    value={propsMap[key] ?? ''}
                    onChange={(e) => setPropsMap((prev) => ({ ...prev, [key]: e.target.value }))}
                    disabled={running}
                  />
                </label>
              ))}
            </div>
            <Button variant="primary" disabled={busy || running} onClick={() => void saveProperties()}>
              {t('host.save')}
            </Button>
          </div>
        )}

        {tab === 'plugins' && (
          <div className="host-plugins">
            <div className="host-plugins__search">
              <input
                value={pluginQuery}
                onChange={(e) => setPluginQuery(e.target.value)}
                placeholder={t('host.pluginSearch')}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') void searchPlugins()
                }}
              />
              <Button variant="primary" disabled={busy || !pluginQuery.trim()} onClick={() => void searchPlugins()}>
                {t('host.search')}
              </Button>
            </div>

            {pluginHits.length > 0 && (
              <div className="host-plugins__hits">
                {pluginHits.map((hit) => (
                  <div key={`${hit.source}-${hit.id}`} className="host-plugin-hit">
                    <div>
                      <strong>{hit.name}</strong>
                      <span className="host-plugin-hit__meta">
                        {hit.source} · {hit.downloads.toLocaleString()}
                      </span>
                      <p>{hit.description}</p>
                    </div>
                    <Button
                      variant="primary"
                      disabled={busy || running}
                      onClick={() => void installPlugin(hit)}
                    >
                      {t('host.install')}
                    </Button>
                  </div>
                ))}
              </div>
            )}

            <h3>{t('host.installedPlugins')}</h3>
            {plugins.length === 0 ? (
              <p className="host-message">{t('host.noPlugins')}</p>
            ) : (
              <ul className="host-plugins__list">
                {plugins.map((p) => (
                  <li key={p.fileName}>
                    <span className={!p.enabled ? 'is-disabled' : undefined}>{p.fileName}</span>
                    <div className="host-plugins__actions">
                      <Button
                        variant="ghost"
                        disabled={busy}
                        onClick={async () => {
                          await window.primeLauncher.host.setPluginEnabled(
                            selected.id,
                            p.fileName,
                            !p.enabled
                          )
                          await loadPluginsTab(selected.id)
                        }}
                      >
                        {p.enabled ? t('host.disable') : t('host.enable')}
                      </Button>
                      <Button
                        variant="ghost"
                        disabled={busy}
                        onClick={async () => {
                          await window.primeLauncher.host.removePlugin(selected.id, p.fileName)
                          await loadPluginsTab(selected.id)
                        }}
                      >
                        <Trash2 size={14} />
                      </Button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}

        {tab === 'worlds' && (
          <div className="host-worlds">
            {worlds.length === 0 ? (
              <p className="host-message">{t('host.noWorlds')}</p>
            ) : (
              <ul className="host-worlds__list">
                {worlds.map((w) => (
                  <li key={w.name}>
                    <strong>{w.name}</strong>
                    <span>{w.isLevel ? t('host.worldReady') : t('host.worldFolder')}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}

        {tab === 'files' && (
          <div className="host-files">
            <p className="host-message">{selected.dir}</p>
            <Button variant="primary" onClick={() => void window.primeLauncher.host.openFolder(selected.id)}>
              <FolderOpen size={16} /> {t('host.openFolder')}
            </Button>
          </div>
        )}

        {loginOpen && <LoginModal onClose={() => setLoginOpen(false)} />}
      </PageShell>
    )
  }

  return (
    <PageShell
      title={t('host.title')}
      subtitle={t('host.subtitle')}
      actions={
        <Button variant="primary" onClick={() => setView('create')}>
          <Plus size={16} /> {t('host.create')}
        </Button>
      }
    >
      {message && <p className="host-message">{message}</p>}
      {servers.length === 0 ? (
        <div className="host-empty">
          <p>{t('host.empty')}</p>
          <Button variant="primary" onClick={() => setView('create')}>
            <Plus size={16} /> {t('host.create')}
          </Button>
        </div>
      ) : (
        <div className="host-grid">
          {servers.map((s) => (
            <button
              key={s.id}
              type="button"
              className="host-card"
              onClick={() => void openDetail(s.id)}
            >
              <div className="host-card__top">
                <h3>{s.name}</h3>
                <Badge variant={statusVariant(s.status)}>{statusLabel(s.status, t)}</Badge>
              </div>
              <p>
                {HOST_SOFTWARE_LABELS[s.software]} {s.mcVersion}
              </p>
              <p className="host-card__meta">
                :{s.port} · {s.ramMb} MB · build {s.build}
              </p>
            </button>
          ))}
        </div>
      )}
    </PageShell>
  )
}
