import { Suspense, lazy, type ComponentType, type ReactNode } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { DashboardPage } from '@renderer/pages/DashboardPage'
import type { FavoriteServer, NewsItem } from '@shared/types'

interface AppRoutesProps {
  news: NewsItem[]
  servers: FavoriteServer[]
}

function lazyNamed<T extends Record<string, ComponentType<any>>>(
  loader: () => Promise<T>,
  exportName: keyof T
) {
  return lazy(async () => {
    const mod = await loader()
    return { default: mod[exportName] as ComponentType<any> }
  })
}

const AccountsPage = lazyNamed(() => import('@renderer/pages/AccountsPage'), 'AccountsPage')
const ProfilePage = lazyNamed(() => import('@renderer/pages/ProfilePage'), 'ProfilePage')
const InstancesPage = lazyNamed(() => import('@renderer/pages/InstancesPage'), 'InstancesPage')
const SkinsPage = lazyNamed(() => import('@renderer/pages/SkinsPage'), 'SkinsPage')
const LibraryPage = lazyNamed(() => import('@renderer/pages/LibraryPage'), 'LibraryPage')
const ModsPage = lazyNamed(() => import('@renderer/pages/ModsPage'), 'ModsPage')
const ResourcesPage = lazyNamed(() => import('@renderer/pages/ResourcesPage'), 'ResourcesPage')
const ShadersPage = lazyNamed(() => import('@renderer/pages/ShadersPage'), 'ShadersPage')
const StorePage = lazyNamed(() => import('@renderer/pages/StorePage'), 'StorePage')
const ServersPage = lazyNamed(() => import('@renderer/pages/ServersPage'), 'ServersPage')
const FriendsPage = lazyNamed(() => import('@renderer/pages/FriendsPage'), 'FriendsPage')
const ChatPage = lazyNamed(() => import('@renderer/pages/ChatPage'), 'ChatPage')
const NewsPage = lazyNamed(() => import('@renderer/pages/NewsPage'), 'NewsPage')
const MediaPage = lazyNamed(() => import('@renderer/pages/MediaPage'), 'MediaPage')
const PerformancePage = lazyNamed(() => import('@renderer/pages/PerformancePage'), 'PerformancePage')
const DownloadsPage = lazyNamed(() => import('@renderer/pages/DownloadsPage'), 'DownloadsPage')
const ConsolePage = lazyNamed(() => import('@renderer/pages/ConsolePage'), 'ConsolePage')
const SettingsPage = lazyNamed(() => import('@renderer/pages/SettingsPage'), 'SettingsPage')

function RouteFallback(): ReactNode {
  return <div className="page-shell" style={{ opacity: 0.5, padding: 24 }} aria-busy="true" />
}

export function AppRoutes({ news, servers }: AppRoutesProps) {
  return (
    <Suspense fallback={<RouteFallback />}>
      <Routes>
        <Route index element={<DashboardPage news={news} servers={servers} />} />
        <Route path="accounts" element={<AccountsPage />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="instances" element={<InstancesPage />} />
        <Route path="skins" element={<SkinsPage />} />
        <Route path="library" element={<LibraryPage />} />
        <Route path="mods" element={<ModsPage />} />
        <Route path="resources" element={<ResourcesPage />} />
        <Route path="shaders" element={<ShadersPage />} />
        <Route path="store" element={<StorePage />} />
        <Route path="cosmetics" element={<Navigate to="/skins" replace />} />
        <Route path="servers" element={<ServersPage />} />
        <Route path="friends" element={<FriendsPage />} />
        <Route path="chat" element={<ChatPage />} />
        <Route path="news" element={<NewsPage />} />
        <Route path="media" element={<MediaPage />} />
        <Route path="performance" element={<PerformancePage />} />
        <Route path="downloads" element={<DownloadsPage />} />
        <Route path="console" element={<ConsolePage />} />
        <Route path="settings" element={<SettingsPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  )
}
