import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { Download } from 'lucide-react'
import { Button, SearchInput, Tabs } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import type { ModrinthSearchHitDto } from '@shared/ipc'
import '@renderer/components/LoginModal.css'

type ContentSource = 'modrinth' | 'curseforge'

interface ContentBrowseModalProps {
  type: 'mod' | 'resourcepack' | 'shader'
  instanceId: string | null
  onClose: () => void
  onInstalled: () => void
}

export function ContentBrowseModal({ type, instanceId, onClose, onInstalled }: ContentBrowseModalProps) {
  const { t } = useI18n()
  const [source, setSource] = useState<ContentSource>('modrinth')
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<ModrinthSearchHitDto[]>([])
  const [searching, setSearching] = useState(false)
  const [installingId, setInstallingId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!query.trim()) {
      setResults([])
      return
    }

    const timer = setTimeout(() => {
      void (async () => {
        setSearching(true)
        setError(null)
        try {
          const hits =
            source === 'modrinth'
              ? await window.primeLauncher.content.searchModrinth(query.trim(), type, instanceId ?? undefined)
              : await window.primeLauncher.content.searchCurseForge(query.trim(), type, instanceId ?? undefined)
          setResults(hits)
        } catch (err) {
          setError(err instanceof Error ? err.message : t('modals.browse.searchFailed'))
        } finally {
          setSearching(false)
        }
      })()
    }, 350)

    return () => clearTimeout(timer)
  }, [query, type, instanceId, source, t])

  async function handleInstall(hit: ModrinthSearchHitDto) {
    setInstallingId(hit.project_id)
    setError(null)

    let result
    if (type === 'mod') {
      result = await window.primeLauncher.content.installMod(
        hit.project_id,
        hit.title,
        instanceId ?? undefined,
        source
      )
    } else if (type === 'resourcepack') {
      result = await window.primeLauncher.content.installResourcePack(
        hit.project_id,
        hit.title,
        instanceId ?? undefined,
        source
      )
    } else {
      result = await window.primeLauncher.content.installShader(
        hit.project_id,
        hit.title,
        instanceId ?? undefined,
        source
      )
    }

    setInstallingId(null)
    if (result.ok) {
      onInstalled()
      onClose()
    } else if (result.error !== 'Cancelled.') {
      setError(result.error ?? t('modals.browse.installFailed'))
    }
  }

  const title =
    type === 'mod'
      ? t('modals.browse.modsTitle')
      : type === 'resourcepack'
        ? t('modals.browse.resourcePacksTitle')
        : t('modals.browse.shadersTitle')

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
        style={{ width: 'min(640px, 100%)', maxHeight: '80vh', overflow: 'auto' }}
        initial={{ opacity: 0, scale: 0.95, y: 12 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 12 }}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="modal__title">{title}</h2>
        <p className="modal__subtitle">{t('modals.browse.subtitle')}</p>

        <Tabs
          tabs={[
            { id: 'modrinth', label: 'Modrinth' },
            { id: 'curseforge', label: 'CurseForge' }
          ]}
          active={source}
          onChange={(id) => setSource(id as ContentSource)}
        />

        <div style={{ marginTop: 12 }}>
          <SearchInput
            value={query}
            onChange={setQuery}
            placeholder={
              source === 'modrinth' ? t('actions.searchModrinth') : t('actions.searchCurseForge')
            }
          />
        </div>

        {searching && <p className="text-caption">{t('modals.browse.searching')}</p>}
        {error && <div className="modal__error">{error}</div>}

        <div className="page-list" style={{ marginTop: 16 }}>
          {results.map((hit) => (
            <div key={`${source}-${hit.project_id}`} className="list-row">
              {hit.icon_url ? (
                <img
                  src={hit.icon_url}
                  alt=""
                  width={40}
                  height={40}
                  style={{ borderRadius: 8, objectFit: 'cover' }}
                />
              ) : (
                <div className="list-row__icon">
                  <Download size={18} />
                </div>
              )}
              <div className="list-row__body">
                <div className="list-row__title">{hit.title}</div>
                <div className="list-row__desc">{hit.description}</div>
              </div>
              <Button
                variant="primary"
                size="sm"
                disabled={installingId === hit.project_id}
                onClick={() => void handleInstall(hit)}
              >
                {installingId === hit.project_id ? t('actions.installing') : t('actions.install')}
              </Button>
            </div>
          ))}
        </div>

        <div className="modal__footer">
          <Button variant="ghost" onClick={onClose}>
            {t('modals.browse.close')}
          </Button>
        </div>
      </motion.div>
    </motion.div>
  )
}
