import { useEffect, useState } from 'react'
import { AnimatePresence } from 'framer-motion'
import { HashRouter } from 'react-router-dom'
import { V3Shell } from '@renderer/layouts/V3Shell'
import { SplashScreen } from '@renderer/pages/SplashScreen'
import { AppRoutes } from '@renderer/routes/AppRoutes'
import { AccountProvider } from '@renderer/context/AccountProvider'
import { I18nProvider } from '@renderer/context/I18nProvider'
import { ThemeProvider } from '@renderer/context/ThemeProvider'
import { ConfirmProvider, ToastProvider } from '@renderer/design-system/components'
import { useBootSequence } from '@renderer/hooks/useBootSequence'
import type { FavoriteServer } from '@shared/types'

function LauncherApp() {
  const [servers, setServers] = useState<FavoriteServer[]>([])
  const [ready, setReady] = useState(false)

  useEffect(() => {
    void (async () => {
      try {
        setServers(await window.primeLauncher.servers.list())
      } catch {
        setServers([])
      } finally {
        setReady(true)
      }
    })()
  }, [])

  if (!ready) {
    return null
  }

  return (
    <HashRouter>
      <V3Shell>
        <AppRoutes servers={servers} />
      </V3Shell>
    </HashRouter>
  )
}

export default function App() {
  const { booting, stepIndex, progress } = useBootSequence()
  const [version, setVersion] = useState('0.2.0')

  useEffect(() => {
    void window.primeLauncher.app.getVersion().then(setVersion)
  }, [])

  return (
    <I18nProvider>
      <ThemeProvider>
        <AccountProvider>
          <ToastProvider>
            <ConfirmProvider>
              <AnimatePresence>
                {booting && (
                  <SplashScreen progress={progress} stepIndex={stepIndex} version={version} />
                )}
              </AnimatePresence>
              {!booting && <LauncherApp />}
            </ConfirmProvider>
          </ToastProvider>
        </AccountProvider>
      </ThemeProvider>
    </I18nProvider>
  )
}
