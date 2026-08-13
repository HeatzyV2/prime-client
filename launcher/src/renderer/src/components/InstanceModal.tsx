import { useEffect, useMemo, useState } from 'react'
import { motion } from 'framer-motion'
import { Button, Select } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import type { GameInstance } from '@shared/types'
import type { JavaInstallationDto, MinecraftVersionOptionDto } from '@shared/ipc'
import {
  DEFAULT_MINECRAFT_TARGET,
  MINECRAFT_TARGETS,
  isSupportedPrimeVersion
} from '@shared/minecraft-targets'
import '@renderer/components/LoginModal.css'
import '@renderer/components/InstanceModal.css'

export type InstancePreset = 'prime' | 'fabric' | 'vanilla'

interface InstanceModalProps {
  mode: 'create' | 'edit'
  preset?: InstancePreset
  /** Pre-select MC version when opening create (e.g. "26.2"). */
  initialMcVersion?: string
  instance?: GameInstance
  onClose: () => void
  onSaved: () => void
}

type InstanceKind = 'prime' | 'fabric' | 'vanilla'

function kindFromInstance(inst: GameInstance): InstanceKind {
  if (inst.loader === 'vanilla') return 'vanilla'
  if (inst.includePrimeMod) return 'prime'
  return 'fabric'
}

function defaultNameFor(kind: InstanceKind, mcVersion: string): string {
  if (kind === 'prime') return `Prime Client ${mcVersion}`
  if (kind === 'fabric') return `Fabric ${mcVersion}`
  return `Vanilla ${mcVersion}`
}

function pickDefaultVersion(
  versions: MinecraftVersionOptionDto[],
  kind: InstanceKind,
  preferred?: string
): string {
  if (preferred) {
    const hit = versions.find((v) => v.id === preferred)
    if (hit) {
      if (kind === 'prime' && !hit.primeAvailable) {
        // fall through
      } else if (kind === 'fabric' && !hit.fabricAvailable) {
        // fall through
      } else {
        return hit.id
      }
    }
  }

  if (kind === 'prime') {
    return (
      versions.find((v) => v.primeAvailable && v.recommended)?.id ??
      versions.find((v) => v.primeAvailable)?.id ??
      DEFAULT_MINECRAFT_TARGET.mcVersion
    )
  }

  if (kind === 'fabric') {
    return (
      versions.find((v) => v.fabricAvailable && v.recommended)?.id ??
      versions.find((v) => v.fabricAvailable)?.id ??
      DEFAULT_MINECRAFT_TARGET.mcVersion
    )
  }

  return versions.find((v) => v.recommended)?.id ?? versions[0]?.id ?? DEFAULT_MINECRAFT_TARGET.mcVersion
}

function offlineFallbackVersions(): MinecraftVersionOptionDto[] {
  return MINECRAFT_TARGETS.map((t) => ({
    id: t.mcVersion,
    type: 'release' as const,
    fabricAvailable: true,
    primeAvailable: true,
    recommended: Boolean(t.recommended),
    javaMajor: t.javaMajor,
    fabricLoader: t.fabricLoader,
    fabricApi: t.fabricApi
  }))
}

