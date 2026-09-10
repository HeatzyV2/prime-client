import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode
} from 'react'
import type { PrimeThemeId } from '@shared/ipc'
import { normalizePrimeTheme } from '@shared/theme'
import { setUiSoundsEnabled } from '@renderer/lib/uiSounds'

interface ThemeContextValue {
  refreshTheme: () => Promise<void>
  /** Apply theme CSS immediately (before IPC round-trip). */
  applyThemeId: (theme: PrimeThemeId) => void
  /** User setting — lighter UI for low-end PCs. */
  performanceMode: boolean
  /** True when performance mode OR OS prefers-reduced-motion. */
  reduceMotion: boolean
}

const ThemeContext = createContext<ThemeContextValue | null>(null)

function applyReduceMotionFlag(reduce: boolean): void {
  document.documentElement.dataset.reduceMotion = reduce ? 'true' : 'false'
}

function clearAccentOverrides(root: HTMLElement): void {
  root.style.removeProperty('--prime-accent-override')
  root.style.removeProperty('--accent')
  root.style.removeProperty('--accent-bright')
  root.style.removeProperty('--accent-subtle')
  root.style.removeProperty('--accent-ring')
  root.style.removeProperty('--prime-red')
  root.style.removeProperty('--prime-red-bright')
  root.style.removeProperty('--prime-red-glow')
  root.style.removeProperty('--prime-red-subtle')
}

function applyAccentOverride(root: HTMLElement, accent: string): void {
  root.style.setProperty('--prime-accent-override', accent)
  root.style.setProperty('--accent', accent)
  root.style.setProperty('--accent-bright', accent)
  root.style.setProperty('--accent-subtle', `color-mix(in srgb, ${accent} 14%, transparent)`)
  root.style.setProperty('--accent-ring', `color-mix(in srgb, ${accent} 35%, transparent)`)
  root.style.setProperty('--prime-red', accent)
  root.style.setProperty('--prime-red-bright', accent)
  root.style.setProperty('--prime-red-glow', `color-mix(in srgb, ${accent} 45%, transparent)`)
  root.style.setProperty('--prime-red-subtle', `color-mix(in srgb, ${accent} 14%, transparent)`)
}

function applyThemeIdSync(theme: PrimeThemeId, clearAccent = true): void {
  const root = document.documentElement
  root.dataset.theme = normalizePrimeTheme(theme)
  if (clearAccent) clearAccentOverrides(root)
}

function applyWallpaper(dataUrl: string | null): void {
  const root = document.documentElement
  if (dataUrl) {
    // Escape quotes in case of odd data URLs
    const safe = dataUrl.replace(/\\/g, '\\\\').replace(/"/g, '\\"')
    root.style.setProperty('--prime-wallpaper', `url("${safe}")`)
    root.dataset.wallpaper = 'custom'
  } else {
    root.style.removeProperty('--prime-wallpaper')
    delete root.dataset.wallpaper
  }
}

async function applyThemeFromSettings(): Promise<{ performanceMode: boolean }> {
  // Theme first — never block identity on catalog / wallpaper I/O.
  const settings = await window.primeLauncher.settings.get()
  const theme = normalizePrimeTheme(settings.theme)
  applyThemeIdSync(theme, !settings.accentColor)

  if (settings.accentColor) {
    applyAccentOverride(document.documentElement, settings.accentColor)
  }

  setUiSoundsEnabled(settings.uiSounds !== false && !settings.performanceMode)

  // Nebula is a free appearance toggle — apply immediately (not gated on store catalog).
  document.documentElement.dataset.background =
    settings.backgroundNebula && !settings.performanceMode ? 'nebula' : 'default'

  // Secondary chrome — tolerate failures (cache / network / missing file).
  void (async () => {
    try {
      const wallpaperData = await window.primeLauncher.settings.wallpaperData().catch(() => null)
      applyWallpaper(wallpaperData)
    } catch {
      /* ignore wallpaper load failures */
    }
  })()

  return { performanceMode: Boolean(settings.performanceMode) }
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [performanceMode, setPerformanceMode] = useState(false)
  const [osReducedMotion, setOsReducedMotion] = useState(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return false
    return window.matchMedia('(prefers-reduced-motion: reduce)').matches
  })

  const applyThemeId = useCallback((theme: PrimeThemeId) => {
    applyThemeIdSync(theme, true)
  }, [])

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
    () => ({ refreshTheme, applyThemeId, performanceMode, reduceMotion }),
    [refreshTheme, applyThemeId, performanceMode, reduceMotion]
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
