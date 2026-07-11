import type {
  CosmeticItem,
  DownloadTask,
  FriendEntry,
  HardwareProfile,
  MediaItem,
  ModEntry,
  PerformancePresetInfo,
  ResourcePackEntry,
  ShaderEntry,
  StoreItem
} from './content-types'
import type { FavoriteServer, GameInstance, NewsItem } from './types'

export const MOCK_INSTANCES: GameInstance[] = [
  {
    id: 'prime-fabric',
    name: 'Prime Client',
    minecraftVersion: '1.21.11',
    loader: 'fabric',
    ramMb: 4096,
    jvmArgs: ['-XX:+UseG1GC', '-XX:+ParallelRefProcEnabled'],
    modCount: 12
  },
  {
    id: 'vanilla-latest',
    name: 'Vanilla Latest',
    minecraftVersion: '1.21.11',
    loader: 'vanilla',
    ramMb: 2048,
    jvmArgs: [],
    modCount: 0
  },
  {
    id: 'modded-forge',
    name: 'Modded Adventure',
    minecraftVersion: '1.20.1',
    loader: 'forge',
    ramMb: 6144,
    jvmArgs: ['-XX:+UseG1GC'],
    modCount: 47
  }
]

export const MOCK_MODS: ModEntry[] = [
  { id: 'primeclient', fileName: 'prime-client.jar', name: 'Prime Client', description: 'Premium all-in-one Minecraft client.', version: '1.1.0', author: 'Prime Team', enabled: true, source: 'local' },
  { id: 'sodium', fileName: 'sodium.jar', name: 'Sodium', description: 'Modern rendering engine and optimization.', version: '0.6.13', author: 'JellySquid', enabled: true, source: 'modrinth' },
  { id: 'iris', fileName: 'iris.jar', name: 'Iris Shaders', description: 'Shader pack loader for Fabric.', version: '1.8.8', author: 'IMS', enabled: true, source: 'modrinth' },
  { id: 'lithium', fileName: 'lithium.jar', name: 'Lithium', description: 'General-purpose optimization mod.', version: '0.14.7', author: 'JellySquid', enabled: true, source: 'modrinth' },
  { id: 'fabric-api', fileName: 'fabric-api.jar', name: 'Fabric API', description: 'Core API for Fabric mods.', version: '0.110.5', author: 'Fabric Team', enabled: true, source: 'modrinth' },
  { id: 'journeymap', fileName: 'journeymap.jar', name: 'JourneyMap', description: 'Real-time mapping mod.', version: '6.0.0', author: 'Techbrew', enabled: false, source: 'curseforge' }
]

export const MOCK_RESOURCE_PACKS: ResourcePackEntry[] = [
  { id: 'prime-dark', fileName: 'prime-dark.zip', name: 'Prime Dark UI', description: 'Minimal dark interface pack.', resolution: '16x', active: true },
  { id: 'faithful', fileName: 'faithful.zip', name: 'Faithful 32x', description: 'Double resolution vanilla style.', resolution: '32x', active: false },
  { id: 'motschen', fileName: 'motschen-leaves.zip', name: 'Motschen\'s Better Leaves', description: 'Enhanced leaf textures.', resolution: '16x', active: false }
]

export const MOCK_SHADERS: ShaderEntry[] = [
  { id: 'complementary', fileName: 'complementary.zip', name: 'Complementary Reimagined', description: 'Beautiful balanced shaders.', backend: 'iris', active: true },
  { id: 'bsl', fileName: 'bsl.zip', name: 'BSL Shaders', description: 'Popular performance-friendly pack.', backend: 'iris', active: false },
  { id: 'seus', fileName: 'seus.zip', name: 'SEUS PTGI', description: 'Ray-traced style lighting.', backend: 'optifine', active: false }
]

export const MOCK_STORE: StoreItem[] = [
  { id: 'cape-prime', name: 'Prime Cape', description: 'Official Prime Client cape.', price: 0, category: 'cosmetic', owned: true },
  { id: 'theme-crimson', name: 'Crimson Theme', description: 'Deep red launcher theme.', price: 4.99, category: 'theme', owned: false },
  { id: 'bg-nebula', name: 'Nebula Background', description: 'Animated space background.', price: 2.99, category: 'background', owned: false },
  { id: 'badge-founder', name: 'Founder Badge', description: 'Limited edition profile badge.', price: 9.99, category: 'badge', owned: false },
  { id: 'wings-ember', name: 'Ember Wings', description: 'Fiery cosmetic wings.', price: 7.99, category: 'cosmetic', owned: false }
]

