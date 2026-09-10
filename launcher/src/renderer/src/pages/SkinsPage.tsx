import { Suspense, lazy, useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Check, Plus, Sparkles, Trash2 } from 'lucide-react'
import type { CosmeticItem } from '@shared/content-types'
import { playerBodyUrl, playerCapeUrl } from '@shared/format'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Button, Tabs } from '@renderer/design-system/components'
import { EmptyState } from '@renderer/components/EmptyState'
import { useAccounts } from '@renderer/context/AccountProvider'
import { useI18n } from '@renderer/context/I18nProvider'
import { playUiSound } from '@renderer/lib/uiSounds'
import './SkinsPage.css'

const SkinViewer3D = lazy(() =>
  import('@renderer/components/SkinViewer3D').then((m) => ({ default: m.SkinViewer3D }))
)

type Pose = 'idle' | 'walk' | 'run'
type SkinTab = 'skins' | 'cape' | 'wings' | 'aura' | 'trail' | 'hat' | 'emote' | 'badge'

interface LocalSkin {
  id: string
  name: string
  dataUrl: string
}

const RARITY_VARIANT: Record<CosmeticItem['rarity'], 'default' | 'red' | 'prime' | 'success'> = {
  common: 'default',
  rare: 'success',
  epic: 'red',
  legendary: 'prime',
  mythic: 'prime',
  prime_exclusive: 'prime'
}

