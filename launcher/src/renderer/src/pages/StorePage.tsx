import { useCallback, useEffect, useState } from 'react'
import { ShoppingBag } from 'lucide-react'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Button, Tabs } from '@renderer/design-system/components'
import type { StoreItem } from '@shared/content-types'

const CATEGORIES = [
  { id: 'all', label: 'All' },
  { id: 'cosmetic', label: 'Cosmetics' },
  { id: 'theme', label: 'Themes' },
  { id: 'background', label: 'Backgrounds' },
  { id: 'badge', label: 'Badges' }
]

export function StorePage() {
  const [category, setCategory] = useState('all')
  const [items, setItems] = useState<StoreItem[]>([])
  const [balance, setBalance] = useState(0)
  const [message, setMessage] = useState<string | null>(null)

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
      setMessage(`${item.name} unlocked! Equip cosmetics from the Cosmetics page.`)
      await refresh()
    } else {
      setMessage(result.error ?? 'Purchase failed.')
    }
  }

  const filtered = items.filter((i) => category === 'all' || i.category === category)

  return (
    <PageShell
      title="Prime Store"
      subtitle="Local storefront — spend Prime Coins (no real payments, no server)."
      actions={<Badge variant="prime">{balance} Prime Coins</Badge>}
    >
      {message && (
        <p className="text-caption" style={{ marginBottom: 16, color: 'var(--prime-muted)' }}>
          {message}
        </p>
      )}

      <Tabs tabs={CATEGORIES} active={category} onChange={setCategory} />

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
                <Badge variant="success">Owned</Badge>
              ) : (
                <span style={{ fontWeight: 700, color: 'var(--prime-red-bright)' }}>
                  {item.price === 0 ? 'Free' : `${item.price} coins`}
                </span>
              )}
              <Button
                variant={item.owned ? 'secondary' : 'primary'}
                size="sm"
                disabled={item.owned}
                onClick={() => void handlePurchase(item)}
              >
                {item.owned ? 'Owned' : item.price === 0 ? 'Claim' : 'Buy'}
              </Button>
            </div>
          </div>
        ))}
      </div>
    </PageShell>
  )
}
