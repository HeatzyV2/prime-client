import { Routes, Route, Navigate } from 'react-router-dom'
import { DashboardPage } from '@renderer/pages/DashboardPage'
import { AccountsPage } from '@renderer/pages/AccountsPage'
import { InstancesPage } from '@renderer/pages/InstancesPage'
import { ModsPage } from '@renderer/pages/ModsPage'
import { ResourcesPage } from '@renderer/pages/ResourcesPage'
import { ShadersPage } from '@renderer/pages/ShadersPage'
import { StorePage } from '@renderer/pages/StorePage'
import { CosmeticsPage } from '@renderer/pages/CosmeticsPage'
import { ServersPage } from '@renderer/pages/ServersPage'
import { FriendsPage } from '@renderer/pages/FriendsPage'
import { NewsPage } from '@renderer/pages/NewsPage'
import { MediaPage } from '@renderer/pages/MediaPage'
import { PerformancePage } from '@renderer/pages/PerformancePage'
import { DownloadsPage } from '@renderer/pages/DownloadsPage'
import { ConsolePage } from '@renderer/pages/ConsolePage'
import { SettingsPage } from '@renderer/pages/SettingsPage'
import type { FavoriteServer, NewsItem } from '@shared/types'

interface AppRoutesProps {
  news: NewsItem[]
  servers: FavoriteServer[]
}

export function AppRoutes({ news, servers }: AppRoutesProps) {
  return (
    <Routes>
      <Route index element={<DashboardPage news={news} servers={servers} />} />
      <Route path="accounts" element={<AccountsPage />} />
      <Route path="instances" element={<InstancesPage />} />
      <Route path="mods" element={<ModsPage />} />
      <Route path="resources" element={<ResourcesPage />} />
      <Route path="shaders" element={<ShadersPage />} />
      <Route path="store" element={<StorePage />} />
      <Route path="cosmetics" element={<CosmeticsPage />} />
      <Route path="servers" element={<ServersPage />} />
      <Route path="friends" element={<FriendsPage />} />
      <Route path="news" element={<NewsPage />} />
      <Route path="media" element={<MediaPage />} />
      <Route path="performance" element={<PerformancePage />} />
      <Route path="downloads" element={<DownloadsPage />} />
      <Route path="console" element={<ConsolePage />} />
      <Route path="settings" element={<SettingsPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
