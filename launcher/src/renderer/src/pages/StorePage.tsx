import { useCallback, useEffect, useMemo, useState } from 'react'
import { ShoppingBag, X } from 'lucide-react'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Button, Tabs } from '@renderer/design-system/components'
import type { StoreItem } from '@shared/content-types'
import { playerCapeUrl } from '@shared/format'
import { useI18n } from '@renderer/context/I18nProvider'
import { useTheme } from '@renderer/context/ThemeProvider'
import { useAccounts } from '@renderer/context/AccountProvider'
import { themeIdFromStoreId } from '@shared/theme'
import { SkinViewer3D } from '@renderer/components/SkinViewer3D'
import { EmptyState } from '@renderer/components/EmptyState'
import { playUiSound } from '@renderer/lib/uiSounds'
import './StorePage.css'

export function StorePage() {
  const { t } = useI18n()
  const { refreshTheme } = useTheme()
  const { activeAccount } = useAccounts()
  const [category, setCategory] = useState('all')
  const [items, setItems] = useState<StoreItem[]>([])
  const [balance, setBalance] = useState(0)
  const [message, setMessage] = useState<string | null>(null)
  const [preview, setPreview] = useState<StoreItem | null>(null)

  const categories = useMemo(
    () => [
      { id: 'all', label: t('store.categories.all') },
      { id: 'cosmetic', label: t('store.categories.cosmetic') },
      { id: 'theme', label: t('store.categories.theme') },
      { id: 'background', label: t('store.categories.background') },
      { id: 'badge', label: t('store.categories.badge') }
    ],
    [t]
  )

  const refresh = useCallback(async () => {
    const [catalog, coins] = await Promise.all([
      window.primeLauncher.store.catalog(),
      window.primeLauncher.store.balance()
    ])
    setItems(catalog)
    setBalance(coins)
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  async function handlePurchase(item: StoreItem) {
    setMessage(null)
    const result = await window.primeLauncher.store.purchase(item.id)
    if (result.ok) {
      playUiSound('success')
      setMessage(t('store.unlocked', { name: item.name }))
      if (item.category === 'theme') {
        const themeId = themeIdFromStoreId(item.id)
        if (themeId) {
          await window.primeLauncher.settings.update({ theme: themeId, accentColor: null })
        }
        await refreshTheme()
      }
      if (item.id === 'bg-nebula') {
        await window.primeLauncher.settings.update({ backgroundNebula: true })
        await refreshTheme()
      }
      await refresh()
      setPreview(null)
    } else {
      playUiSound('error')
      setMessage(result.error ?? t('store.purchaseFailed'))
    }
  }

  const filtered = items.filter((i) => category === 'all' || i.category === category)
  const username = activeAccount?.username ?? 'Steve'
  const capeUrl = playerCapeUrl(activeAccount?.uuid, username, activeAccount?.capeUrl)

  return (
    <PageShell
      title={t('pages.store.title')}
      subtitle={t('pages.store.subtitle')}
      actions={<Badge variant="prime">{t('store.coins', { balance })}</Badge>}
    >
      {message && (
        <p className="text-caption" style={{ marginBottom: 16, color: 'var(--prime-muted)' }}>
          {message}
        </p>
      )}

      <Tabs tabs={categories} active={category} onChange={setCategory} />

      {filtered.length === 0 ? (
        <EmptyState
          icon={<ShoppingBag size={24} />}
          title={t('store.emptyTitle')}
          description={t('store.emptyDesc')}
        />
      ) : (
        <div className="page-grid page-grid--3" style={{ marginTop: 24 }}>
          {filtered.map((item) => (
            <div key={item.id} className="tile">
              <button type="button" className="tile__preview store-tile__preview" onClick={() => setPreview(item)}>
                <ShoppingBag size={28} />
                <span>{t('store.preview')}</span>
              </button>
              <div className="tile__name">{item.name}</div>
              <div className="tile__desc">{item.description}</div>
              <div style={{ marginTop: 16, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                {item.owned ? (
                  <Badge variant="success">{t('actions.owned')}</Badge>
                ) : (
                  <span style={{ fontWeight: 700, color: 'var(--prime-red-bright)' }}>
                    {item.price === 0 ? t('actions.free') : t('store.coinsPrice', { price: item.price })}
                  </span>
                )}
                <Button
                  variant={item.owned ? 'secondary' : 'primary'}
                  size="sm"
                  disabled={item.owned}
                  onClick={() => void handlePurchase(item)}
                >
                  {item.owned
                    ? t('actions.owned')
                    : item.price === 0
                      ? t('actions.claim')
                      : t('actions.buy')}
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      {preview && (
        <div className="store-preview">
          <button type="button" className="store-preview__backdrop" onClick={() => setPreview(null)} />
          <div className="store-preview__card">
            <button type="button" className="store-preview__close" onClick={() => setPreview(null)}>
              <X size={16} />
            </button>
            <SkinViewer3D
              uuid={activeAccount?.uuid}
              username={username}
              capeUrl={preview.category === 'cosmetic' ? capeUrl : capeUrl}
              pose="walk"
              width={240}
              height={320}
              showControls
              backdrop="soft"
            />
            <div className="store-preview__meta">
              <h3>{preview.name}</h3>
              <p>{preview.description}</p>
              <div className="store-preview__actions">
                {preview.owned ? (
                  <Badge variant="success">{t('actions.owned')}</Badge>
                ) : (
                  <Button variant="primary" onClick={() => void handlePurchase(preview)}>
                    {preview.price === 0
                      ? t('actions.claim')
                      : t('store.buyFor', { price: preview.price })}
                  </Button>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </PageShell>
  )
}
