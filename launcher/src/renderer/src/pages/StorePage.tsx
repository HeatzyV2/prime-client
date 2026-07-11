import { useCallback, useEffect, useMemo, useState } from 'react'
import { ShoppingBag } from 'lucide-react'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Button, Tabs } from '@renderer/design-system/components'
import type { StoreItem } from '@shared/content-types'
import { useI18n } from '@renderer/context/I18nProvider'
import { useTheme } from '@renderer/context/ThemeProvider'

export function StorePage() {
  const { t } = useI18n()
  const { refreshTheme } = useTheme()
  const [category, setCategory] = useState('all')
  const [items, setItems] = useState<StoreItem[]>([])
  const [balance, setBalance] = useState(0)
  const [message, setMessage] = useState<string | null>(null)

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
      setMessage(t('store.unlocked', { name: item.name }))
      if (item.id === 'theme-crimson') {
        await refreshTheme()
      }
      if (item.id === 'bg-nebula') {
        await window.primeLauncher.settings.update({ backgroundNebula: true })
        await refreshTheme()
      }
      await refresh()
    } else {
      setMessage(result.error ?? t('store.purchaseFailed'))
    }
  }

  const filtered = items.filter((i) => category === 'all' || i.category === category)

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

      <div className="page-grid page-grid--3" style={{ marginTop: 24 }}>
        {filtered.map((item) => (
          <div key={item.id} className="tile">
            <div className="tile__preview">
              <ShoppingBag size={28} />
            </div>
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
    </PageShell>
  )
}