export function InstanceModal({
  mode,
  preset = 'prime',
  initialMcVersion,
  instance,
  onClose,
  onSaved
}: InstanceModalProps) {
  const { t } = useI18n()
  const [versions, setVersions] = useState<MinecraftVersionOptionDto[]>(offlineFallbackVersions)
  const [versionsLoading, setVersionsLoading] = useState(mode === 'create')
  const [kind, setKind] = useState<InstanceKind>(
    mode === 'edit' && instance ? kindFromInstance(instance) : preset
  )
  const [mcVersion, setMcVersion] = useState(
    mode === 'edit' && instance
      ? instance.minecraftVersion
      : initialMcVersion ?? DEFAULT_MINECRAFT_TARGET.mcVersion
  )
  const [name, setName] = useState(
    mode === 'edit' && instance
      ? instance.name
      : defaultNameFor(preset, initialMcVersion ?? DEFAULT_MINECRAFT_TARGET.mcVersion)
  )
  const [ramMb, setRamMb] = useState(
    mode === 'edit' && instance ? instance.ramMb : preset === 'vanilla' ? 2048 : 4096
  )
  const [showAdvanced, setShowAdvanced] = useState(false)
  const [jvmArgsText, setJvmArgsText] = useState(
    (instance?.jvmArgs ?? (preset === 'vanilla' ? [] : ['-XX:+UseG1GC'])).join('\n')
  )
  const [javaPath, setJavaPath] = useState(instance?.javaPath ?? '')
  const [javaInstalls, setJavaInstalls] = useState<JavaInstallationDto[]>([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [nameTouched, setNameTouched] = useState(mode === 'edit')
  const [versionFilter, setVersionFilter] = useState('')

  useEffect(() => {
    void window.primeLauncher.settings.listJava().then(setJavaInstalls)
  }, [])

  useEffect(() => {
    let cancelled = false
    setVersionsLoading(true)
    void window.primeLauncher.instance
      .listVersions()
      .then((list) => {
        if (cancelled || !list?.length) return
        setVersions(list)
      })
      .catch(() => {
        /* keep offline fallback */
      })
      .finally(() => {
        if (!cancelled) setVersionsLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (mode !== 'create') return
    setKind(preset)
    const nextVersion = pickDefaultVersion(versions, preset, initialMcVersion)
    setMcVersion(nextVersion)
    setName(defaultNameFor(preset, nextVersion))
    setNameTouched(false)
    setRamMb(preset === 'vanilla' ? 2048 : 4096)
    setJvmArgsText(preset === 'vanilla' ? '' : '-XX:+UseG1GC')
    setJavaPath('')
    setShowAdvanced(false)
    setError(null)
    setVersionFilter('')
    // Only reset when opening / changing preset — not when the live catalog arrives.
    // eslint-disable-next-line react-hooks/exhaustive-deps -- versions intentionally omitted
  }, [mode, preset, initialMcVersion])

  useEffect(() => {
    if (!nameTouched && mode === 'create') {
      setName(defaultNameFor(kind, mcVersion))
    }
  }, [kind, mcVersion, nameTouched, mode])

  const filteredVersions = useMemo(() => {
    const q = versionFilter.trim().toLowerCase()
    return versions.filter((v) => {
      if (kind === 'prime' && !v.primeAvailable) return false
      if (kind === 'fabric' && !v.fabricAvailable) return false
      if (!q) return true
      return v.id.toLowerCase().includes(q)
    })
  }, [versions, kind, versionFilter])

  const versionOptions = useMemo(() => {
    return filteredVersions.map((v) => {
      const bits: string[] = [v.id]
      if (v.recommended) bits.push(`· ${t('modals.instance.recommended')}`)
      else if (kind === 'prime' && v.primeAvailable) bits.push('· Prime')
      return { value: v.id, label: bits.join(' ') }
    })
  }, [filteredVersions, kind, t])

  const selectedMeta = useMemo(
    () => versions.find((v) => v.id === mcVersion),
    [versions, mcVersion]
  )

  useEffect(() => {
    if (filteredVersions.length === 0) return
    if (!filteredVersions.some((v) => v.id === mcVersion)) {
      setMcVersion(pickDefaultVersion(filteredVersions, kind, initialMcVersion))
    }
  }, [filteredVersions, kind, mcVersion, initialMcVersion])

  function handleKindChange(next: InstanceKind) {
    setKind(next)
    if (mode === 'create') {
      setRamMb(next === 'vanilla' ? 2048 : 4096)
      setJvmArgsText(next === 'vanilla' ? '' : '-XX:+UseG1GC')
    }
  }

  async function handleSubmit() {
    setBusy(true)
    setError(null)

    if (kind === 'prime' && !isSupportedPrimeVersion(mcVersion)) {
      setBusy(false)
      setError(t('modals.instance.primeUnsupported', { version: mcVersion }))
      return
    }

    if (kind === 'fabric' && selectedMeta && !selectedMeta.fabricAvailable) {
      setBusy(false)
      setError(t('modals.instance.fabricUnsupported', { version: mcVersion }))
      return
    }

    const jvmArgs = jvmArgsText
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean)

    const loader = kind === 'vanilla' ? 'vanilla' : 'fabric'
    const includePrimeMod = kind === 'prime'
    const payload = {
      name,
      minecraftVersion: mcVersion,
      loader: loader as 'vanilla' | 'fabric',
      fabricLoaderVersion:
        loader === 'fabric' ? selectedMeta?.fabricLoader ?? 'latest' : undefined,
      fabricApiVersion: includePrimeMod ? selectedMeta?.fabricApi : undefined,
      includePrimeMod,
      ramMb,
      jvmArgs
    }

    if (mode === 'create') {
      const result = await window.primeLauncher.instance.create(payload)
      setBusy(false)
      if (result.ok) {
        onSaved()
        onClose()
      } else {
        setError(result.error ?? t('modals.instance.createFailed'))
      }
      return
    }

    if (!instance) {
      setBusy(false)
      return
    }

    const result = await window.primeLauncher.instance.update({
      id: instance.id,
      ...payload,
      javaPath: javaPath || undefined
    })
    setBusy(false)
    if (result.ok) {
      onSaved()
      onClose()
    } else {
      setError(result.error ?? t('modals.instance.saveFailed'))
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
        className="modal instance-modal"
        initial={{ opacity: 0, scale: 0.95, y: 12 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 12 }}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="modal__title">
          {mode === 'create' ? t('modals.instance.createTitle') : t('modals.instance.editTitle')}
        </h2>
        <p className="modal__subtitle">{t('modals.instance.subtitle')}</p>

        <label className="text-caption" htmlFor="instance-name">
          {t('modals.instance.name')}
        </label>
        <input
          id="instance-name"
          className="modal__field"
          value={name}
          maxLength={32}
          onChange={(e) => {
            setNameTouched(true)
            setName(e.target.value)
          }}
        />

        <label className="text-caption">{t('modals.instance.minecraftVersion')}</label>
        <input
          className="modal__field instance-modal__version-filter"
          type="search"
          placeholder={t('modals.instance.versionSearch')}
          value={versionFilter}
          onChange={(e) => setVersionFilter(e.target.value)}
          aria-label={t('modals.instance.versionSearch')}
        />
        <Select
          className="modal__select instance-modal__version-select"
          value={mcVersion}
          aria-label={t('modals.instance.minecraftVersion')}
          placeholder={
            versionsLoading
              ? t('modals.instance.versionsLoading')
              : t('modals.instance.minecraftVersion')
          }
          onChange={setMcVersion}
          options={
            versionOptions.length > 0
              ? versionOptions
              : [{ value: mcVersion, label: mcVersion, disabled: true }]
          }
        />
        {kind === 'prime' && (
          <p className="text-caption instance-modal__note">{t('modals.instance.primeAutoNote')}</p>
        )}
        {kind === 'fabric' && (
          <p className="text-caption instance-modal__note">{t('modals.instance.fabricNote')}</p>
        )}

        <label className="text-caption">{t('modals.instance.kind')}</label>
        <div className="instance-modal__cards">
          {(
            [
              ['prime', t('instances.primeClient')],
              ['fabric', t('instances.fabric')],
              ['vanilla', t('instances.vanilla')]
            ] as const
          ).map(([id, label]) => (
            <button
              key={id}
              type="button"
              className={`instance-modal__card${kind === id ? ' is-active' : ''}${
                id === 'prime' ? ' instance-modal__card--prime' : ''
              }`}
              onClick={() => handleKindChange(id)}
            >
              <span className="instance-modal__card-title">
                {label}
                {id === 'prime' ? (
                  <span className="instance-modal__badge">{t('modals.instance.recommended')}</span>
                ) : null}
              </span>
              <span className="instance-modal__card-desc">
                {id === 'prime'
                  ? t('modals.instance.kindPrimeHint')
                  : id === 'fabric'
                    ? t('modals.instance.kindFabricHint')
                    : t('modals.instance.kindVanillaHint')}
              </span>
            </button>
          ))}
        </div>

        <button
          type="button"
          className="instance-modal__advanced-toggle"
          onClick={() => setShowAdvanced((v) => !v)}
        >
          {showAdvanced ? t('modals.instance.hideAdvanced') : t('modals.instance.showAdvanced')}
        </button>

        {showAdvanced && (
          <>
            <label className="text-caption">{t('modals.instance.ram')}</label>
            <input
              className="modal__field"
              type="number"
              min={512}
              max={16384}
              step={256}
              value={ramMb}
              onChange={(e) => setRamMb(Number(e.target.value))}
            />

            <label className="text-caption">{t('modals.instance.jvmArgs')}</label>
            <textarea
              className="modal__field"
              style={{ height: 72, padding: '10px 12px', resize: 'vertical' }}
              value={jvmArgsText}
              onChange={(e) => setJvmArgsText(e.target.value)}
            />

            {mode === 'edit' && (
              <>
                <label className="text-caption">{t('modals.instance.javaPath')}</label>
                <p className="text-caption" style={{ margin: '0 0 8px', color: 'var(--prime-muted)' }}>
                  {t('modals.instance.javaPathHint')}
                </p>
                <div style={{ display: 'flex', gap: 8, alignItems: 'flex-start' }}>
                  <Select
                    className="modal__select"
                    value={javaPath || 'auto'}
                    aria-label={t('modals.instance.javaPath')}
                    onChange={(v) => setJavaPath(v === 'auto' ? '' : v)}
                    options={[
                      { value: 'auto', label: t('common.automatic') },
                      ...javaInstalls.map((java) => ({ value: java.path, label: java.label }))
                    ]}
                  />
                  <Button
                    variant="ghost"
                    onClick={() =>
                      void (async () => {
                        const result = await window.primeLauncher.settings.browseJava()
                        if (!result.ok || !result.install) {
                          if (result.error && result.error !== 'Cancelled.') {
                            setError(result.error)
                          }
                          return
                        }
                        await window.primeLauncher.settings.addJavaPath(result.install.path)
                        setJavaPath(result.install.path)
                        setJavaInstalls(await window.primeLauncher.settings.listJava())
                      })()
                    }
                  >
                    {t('settings.javaPath.addPath')}
                  </Button>
                </div>
              </>
            )}
          </>
        )}

        {error && <div className="modal__error">{error}</div>}

        <div className="modal__footer">
          <Button variant="ghost" onClick={onClose} disabled={busy}>
            {t('actions.cancel')}
          </Button>
          <Button variant="primary" disabled={busy || !name.trim()} onClick={() => void handleSubmit()}>
            {busy
              ? t('modals.instance.saving')
              : mode === 'create'
                ? t('modals.instance.create')
                : t('actions.save')}
          </Button>
        </div>
      </motion.div>
    </motion.div>
  )
}
