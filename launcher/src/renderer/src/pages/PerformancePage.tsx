import { useEffect, useState } from 'react'
import { Cpu, Monitor, Zap } from 'lucide-react'
import type { HardwareProfile, PerformancePreset, PerformancePresetInfo } from '@shared/content-types'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Button, Card } from '@renderer/design-system/components'
import { useActiveInstance } from '@renderer/hooks/useActiveInstance'

export function PerformancePage() {
  const { instanceId } = useActiveInstance()
  const [selected, setSelected] = useState<PerformancePreset>('balanced')
  const [hw, setHw] = useState<HardwareProfile | null>(null)
  const [presets, setPresets] = useState<PerformancePresetInfo[]>([])
  const [message, setMessage] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    void (async () => {
      const [hardware, list, active] = await Promise.all([
        window.primeLauncher.performance.hardware(),
        window.primeLauncher.performance.presets(),
        window.primeLauncher.performance.selected()
      ])
      setHw(hardware)
      setPresets(list)
      setSelected(active)
    })()
  }, [])

  async function handleApply() {
    setBusy(true)
    setMessage(null)
    const result = await window.primeLauncher.performance.apply(selected, instanceId ?? undefined)
    setBusy(false)
    setMessage(result.ok ? 'Preset applied to active instance (RAM + options.txt).' : result.error ?? 'Failed.')
  }

  return (
    <PageShell
      title="Performance Center"
      subtitle="Detects local hardware and applies Minecraft presets — no cloud optimizer."
      actions={
        <Button variant="primary" icon={<Zap size={16} />} disabled={busy} onClick={() => void handleApply()}>
          {busy ? 'Applying…' : 'Apply Preset'}
        </Button>
      }
    >
      {message && (
        <p className="text-caption" style={{ marginBottom: 16, color: 'var(--prime-muted)' }}>
          {message}
        </p>
      )}

      <Card title="Detected Hardware" glow>
        {hw ? (
          <div style={{ display: 'flex', gap: 32, flexWrap: 'wrap' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <Cpu size={20} style={{ color: 'var(--prime-red-bright)' }} />
              <div>
                <div className="text-caption">CPU</div>
                <div className="list-row__title">{hw.cpu}</div>
              </div>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <Monitor size={20} style={{ color: 'var(--prime-red-bright)' }} />
              <div>
                <div className="text-caption">GPU</div>
                <div className="list-row__title">{hw.gpu}</div>
              </div>
            </div>
            <div>
              <div className="text-caption">System RAM</div>
              <div className="list-row__title">{hw.ramGb} GB</div>
            </div>
          </div>
        ) : (
          <p className="text-caption">Detecting hardware…</p>
        )}
      </Card>

      <div className="page-grid page-grid--4" style={{ marginTop: 8 }}>
        {presets.map((preset) => (
          <div
            key={preset.id}
            className={`tile${selected === preset.id ? ' tile--active' : ''}`}
            onClick={() => setSelected(preset.id)}
            role="button"
            tabIndex={0}
          >
            <div className="tile__name">{preset.label}</div>
            <div className="tile__desc">{preset.description}</div>
            <div style={{ marginTop: 16, display: 'flex', flexDirection: 'column', gap: 8 }}>
              <Badge variant="default">{preset.ramMb} MB RAM</Badge>
              <Badge variant="default">{preset.renderDistance} chunks</Badge>
            </div>
          </div>
        ))}
      </div>
    </PageShell>
  )
}
