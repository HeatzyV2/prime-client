import { Suspense, lazy, useCallback, useEffect, useMemo, useState } from 'react'
import { ShoppingBag, Ticket } from 'lucide-react'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Button, Tabs } from '@renderer/design-system/components'
import type { StoreItem } from '@shared/content-types'
import { playerCapeUrl } from '@shared/format'
import { useI18n } from '@renderer/context/I18nProvider'
import { useTheme } from '@renderer/context/ThemeProvider'
import { useAccounts } from '@renderer/context/AccountProvider'
import { themeIdFromStoreId } from '@shared/theme'
import { EmptyState } from '@renderer/components/EmptyState'
import { playUiSound } from '@renderer/lib/uiSounds'
import './StorePage.css'

const SkinViewer3D = lazy(() =>
  import('@renderer/components/SkinViewer3D').then((m) => ({ default: m.SkinViewer3D }))
)

type StoreTab = 'catalog' | 'history' | 'promos'

interface HistoryRow {
  id: string
  itemId: string
  itemName: string
  price: number
  purchasedAt: string
}

interface PromoRow {
  code: string
  label: string
  coins: number
  redeemed: boolean
}

export function StorePage() {
  const { t } = useI18n()
  const { refreshTheme } = useTheme()
  const { activeAccount } = useAccounts()
  const [tab, setTab] = useState<StoreTab>('catalog')
  const [category, setCategory] = useState('all')
  const [items, setItems] = useState<StoreItem[]>([])
  const [history, setHistory] = useState<HistoryRow[]>([])
  const [promos, setPromos] = useState<PromoRow[]>([])
  const [promoCode, setPromoCode] = useState('')
  const [balance, setBalance] = useState(0)
  const [syncMode, setSyncMode] = useState<'synced' | 'local'>('local')
  const [message, setMessage] = useState<string | null>(null)
  const [selectedId, setSelectedId] = useState<string | null>(null)

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

  const mainTabs = useMemo(
    () => [
      { id: 'catalog', label: t('store.tabs.catalog') },
      { id: 'history', label: t('store.tabs.history') },
      { id: 'promos', label: t('store.tabs.promos') }
    ],
    [t]
  )

  const refresh = useCallback(async () => {
    const [catalog, coins, hist, promoList] = await Promise.all([
      window.primeLauncher.store.catalog(),
      window.primeLauncher.store.balance(),
      window.primeLauncher.store.history(),
      window.primeLauncher.store.promos()
    ])
    const mode = await window.primeLauncher.store.syncMode()
    setItems(catalog)
    setBalance(coins)
    setHistory(hist)
    setPromos(promoList)
    setSyncMode(mode)
    setSelectedId((prev) => prev ?? catalog[0]?.id ?? null)
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
    } else {
      playUiSound('error')
      setMessage(result.error ?? t('store.purchaseFailed'))
    }
  }

  async function handleRedeem() {
    setMessage(null)
    const result = await window.primeLauncher.store.redeem(promoCode)
    if (result.ok) {
      playUiSound('success')
      setMessage(t('store.promoOk', { coins: result.coins ?? 0 }))
      setPromoCode('')
      await refresh()
    } else {
      playUiSound('error')
      setMessage(result.error ?? t('store.promoFailed'))
    }
  }

  const filtered = items.filter((i) => category === 'all' || i.category === category)
  const selected = filtered.find((i) => i.id === selectedId) ?? filtered[0] ?? null
  const username = activeAccount?.username ?? 'Steve'
  const capeUrl = playerCapeUrl(activeAccount?.uuid, username, activeAccount?.capeUrl)
  const showViewer = selected?.category === 'cosmetic'

  return (
    <PageShell
      title={t('pages.store.title')}
      subtitle={t('pages.store.subtitle')}
      actions={
        <div className="store-v3__header-actions">
          <Badge variant={syncMode === 'synced' ? 'success' : 'default'}>
            {syncMode === 'synced' ? t('store.syncSynced') : t('store.syncLocal')}
          </Badge>
          <Badge variant="prime">{t('store.coins', { balance })}</Badge>
        </div>
      }
    >
      <div className="store-v3">
        {message && (
          <p className="store-v3__message" role="status">
            {message}
          </p>
        )}

        <Tabs tabs={mainTabs} active={tab} onChange={(id) => setTab(id as StoreTab)} />

        {tab === 'catalog' && (
          <>
            <Tabs tabs={categories} active={category} onChange={setCategory} />
            {filtered.length === 0 ? (
              <EmptyState
                icon={<ShoppingBag size={22} strokeWidth={1.75} />}
                title={t('store.emptyTitle')}
                description={t('store.emptyDesc')}
              />
            ) : (
              <div className="store-v3__catalog">
                <div className="store-v3__list" role="listbox" aria-label={t('store.tabs.catalog')}>
                  {filtered.map((item) => {
                    const active = item.id === selected?.id
                    return (
                      <button
                        key={item.id}
                        type="button"
                        role="option"
                        aria-selected={active}
                        className={`store-v3__row${active ? ' is-active' : ''}`}
                        onClick={() => setSelectedId(item.id)}
                      >
                        <span>
                          <span className="store-v3__row-name">{item.name}</span>
                          <span className="store-v3__row-desc">{item.description}</span>
                        </span>
                        <span className="store-v3__row-meta">
                          {item.owned ? (
                            <Badge variant="success">{t('actions.owned')}</Badge>
                          ) : (
                            <span className="store-v3__price">
                              {item.price === 0
                                ? t('actions.free')
                                : t('store.coinsPrice', { price: item.price })}
                            </span>
                          )}
                        </span>
                      </button>
                    )
                  })}
                </div>

                {selected && (
                  <div className="store-v3__detail v3-fade-in">
                    <p className="text-label">{selected.category}</p>
                    <h2 className="store-v3__detail-title">{selected.name}</h2>
                    <p className="store-v3__detail-desc">{selected.description}</p>

                    {showViewer && (
                      <div className="store-v3__viewer-wrap">
                        <Suspense fallback={<div className="store-v3__viewer-fallback" aria-hidden />}>
                          <SkinViewer3D
                            uuid={activeAccount?.uuid}
                            username={username}
                            capeUrl={capeUrl}
                            pose="idle"
                            width={180}
                            height={240}
                            showControls={false}
                          />
                        </Suspense>
                      </div>
                    )}

                    <div className="store-v3__detail-actions">
                      {selected.owned ? (
                        <Badge variant="success">{t('actions.owned')}</Badge>
                      ) : (
                        <Button variant="primary" onClick={() => void handlePurchase(selected)}>
                          {selected.price === 0
                            ? t('actions.claim')
                            : t('store.buyFor', { price: selected.price })}
                        </Button>
                      )}
                    </div>
                  </div>
                )}
              </div>
            )}
          </>
        )}

        {tab === 'history' && (
          <div className="page-list">
            {history.length === 0 ? (
              <EmptyState
                icon={<ShoppingBag size={22} strokeWidth={1.75} />}
                title={t('store.historyEmptyTitle')}
                description={t('store.historyEmptyDesc')}
              />
            ) : (
              history.map((row) => (
                <div key={row.id} className="list-row">
                  <div className="list-row__body">
                    <div className="list-row__title">{row.itemName}</div>
                    <div className="list-row__desc">{new Date(row.purchasedAt).toLocaleString()}</div>
                  </div>
                  <div className="list-row__meta text-mono">
                    {row.price === 0 ? t('actions.free') : t('store.coinsPrice', { price: row.price })}
                  </div>
                </div>
              ))
            )}
          </div>
        )}

        {tab === 'promos' && (
          <div className="store-v3__promos">
            <div className="store-v3__promo-row">
              <input
                className="store-v3__promo-input"
                placeholder={t('store.promoPlaceholder')}
                value={promoCode}
                onChange={(e) => setPromoCode(e.target.value)}
                aria-label={t('store.promoPlaceholder')}
              />
              <Button
                variant="primary"
                icon={<Ticket size={16} strokeWidth={1.75} />}
                disabled={promoCode.trim().length < 3}
                onClick={() => void handleRedeem()}
              >
                {t('store.redeem')}
              </Button>
            </div>
            <div className="page-list">
              {promos.map((p) => (
                <div key={p.code} className="list-row">
                  <div className="list-row__body">
                    <div className="list-row__title">{p.label}</div>
                    <div className="list-row__desc text-mono">{p.code}</div>
                  </div>
                  <div className="list-row__meta">
                    <Badge variant={p.redeemed ? 'success' : 'prime'}>
                      {p.redeemed ? t('store.redeemed') : t('store.coinsPrice', { price: p.coins })}
                    </Badge>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </PageShell>
  )
}
