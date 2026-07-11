import { useCallback, useEffect, useState } from 'react'
import { AnimatePresence } from 'framer-motion'
import { Plus, Play, Box, FolderOpen, Copy, Trash2, Star } from 'lucide-react'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Button, Card } from '@renderer/design-system/components'
import { InstanceModal, type InstancePreset } from '@renderer/components/InstanceModal'
import { LoginModal } from '@renderer/components/LoginModal'
import { useAccounts } from '@renderer/context/AccountProvider'
import { useI18n } from '@renderer/context/I18nProvider'
import type { GameInstance } from '@shared/types'

export function InstancesPage() {
  const { t, locale } = useI18n()
  const { launch, activeAccount } = useAccounts()
  const [instances, setInstances] = useState<GameInstance[]>([])
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState<{ mode: 'create' | 'edit'; preset?: InstancePreset; instance?: GameInstance } | null>(
    null
  )
  const [showLogin, setShowLogin] = useState(false)
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
      setShowLogin(true)
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
    if (!confirm(t('confirm.deleteInstance', { name: inst.name }))) {
      return
    }
    const deleteFiles = confirm(t('confirm.deleteFiles'))
    const result = await window.primeLauncher.instance.remove(inst.id, deleteFiles)
    if (!result.ok) {
      alert(result.error ?? t('errors.deleteInstance'))
      return
    }
    await refresh()
  }

  return (
    <PageShell
      title={t('pages.instances.title')}
      subtitle={t('pages.instances.subtitle')}
      actions={
        <div style={{ display: 'flex', gap: 8 }}>
          <Button variant="secondary" size="sm" onClick={() => setModal({ mode: 'create', preset: 'vanilla' })}>
            {t('instances.vanilla')}
          </Button>
          <Button variant="secondary" size="sm" onClick={() => setModal({ mode: 'create', preset: 'fabric' })}>
            {t('instances.fabric')}
          </Button>
          <Button variant="primary" icon={<Plus size={16} />} onClick={() => setModal({ mode: 'create', preset: 'prime' })}>
            {t('instances.primeClient')}
          </Button>
        </div>
      }
    >
      {loading ? (
        <p className="text-caption">{t('instances.loading')}</p>
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
                <Badge variant="default">{t('instances.ramBadge', { mb: inst.ramMb })}</Badge>
                <Badge variant="default">{t('instances.modsBadge', { count: inst.modCount })}</Badge>
                {inst.isDefault && <Badge variant="prime">{t('instances.default')}</Badge>}
              </div>

              {inst.lastPlayed && (
                <p className="text-caption" style={{ marginBottom: 12 }}>
                  {t('instances.lastPlayed', {
                    date: new Date(inst.lastPlayed).toLocaleDateString(locale)
                  })}
                </p>
              )}

              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                <Button
                  variant="primary"
                  size="sm"
                  icon={<Play size={14} />}
                  disabled={busyId === inst.id}
                  onClick={() => void handlePlay(inst)}
                >
                  {activeAccount ? t('instances.play') : t('instances.signInToPlay')}
                </Button>
                <Button variant="secondary" size="sm" onClick={() => setModal({ mode: 'edit', instance: inst })}>
                  {t('actions.configure')}
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  icon={<FolderOpen size={14} />}
                  onClick={() => void window.primeLauncher.instance.openFolder(inst.id)}
                >
                  {t('actions.folder')}
                </Button>
                {!inst.isDefault && (
                  <Button
                    variant="ghost"
                    size="sm"
                    icon={<Star size={14} />}
                    onClick={() => void handleSetDefault(inst.id)}
                  >
                    {t('instances.default')}
                  </Button>
                )}
                <Button
                  variant="ghost"
                  size="sm"
                  icon={<Copy size={14} />}
                  onClick={() => void handleDuplicate(inst.id)}
                >
                  {t('actions.duplicate')}
                </Button>
                {instances.length > 1 && (
                  <Button
                    variant="ghost"
                    size="sm"
                    icon={<Trash2 size={14} />}
                    onClick={() => void handleDelete(inst)}
                  >
                    {t('actions.delete')}
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
        {showLogin && <LoginModal onClose={() => setShowLogin(false)} />}
      </AnimatePresence>
    </PageShell>
  )
}
