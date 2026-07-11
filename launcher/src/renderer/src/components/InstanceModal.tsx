import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { Button } from '@renderer/design-system/components'
import type { GameInstance } from '@shared/types'
import type { CreateInstanceDto } from '@shared/ipc'
import '@renderer/components/LoginModal.css'

export type InstancePreset = 'prime' | 'fabric' | 'vanilla'

interface InstanceModalProps {
  mode: 'create' | 'edit'
  preset?: InstancePreset
  instance?: GameInstance
  onClose: () => void
  onSaved: () => void
}

const PRESETS: Record<
  InstancePreset,
  Omit<CreateInstanceDto, 'name'> & { defaultName: string }
> = {
  prime: {
    defaultName: 'Prime Client',
    minecraftVersion: '1.21.11',
    loader: 'fabric',
    fabricLoaderVersion: '0.19.3',
    fabricApiVersion: '0.141.4+1.21.11',
    includePrimeMod: true,
    ramMb: 4096,
    jvmArgs: ['-XX:+UseG1GC']
  },
  fabric: {
    defaultName: 'Fabric',
    minecraftVersion: '1.21.11',
    loader: 'fabric',
    fabricLoaderVersion: '0.19.3',
    includePrimeMod: false,
    ramMb: 4096,
    jvmArgs: ['-XX:+UseG1GC']
  },
  vanilla: {
    defaultName: 'Vanilla',
    minecraftVersion: '1.21.11',
    loader: 'vanilla',
    includePrimeMod: false,
    ramMb: 2048,
    jvmArgs: []
  }
}

export function InstanceModal({ mode, preset = 'fabric', instance, onClose, onSaved }: InstanceModalProps) {
  const base = mode === 'edit' && instance ? instance : PRESETS[preset]
  const [name, setName] = useState(
    mode === 'edit' && instance ? instance.name : PRESETS[preset].defaultName
  )
  const [minecraftVersion, setMinecraftVersion] = useState(base.minecraftVersion)
  const [loader, setLoader] = useState<'vanilla' | 'fabric'>(base.loader === 'vanilla' ? 'vanilla' : 'fabric')
  const [ramMb, setRamMb] = useState(base.ramMb)
  const [fabricLoaderVersion, setFabricLoaderVersion] = useState(instance?.fabricLoaderVersion ?? '0.19.3')
  const [includePrimeMod, setIncludePrimeMod] = useState(instance?.includePrimeMod ?? preset === 'prime')
  const [jvmArgsText, setJvmArgsText] = useState((instance?.jvmArgs ?? PRESETS[preset].jvmArgs ?? []).join('\n'))
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (mode === 'create') {
      const p = PRESETS[preset]
      setName(p.defaultName)
      setMinecraftVersion(p.minecraftVersion)
      setLoader(p.loader)
      setRamMb(p.ramMb)
      setFabricLoaderVersion(p.fabricLoaderVersion ?? '0.19.3')
      setIncludePrimeMod(Boolean(p.includePrimeMod))
      setJvmArgsText((p.jvmArgs ?? []).join('\n'))
    }
  }, [mode, preset])

  async function handleSubmit() {
    setBusy(true)
    setError(null)

    const jvmArgs = jvmArgsText
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean)

    if (mode === 'create') {
      const result = await window.primeLauncher.instance.create({
        name,
        minecraftVersion,
        loader,
        fabricLoaderVersion: loader === 'fabric' ? fabricLoaderVersion : undefined,
        fabricApiVersion: loader === 'fabric' && includePrimeMod ? '0.141.4+1.21.11' : undefined,
        includePrimeMod: loader === 'fabric' ? includePrimeMod : false,
        ramMb,
        jvmArgs
      })
      setBusy(false)
      if (result.ok) {
        onSaved()
        onClose()
      } else {
        setError(result.error ?? 'Could not create instance.')
      }
      return
    }

    if (!instance) {
      setBusy(false)
      return
    }

    const result = await window.primeLauncher.instance.update({
      id: instance.id,
      name,
      minecraftVersion,
      loader,
      fabricLoaderVersion: loader === 'fabric' ? fabricLoaderVersion : undefined,
      includePrimeMod: loader === 'fabric' ? includePrimeMod : false,
      ramMb,
      jvmArgs
    })
    setBusy(false)
    if (result.ok) {
      onSaved()
      onClose()
    } else {
      setError(result.error ?? 'Could not save instance.')
    }
  }

  return (
    <motion.div
      className="modal-backdrop"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onClick={onClose}
    >
      <motion.div
        className="modal"
        style={{ width: 'min(520px, 100%)' }}
        initial={{ opacity: 0, scale: 0.95, y: 12 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 12 }}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="modal__title">{mode === 'create' ? 'New Instance' : 'Configure Instance'}</h2>
        <p className="modal__subtitle">
          Stored locally in AppData — Vanilla and Fabric supported, no server required.
        </p>

        <label className="text-caption">Name</label>
        <input className="modal__field" value={name} onChange={(e) => setName(e.target.value)} />

        <label className="text-caption">Minecraft version</label>
        <input
          className="modal__field"
          value={minecraftVersion}
          onChange={(e) => setMinecraftVersion(e.target.value)}
          placeholder="1.21.11"
        />

        <label className="text-caption">Loader</label>
        <select
          className="modal__field"
          value={loader}
          onChange={(e) => setLoader(e.target.value as 'vanilla' | 'fabric')}
        >
          <option value="fabric">Fabric</option>
          <option value="vanilla">Vanilla</option>
        </select>

        {loader === 'fabric' && (
          <>
            <label className="text-caption">Fabric loader</label>
            <input
              className="modal__field"
              value={fabricLoaderVersion}
              onChange={(e) => setFabricLoaderVersion(e.target.value)}
              placeholder="0.19.3"
            />
            <label className="text-caption" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <input
                type="checkbox"
                checked={includePrimeMod}
                onChange={(e) => setIncludePrimeMod(e.target.checked)}
              />
              Include Prime Client mod (+ Fabric API)
            </label>
          </>
        )}

        <label className="text-caption">RAM (MB)</label>
        <input
          className="modal__field"
          type="number"
          min={512}
          max={16384}
          step={256}
          value={ramMb}
          onChange={(e) => setRamMb(Number(e.target.value))}
        />

        <label className="text-caption">JVM args (one per line)</label>
        <textarea
          className="modal__field"
          style={{ height: 72, padding: '10px 12px', resize: 'vertical' }}
          value={jvmArgsText}
          onChange={(e) => setJvmArgsText(e.target.value)}
        />

        {error && <div className="modal__error">{error}</div>}

        <div className="modal__footer">
          <Button variant="ghost" onClick={onClose} disabled={busy}>
            Cancel
          </Button>
          <Button variant="primary" disabled={busy} onClick={() => void handleSubmit()}>
            {busy ? 'Saving…' : mode === 'create' ? 'Create' : 'Save'}
          </Button>
        </div>
      </motion.div>
    </motion.div>
  )
}