export const MOCK_COSMETICS: CosmeticItem[] = [
  { id: 'cape-prime', name: 'Prime Cape', type: 'cape', equipped: true, rarity: 'legendary' },
  { id: 'wings-ember', name: 'Ember Wings', type: 'wings', equipped: false, rarity: 'epic' },
  { id: 'pet-fox', name: 'Arctic Fox', type: 'pet', equipped: false, rarity: 'rare' },
  { id: 'emote-wave', name: 'Prime Wave', type: 'emote', equipped: false, rarity: 'common' },
  { id: 'badge-veteran', name: 'Veteran', type: 'badge', equipped: true, rarity: 'rare' }
]

export const MOCK_SERVERS: FavoriteServer[] = [
  { id: '1', name: 'Hypixel', address: 'mc.hypixel.net', players: 84231, maxPlayers: 200000, ping: 24 },
  { id: '2', name: 'Mineplex', address: 'us.mineplex.com', players: 1240, maxPlayers: 5000, ping: 45 },
  { id: '3', name: 'Prime Dev', address: 'localhost:25565', players: 0, maxPlayers: 20, ping: 0 },
  { id: '4', name: 'PvP Club', address: 'pvp.club', players: 892, maxPlayers: 3000, ping: 32 }
]

export const MOCK_FRIENDS: FriendEntry[] = [
  { id: '1', username: 'NovaCraft', status: 'in-game', activity: 'Hypixel — Bed Wars' },
  { id: '2', username: 'LunarFox', status: 'online', activity: 'In Prime Launcher' },
  { id: '3', username: 'PixelKing', status: 'away' },
  { id: '4', username: 'StormHD', status: 'offline' },
  { id: '5', username: 'EchoBlade', status: 'in-game', activity: 'Singleplayer' }
]

export const MOCK_NEWS: NewsItem[] = [
  { id: '1', title: 'Prime Client v1.1 — Premium Update', summary: 'New title screen, Discord RPC, onboarding wizard, and 50 modules shipped.', date: '2026-07-11', tag: 'update' },
  { id: '2', title: 'Prime Launcher Phase 2', summary: 'Full UI is here. Account system and game launch coming next.', date: '2026-07-11', tag: 'announcement' },
  { id: '3', title: 'Summer PvP Event', summary: 'Join official Prime scrims every weekend in July. Exclusive cosmetics for participants.', date: '2026-07-08', tag: 'event' },
  { id: '4', title: 'Cloud Sync Beta', summary: 'Sync your HUD, configs, and cosmetics across devices. Opt-in from Settings.', date: '2026-07-01', tag: 'announcement' }
]

export const MOCK_MEDIA: MediaItem[] = [
  { id: '1', type: 'screenshot', title: 'Sunset build', date: '2026-07-10', size: '2.4 MB' },
  { id: '2', type: 'screenshot', title: 'PvP clutch', date: '2026-07-09', size: '1.8 MB' },
  { id: '3', type: 'replay', title: 'Bed Wars full match', date: '2026-07-08', size: '48 MB' },
  { id: '4', type: 'clip', title: '360 no-scope', date: '2026-07-07', size: '12 MB' },
  { id: '5', type: 'replay', title: 'Speedrun attempt #12', date: '2026-07-05', size: '62 MB' }
]

export const MOCK_DOWNLOADS: DownloadTask[] = [
  { id: '1', name: 'Minecraft 1.21.11', progress: 78, speed: '24.5 MB/s', size: '420 MB', eta: '0:18', status: 'downloading' },
  { id: '2', name: 'Fabric Loader 0.16.14', progress: 100, speed: '—', size: '12 MB', eta: '—', status: 'completed' },
  { id: '3', name: 'Prime Client 1.1.0', progress: 45, speed: '8.2 MB/s', size: '8 MB', eta: '0:02', status: 'downloading' },
  { id: '4', name: 'Complementary Shaders', progress: 0, speed: '—', size: '35 MB', eta: '—', status: 'queued' }
]

export const MOCK_HARDWARE: HardwareProfile = {
  cpu: 'AMD Ryzen 7 7800X3D',
  gpu: 'NVIDIA GeForce RTX 4070',
  ramGb: 32
}

export const PERFORMANCE_PRESETS: PerformancePresetInfo[] = [
  { id: 'low', label: 'Low PC', ramMb: 2048, renderDistance: 8, description: 'Minimum settings for weak hardware.' },
  { id: 'balanced', label: 'Balanced', ramMb: 4096, renderDistance: 12, description: 'Recommended for most players.' },
  { id: 'performance', label: 'Performance', ramMb: 6144, renderDistance: 16, description: 'High FPS competitive setup.' },
  { id: 'ultra', label: 'Ultra', ramMb: 8192, renderDistance: 24, description: 'Maximum quality for powerful PCs.' }
]
