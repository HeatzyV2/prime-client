/// <reference types="vite/client" />

import type { PrimeLauncherApi } from './bridge/tauriPrimeApi'

declare global {
  interface Window {
    primeLauncher: PrimeLauncherApi
  }
}

export {}
