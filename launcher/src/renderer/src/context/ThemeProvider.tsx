import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import type { StoreItem } from '@shared/content-types'
import type { PrimeThemeId } from '@shared/ipc'
import { normalizePrimeTheme } from '@shared/theme'

interface ThemeContextValue {
  refreshTheme: () => Promise<void>
}

const ThemeContext = createContext<ThemeContextValue | null>(null)

async function applyThemeFromSettings(): Promise<void> {
  const [settings, catalog] = await Promise.all([
    window.primeLauncher.settings.get(),
    window.primeLauncher.store.catalog()
  ])

  const ownsNebula = catalog.some((item: StoreItem) => item.id === 'bg-nebula' && item.owned)

  const theme: PrimeThemeId = normalizePrimeTheme(settings.theme)
  document.documentElement.dataset.theme = theme
  document.documentElement.dataset.background =
    ownsNebula && settings.backgroundNebula ? 'nebula' : 'default'
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [, setTick] = useState(0)

  const refreshTheme = useCallback(async () => {
    await applyThemeFromSettings()
    setTick((n) => n + 1)
  }, [])

  useEffect(() => {
    void refreshTheme()
  }, [refreshTheme])

  const value = useMemo(() => ({ refreshTheme }), [refreshTheme])

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext)
  if (!ctx) {
    throw new Error('useTheme must be used within ThemeProvider')
  }
  return ctx
}
