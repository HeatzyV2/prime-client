import { useCallback, useEffect, useState } from 'react'
import { AnimatePresence } from 'framer-motion'
import { Download, Sun, Trash2, Upload } from 'lucide-react'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Button } from '@renderer/design-system/components'
import { ModrinthBrowseModal } from '@renderer/components/ModrinthBrowseModal'
import { useActiveInstance } from '@renderer/hooks/useActiveInstance'
import type { ShaderEntry } from '@shared/content-types'

export function ShadersPage() {
  const { instance, instanceId, refresh: refreshInstance } = useActiveInstance()
  const [shaders, setShaders] = useState<ShaderEntry[]>([])
  const [showBrowse, setShowBrowse] = useState(false)

  const refresh = useCallback(async () => {
    if (!instanceId) {
      return
    }
    setShaders(await window.primeLauncher.content.listShaders(instanceId))
  }, [instanceId])

  useEffect(() => {
    void refresh()
  }, [refresh])

  async function handleActivate(shader: ShaderEntry) {
    if (!instanceId) {
      return
    }
    const next = shader.active ? null : shader.fileName
    await window.primeLauncher.content.setShaderActive(next, instanceId)
    await refresh()
  }

  async function handleImport() {
    const result = await window.primeLauncher.content.importShader(instanceId ?? undefined)
    if (result.ok) {
      await refresh()
    } else if (result.error !== 'Cancelled.') {
      alert(result.error)
    }
  }

  async function handleRemove(shader: ShaderEntry) {
    if (!instanceId || !confirm(`Remove ${shader.name}?`)) {
      return
    }
    await window.primeLauncher.content.removeShader(shader.fileName, instanceId)
    await refresh()
  }

  return (
    <PageShell
      title="Shader Manager"
      subtitle={
        instance
          ? `Iris shader packs for ${instance.name} — activation saved in options.txt.`
          : 'Loading instance…'
      }
      actions={
        <div style={{ display: 'flex', gap: 8 }}>
          <Button variant="secondary" icon={<Upload size={16} />} onClick={() => void handleImport()}>
            Import .zip
          </Button>
          <Button variant="primary" icon={<Download size={16} />} onClick={() => setShowBrowse(true)}>
            Browse Modrinth
          </Button>
        </div>
      }
    >
      {shaders.length === 0 ? (
        <p className="text-caption">
          No shader packs yet. Install Iris (mod) first, then add packs here.
        </p>
      ) : (
        <div className="page-grid page-grid--3">
          {shaders.map((shader) => (
            <div
              key={shader.id}
              className={`tile${shader.active ? ' tile--active' : ''}`}
              onClick={() => void handleActivate(shader)}
              role="button"
              tabIndex={0}
            >
              <div className="tile__preview" style={{ background: 'linear-gradient(135deg, #1a1020, #3d1a28)' }}>
                <Sun size={32} style={{ color: '#ff6b6b' }} />
              </div>
              <div className="tile__name">{shader.name}</div>
              <div className="tile__desc">{shader.description}</div>
              <div style={{ marginTop: 12, display: 'flex', gap: 8, alignItems: 'center' }}>
                <Badge variant={shader.backend === 'iris' ? 'red' : 'default'}>{shader.backend}</Badge>
                {shader.active && <Badge variant="prime">Active</Badge>}
                <Button
                  variant="ghost"
                  size="sm"
                  icon={<Trash2 size={14} />}
                  onClick={(e) => {
                    e.stopPropagation()
                    void handleRemove(shader)
                  }}
                />
              </div>
            </div>
          ))}
        </div>
      )}

      <AnimatePresence>
        {showBrowse && (
          <ModrinthBrowseModal
            type="shader"
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
