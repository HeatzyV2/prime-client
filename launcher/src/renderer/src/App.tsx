import { useEffect, useState } from 'react'
import { AnimatePresence } from 'framer-motion'
import { HashRouter } from 'react-router-dom'
import { AppShell } from '@renderer/layouts/AppShell'
import { SplashScreen } from '@renderer/pages/SplashScreen'
import { AppRoutes } from '@renderer/routes/AppRoutes'
import { AccountProvider } from '@renderer/context/AccountProvider'
import { useBootSequence } from '@renderer/hooks/useBootSequence'
import type { FavoriteServer, NewsItem } from '@shared/types'

function LauncherApp() {
  const [news, setNews] = useState<NewsItem[]>([])
  const [servers, setServers] = useState<FavoriteServer[]>([])
  const [ready, setReady] = useState(false)

  useEffect(() => {
    void (async () => {
      const [newsItems, favServers] = await Promise.all([
        window.primeLauncher.minecraft.getNews(),
        window.primeLauncher.minecraft.getFavoriteServers()
      ])
      setNews(newsItems)
      setServers(favServers)
      setReady(true)
    })()
  }, [])

  if (!ready) {
    return null
  }

  return (
    <HashRouter>
      <AppShell>
        <AppRoutes news={news} servers={servers} />
      </AppShell>
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
    <AccountProvider>
      <AnimatePresence>{booting && <SplashScreen progress={progress} stepIndex={stepIndex} version={version} />}</AnimatePresence>
      {!booting && <LauncherApp />}
    </AccountProvider>
  )
}