export function SkinsPage() {
  const { t } = useI18n()
  const { activeAccount } = useAccounts()
  const [tab, setTab] = useState<SkinTab>('skins')
  const [pose, setPose] = useState<Pose>('idle')
  const [cosmetics, setCosmetics] = useState<CosmeticItem[]>([])
  const [localSkins, setLocalSkins] = useState<LocalSkin[]>([])
  const [activeSkinId, setActiveSkinId] = useState<string | null>(null)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const username = activeAccount?.username ?? 'Steve'
  const uuid = activeAccount?.uuid
  const bodyUrl = playerBodyUrl(uuid, username, 320)
  const capeUrl = playerCapeUrl(uuid, username, activeAccount?.capeUrl)

  const tabs = useMemo(
    () => [
      { id: 'skins', label: t('skins.tabs.skins') },
      { id: 'cape', label: t('skins.tabs.capes') },
      { id: 'wings', label: t('skins.tabs.wings') },
      { id: 'aura', label: 'Auras' },
      { id: 'trail', label: 'Trails' },
      { id: 'hat', label: 'Hats' },
      { id: 'emote', label: 'Emotes' },
      { id: 'badge', label: t('skins.tabs.badges') }
    ],
    [t]
  )

  const refresh = useCallback(async () => {
    const [list, skins, settings] = await Promise.all([
      window.primeLauncher.cosmetic.list(),
      window.primeLauncher.skins.list(),
      window.primeLauncher.settings.get()
    ])
    setCosmetics(list)
    setLocalSkins(skins)
    setActiveSkinId(settings.activeSkinId ?? null)
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  useEffect(() => {
    setSelectedId(null)
  }, [tab])

  const cosmeticItems = useMemo(
    () => cosmetics.filter((c) => c.type === tab),
    [cosmetics, tab]
  )

  const selectedCosmetic = cosmetics.find((c) => c.id === selectedId) ?? null
  const equippedCape = cosmetics.find((c) => c.type === 'cape' && c.equipped)
  const previewSkinUrl =
    (selectedId && localSkins.find((s) => s.id === selectedId)?.dataUrl) ||
    localSkins.find((s) => s.id === activeSkinId)?.dataUrl ||
    null

  async function handleImport() {
    setBusy(true)
    const result = await window.primeLauncher.skins.import()
    if (result.ok) {
      playUiSound('success')
      await refresh()
      if (result.skin) setSelectedId(result.skin.id)
    }
    setBusy(false)
  }

  async function handleApplySkin() {
    if (tab !== 'skins') return
    setBusy(true)
    await window.primeLauncher.skins.setActive(selectedId)
    playUiSound('success')
    await refresh()
    setBusy(false)
  }

  async function handleRemoveSkin(id: string) {
    setBusy(true)
    await window.primeLauncher.skins.remove(id)
    playUiSound('click')
    if (selectedId === id) setSelectedId(null)
    await refresh()
    setBusy(false)
  }

  async function handleApply() {
    if (tab === 'skins') return
    if (!selectedCosmetic) return
    if (!selectedCosmetic.equipped) {
      await window.primeLauncher.cosmetic.toggle(selectedCosmetic.id)
      playUiSound('success')
      await refresh()
    }
  }

  async function handleUnequipSelected() {
    if (!selectedCosmetic?.equipped) return
    await window.primeLauncher.cosmetic.toggle(selectedCosmetic.id)
    playUiSound('click')
    await refresh()
  }

  return (
    <PageShell title={t('pages.skins.title')} subtitle={t('pages.skins.subtitle')}>
      <div className="cosmetics-v3">
        <aside className="cosmetics-v3__stage">
          <Suspense
            fallback={
              <div className="cosmetics-v3__viewer-fallback" aria-busy="true">
                <img src={bodyUrl} alt="" draggable={false} />
              </div>
            }
          >
            <SkinViewer3D
              className="cosmetics-v3__viewer"
              uuid={uuid}
              username={username}
              skinUrl={previewSkinUrl}
              capeUrl={tab === 'badge' ? null : capeUrl}
              pose={pose}
              width={240}
              height={320}
              showControls={false}
            />
          </Suspense>

          {equippedCape && tab !== 'badge' && (
            <Badge variant="prime">{equippedCape.name}</Badge>
          )}

          <div className="cosmetics-v3__poses" role="group" aria-label="Pose">
            {(
              [
                ['idle', t('skins.pose.idle')],
                ['walk', t('skins.pose.walk')],
                ['run', t('skins.pose.run')]
              ] as const
            ).map(([id, label]) => (
              <button
                key={id}
                type="button"
                className={`cosmetics-v3__pose${pose === id ? ' is-active' : ''}`}
                onClick={() => setPose(id)}
              >
                {label}
              </button>
            ))}
          </div>

          <p className="cosmetics-v3__name">{username}</p>
        </aside>

        <section className="cosmetics-v3__main">
          <Tabs tabs={tabs} active={tab} onChange={(id) => setTab(id as SkinTab)} />

          {tab === 'skins' ? (
            <div className="cosmetics-v3__list">
              <button
                type="button"
                className="cosmetics-v3__row cosmetics-v3__row--add"
                disabled={busy}
                onClick={() => void handleImport()}
              >
                <Plus size={16} strokeWidth={1.75} />
                <span>{t('skins.addSkin')}</span>
              </button>

              <button
                type="button"
                className={`cosmetics-v3__row${!selectedId && !activeSkinId ? ' is-active' : ''}`}
                onClick={() => setSelectedId(null)}
              >
                <img className="cosmetics-v3__thumb" src={bodyUrl} alt="" draggable={false} />
                <span className="cosmetics-v3__row-name">{username}</span>
                {!activeSkinId && <span className="cosmetics-v3__tag">{t('common.active')}</span>}
              </button>

              {localSkins.map((skin) => (
                <button
                  key={skin.id}
                  type="button"
                  className={`cosmetics-v3__row${selectedId === skin.id || (!selectedId && activeSkinId === skin.id) ? ' is-active' : ''}`}
                  onClick={() => setSelectedId(skin.id)}
                >
                  <img
                    className="cosmetics-v3__thumb cosmetics-v3__thumb--pixel"
                    src={skin.dataUrl}
                    alt=""
                    draggable={false}
                  />
                  <span className="cosmetics-v3__row-name">{skin.name}</span>
                  {activeSkinId === skin.id && (
                    <span className="cosmetics-v3__tag">{t('common.active')}</span>
                  )}
                  <span
                    className="cosmetics-v3__remove"
                    role="button"
                    tabIndex={0}
                    title={t('skins.remove')}
                    onClick={(e) => {
                      e.stopPropagation()
                      void handleRemoveSkin(skin.id)
                    }}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.stopPropagation()
                        void handleRemoveSkin(skin.id)
                      }
                    }}
                  >
                    <Trash2 size={12} strokeWidth={1.75} />
                  </span>
                </button>
              ))}
            </div>
          ) : cosmeticItems.length === 0 ? (
            <EmptyState
              icon={<Sparkles size={22} strokeWidth={1.75} />}
              title={t('cosmetics.emptyOwned')}
              description={t('skins.emptyCosmeticsHint')}
              action={
                <Link to="/store">
                  <Button variant="secondary" size="sm">
                    {t('skins.openStore')}
                  </Button>
                </Link>
              }
            />
          ) : (
            <div className="cosmetics-v3__list">
              {cosmeticItems.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  className={`cosmetics-v3__row${selectedId === item.id || (!selectedId && item.equipped) ? ' is-active' : ''}`}
                  onClick={() => setSelectedId(item.id)}
                >
                  <span className="cosmetics-v3__icon">
                    <Sparkles size={16} strokeWidth={1.75} />
                  </span>
                  <span className="cosmetics-v3__row-name">{item.name}</span>
                  <Badge variant={RARITY_VARIANT[item.rarity]}>{item.rarity}</Badge>
                  {item.equipped && <span className="cosmetics-v3__tag">{t('common.active')}</span>}
                </button>
              ))}
            </div>
          )}

          <div className="cosmetics-v3__actions">
            {tab === 'skins' ? (
              <Button
                variant="primary"
                icon={<Check size={16} strokeWidth={1.75} />}
                disabled={
                  busy || selectedId === activeSkinId || (selectedId === null && !activeSkinId)
                }
                onClick={() => void handleApplySkin()}
              >
                {t('skins.applySkin')}
              </Button>
            ) : (
              <>
                <Button
                  variant="primary"
                  icon={<Check size={16} strokeWidth={1.75} />}
                  disabled={!selectedCosmetic || selectedCosmetic.equipped}
                  onClick={() => void handleApply()}
                >
                  {t('skins.applyCosmetic')}
                </Button>
                <Button
                  variant="ghost"
                  disabled={!selectedCosmetic?.equipped}
                  onClick={() => void handleUnequipSelected()}
                >
                  {t('actions.unequip')}
                </Button>
              </>
            )}
          </div>
        </section>
      </div>
    </PageShell>
  )
}
