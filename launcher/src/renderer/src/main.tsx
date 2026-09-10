import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import '@renderer/design-system/global.css'

function isTauriRuntime(): boolean {
  return typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window
}

async function bootstrap(): Promise<void> {
  // Keep Tauri bridge out of the Electron production bundle until needed.
  if (isTauriRuntime() && !(window as unknown as { primeLauncher?: unknown }).primeLauncher) {
    const { createTauriPrimeApi } = await import('./bridge/tauriPrimeApi')
    ;(window as unknown as { primeLauncher: ReturnType<typeof createTauriPrimeApi> }).primeLauncher =
      createTauriPrimeApi()
  }

  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <App />
    </StrictMode>
  )
}

void bootstrap()
