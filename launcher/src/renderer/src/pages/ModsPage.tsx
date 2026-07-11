import { useCallback, useEffect, useMemo, useState } from 'react'
import { AnimatePresence } from 'framer-motion'
import { Download, FolderOpen, Puzzle, Trash2, Upload } from 'lucide-react'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Button, SearchInput, Tabs, Toggle } from '@renderer/design-system/components'
import { ModrinthBrowseModal } from '@renderer/components/ModrinthBrowseModal'
import { useActiveInstance } from '@renderer/hooks/useActiveInstance'
import type { ModEntry } from '@shared/content-types'

export function ModsPage() {
  const { instance, instanceId, loading: instanceLoading, refresh: refreshInstance } = useActiveInstance()
  const [mods, setMods] = useState<ModEntry[]>([])
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('all')
  const [showBrowse, setShowBrowse] = useState(false)
  const [busy, setBusy] = useState(false)

  const refresh = useCallback(async () => {
    if (!instanceId) {
      return
    }
    const list = await window.primeLauncher.content.listMods(instanceId)
    setMods(list)
  }, [instanceId])

  useEffect(() => {
    void refresh()
  }, [refresh])

  const filtered = useMemo(() => {
    return mods.filter((m) => {
      const matchSearch = m.name.toLowerCase().includes(search.toLowerCase())
      const matchFilter =
        filter === 'all' ||
        (filter === 'enabled' && m.enabled) ||
        (filter === 'disabled' && !m.enabled)
      return matchSearch && matchFilter
    })
  }, [mods, search, filter])

  async function handleToggle(mod: ModEntry, enabled: boolean) {
    if (!instanceId) {
      return
    }
    await window.primeLauncher.content.setModEnabled(mod.fileName, enabled, instanceId)
    await refresh()
    await refreshInstance()
  }

  async function handleImport() {
    setBusy(true)
    const result = await window.primeLauncher.content.importMod(instanceId ?? undefined)
    setBusy(false)
    if (result.ok) {
      await refresh()
      await refreshInstance()
    } else if (result.error !== 'Cancelled.') {
      alert(result.error)
    }
  }

  async function handleRemove(mod: ModEntry) {
    if (!instanceId || !confirm(`Remove ${mod.name}?`)) {
      return
    }
    await window.primeLauncher.content.removeMod(mod.fileName, instanceId)
    await refresh()
    await refreshInstance()
  }

  return (
    <PageShell
      title="Mod Manager"
      subtitle={
        instance
          ? `Mods for ${instance.name} (${instance.minecraftVersion}) — local folder + Modrinth.`
          : 'Loading instance…'
      }
      actions={
        <div style={{ display: 'flex', gap: 8 }}>
          <Button variant="secondary" icon={<Upload size={16} />} disabled={busy} onClick={() => void handleImport()}>
            Import .jar
          </Button>
          <Button variant="primary" icon={<Download size={16} />} onClick={() => setShowBrowse(true)}>
            Browse Modrinth
          </Button>
        </div>
      }
    >
      <div className="page-toolbar">
        <SearchInput value={search} onChange={setSearch} placeholder="Search mods..." />
        <Tabs
          tabs={[
            { id: 'all', label: 'All' },
            { id: 'enabled', label: 'Enabled' },
            { id: 'disabled', label: 'Disabled' }
          ]}
          active={filter}
          onChange={setFilter}
        />
        {instanceId && (
          <Button
            variant="ghost"
            size="sm"
            icon={<FolderOpen size={14} />}
            onClick={() => void window.primeLauncher.instance.openFolder(instanceId)}
          >
            Open folder
          </Button>
        )}
      </div>

      {instanceLoading ? (
        <p className="text-caption">Loading…</p>
      ) : filtered.length === 0 ? (
        <p className="text-caption">No mods in this instance. Import a .jar or browse Modrinth.</p>
      ) : (
        <div className="page-list">
          {filtered.map((mod) => (
            <div key={mod.id} className="list-row">
              <div className="list-row__icon">
                <Puzzle size={18} />
              </div>
              <div className="list-row__body">
                <div className="list-row__title">{mod.name}</div>
                <div className="list-row__desc">{mod.description}</div>
              </div>
              <div className="list-row__meta">
                <Badge variant="default">{mod.source}</Badge>
                <span className="text-mono" style={{ color: 'var(--prime-muted)' }}>
                  v{mod.version}
                </span>
                <Toggle
                  checked={mod.enabled}
                  label={`Toggle ${mod.name}`}
                  onChange={(enabled) => void handleToggle(mod, enabled)}
                />
                <Button variant="ghost" size="sm" icon={<Trash2 size={14} />} onClick={() => void handleRemove(mod)} />
              </div>
            </div>
          ))}
        </div>
      )}

      <AnimatePresence>
        {showBrowse && (
          <ModrinthBrowseModal
            type="mod"
            instanceId={instanceId}
            onClose={() => setShowBrowse(false)}
            onInstalled={() => {
              void refresh()
              void refreshInstance()
            }}
          />
        )}
      </AnimatePresence>
    </PageShell>
  )
}
