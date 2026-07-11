import { useCallback, useEffect, useState } from 'react'
import { AnimatePresence } from 'framer-motion'
import { Plus, Play, Box, FolderOpen, Copy, Trash2, Star } from 'lucide-react'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Button, Card } from '@renderer/design-system/components'
import { InstanceModal, type InstancePreset } from '@renderer/components/InstanceModal'
import { useAccounts } from '@renderer/context/AccountProvider'
import type { GameInstance } from '@shared/types'

export function InstancesPage() {
  const { launch, activeAccount } = useAccounts()
  const [instances, setInstances] = useState<GameInstance[]>([])
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState<{ mode: 'create' | 'edit'; preset?: InstancePreset; instance?: GameInstance } | null>(
    null
  )
  const [busyId, setBusyId] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    const list = await window.primeLauncher.instance.list()
    setInstances(list)
    setLoading(false)
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  async function handlePlay(inst: GameInstance) {
    if (!activeAccount) {
      return
    }
    setBusyId(inst.id)
    await window.primeLauncher.profile.setInstance(inst.id)
    await launch(inst.id)
    setBusyId(null)
    await refresh()
  }

  async function handleSetDefault(id: string) {
    await window.primeLauncher.instance.setDefault(id)
    await refresh()
  }

  async function handleDuplicate(id: string) {
    await window.primeLauncher.instance.duplicate(id)
    await refresh()
  }

  async function handleDelete(inst: GameInstance) {
    if (!confirm(`Delete "${inst.name}"? Game files can be kept or removed.`)) {
      return
    }
    const deleteFiles = confirm('Also delete saves and mods on disk?')
    const result = await window.primeLauncher.instance.remove(inst.id, deleteFiles)
    if (!result.ok) {
      alert(result.error ?? 'Could not delete instance.')
      return
    }
    await refresh()
  }

  return (
    <PageShell
      title="Instances"
      subtitle="Create and manage local Minecraft installations — versions, RAM, mods, per folder."
      actions={
        <div style={{ display: 'flex', gap: 8 }}>
          <Button variant="secondary" size="sm" onClick={() => setModal({ mode: 'create', preset: 'vanilla' })}>
            Vanilla
          </Button>
          <Button variant="secondary" size="sm" onClick={() => setModal({ mode: 'create', preset: 'fabric' })}>
            Fabric
          </Button>
          <Button variant="primary" icon={<Plus size={16} />} onClick={() => setModal({ mode: 'create', preset: 'prime' })}>
            Prime Client
          </Button>
        </div>
      }
    >
      {loading ? (
        <p className="text-caption">Loading instances…</p>
      ) : (
        <div className="page-grid page-grid--3">
          {instances.map((inst) => (
            <Card key={inst.id} glow={inst.isDefault} hover>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                <div className="list-row__icon">
                  <Box size={20} />
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div className="list-row__title" style={{ fontSize: '1rem' }}>
                    {inst.name}
                  </div>
                  <div className="list-row__desc">
                    {inst.minecraftVersion} · {inst.loader}
                    {inst.includePrimeMod ? ' · Prime' : ''}
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap' }}>
                <Badge variant="default">{inst.ramMb} MB RAM</Badge>
                <Badge variant="default">{inst.modCount} mods</Badge>
                {inst.isDefault && <Badge variant="prime">Default</Badge>}
              </div>

              {inst.lastPlayed && (
                <p className="text-caption" style={{ marginBottom: 12 }}>
                  Last played {new Date(inst.lastPlayed).toLocaleDateString()}
                </p>
              )}

              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                <Button
                  variant="primary"
                  size="sm"
                  icon={<Play size={14} />}
                  disabled={!activeAccount || busyId === inst.id}
                  onClick={() => void handlePlay(inst)}
                >
                  Play
                </Button>
                <Button variant="secondary" size="sm" onClick={() => setModal({ mode: 'edit', instance: inst })}>
                  Configure
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  icon={<FolderOpen size={14} />}
                  onClick={() => void window.primeLauncher.instance.openFolder(inst.id)}
                >
                  Folder
                </Button>
                {!inst.isDefault && (
                  <Button
                    variant="ghost"
                    size="sm"
                    icon={<Star size={14} />}
                    onClick={() => void handleSetDefault(inst.id)}
                  >
                    Default
                  </Button>
                )}
                <Button
                  variant="ghost"
                  size="sm"
                  icon={<Copy size={14} />}
                  onClick={() => void handleDuplicate(inst.id)}
                >
                  Duplicate
                </Button>
                {instances.length > 1 && (
                  <Button
                    variant="ghost"
                    size="sm"
                    icon={<Trash2 size={14} />}
                    onClick={() => void handleDelete(inst)}
                  >
                    Delete
                  </Button>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}

      <AnimatePresence>
        {modal && (
          <InstanceModal
            mode={modal.mode}
            preset={modal.preset}
            instance={modal.instance}
            onClose={() => setModal(null)}
            onSaved={() => void refresh()}
          />
        )}
      </AnimatePresence>
    </PageShell>
  )
}
