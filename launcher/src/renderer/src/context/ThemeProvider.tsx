import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import type { StoreItem } from '@shared/content-types'
import type { PrimeThemeId } from '@shared/ipc'
import { normalizePrimeTheme } from '@shared/theme'
import { setUiSoundsEnabled } from '@renderer/lib/uiSounds'

interface ThemeContextValue {
  refreshTheme: () => Promise<void>
  /** User setting — lighter UI for low-end PCs. */
  performanceMode: boolean
  /** True when performance mode OR OS prefers-reduced-motion. */
  reduceMotion: boolean
}

const ThemeContext = createContext<ThemeContextValue | null>(null)

function applyReduceMotionFlag(reduce: boolean): void {
  document.documentElement.dataset.reduceMotion = reduce ? 'true' : 'false'
}

async function applyThemeFromSettings(): Promise<{ performanceMode: boolean }> {
  const [settings, catalog, wallpaperData] = await Promise.all([
    window.primeLauncher.settings.get(),
    window.primeLauncher.store.catalog(),
    window.primeLauncher.settings.wallpaperData()
  ])

  const ownsNebula = catalog.some((item: StoreItem) => item.id === 'bg-nebula' && item.owned)

  const theme: PrimeThemeId = normalizePrimeTheme(settings.theme)
  document.documentElement.dataset.theme = theme
  document.documentElement.dataset.background =
    ownsNebula && settings.backgroundNebula && !settings.performanceMode ? 'nebula' : 'default'

  const root = document.documentElement
  if (settings.accentColor) {
    // Custom accent overrides brand tokens consistently (not only --prime-red-bright).
    const accent = settings.accentColor
    root.style.setProperty('--prime-accent-override', accent)
    root.style.setProperty('--prime-red', accent)
    root.style.setProperty('--prime-red-bright', accent)
    root.style.setProperty('--prime-red-glow', `color-mix(in srgb, ${accent} 45%, transparent)`)
    root.style.setProperty('--prime-red-subtle', `color-mix(in srgb, ${accent} 14%, transparent)`)
  } else {
    root.style.removeProperty('--prime-accent-override')
    root.style.removeProperty('--prime-red')
    root.style.removeProperty('--prime-red-bright')
    root.style.removeProperty('--prime-red-glow')
    root.style.removeProperty('--prime-red-subtle')
  }

  if (wallpaperData) {
    root.style.setProperty('--prime-wallpaper', `url("${wallpaperData}")`)
    root.dataset.wallpaper = 'custom'
  } else {
    root.style.removeProperty('--prime-wallpaper')
    delete root.dataset.wallpaper
  }

  setUiSoundsEnabled(settings.uiSounds !== false && !settings.performanceMode)

  return { performanceMode: Boolean(settings.performanceMode) }
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [performanceMode, setPerformanceMode] = useState(false)
  const [osReducedMotion, setOsReducedMotion] = useState(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return false
    return window.matchMedia('(prefers-reduced-motion: reduce)').matches
  })

  const refreshTheme = useCallback(async () => {
    const { performanceMode: mode } = await applyThemeFromSettings()
    setPerformanceMode(mode)
  }, [])

  useEffect(() => {
    void refreshTheme()
  }, [refreshTheme])

  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return
    const mq = window.matchMedia('(prefers-reduced-motion: reduce)')
    const onChange = (): void => setOsReducedMotion(mq.matches)
    mq.addEventListener('change', onChange)
    return () => mq.removeEventListener('change', onChange)
  }, [])

  const reduceMotion = performanceMode || osReducedMotion

  useEffect(() => {
    applyReduceMotionFlag(reduceMotion)
  }, [reduceMotion])

  const value = useMemo(
    () => ({ refreshTheme, performanceMode, reduceMotion }),
    [refreshTheme, performanceMode, reduceMotion]
  )

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext)
  if (!ctx) {
    throw new Error('useTheme must be used within ThemeProvider')
  }
  return ctx
}
