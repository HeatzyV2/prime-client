# Prime Launcher

Official launcher for **Prime Client** — premium Minecraft platform.

> Releases & updates via [GitHub](https://github.com/HeatzyV2/prime-client) — see [docs/GITHUB.md](../docs/GITHUB.md)

## Stack

| Layer | Tech |
|-------|------|
| Shell | Tauri 2 (Rust) |
| UI | React 19 + TypeScript |
| Routing | React Router 7 |
| Motion | Framer Motion |
| Icons | Lucide React |
| Build | Vite 7 + `@tauri-apps/cli` |

## Structure

```
launcher/
├── src/
│   ├── shared/               # Types + shared constants
│   └── renderer/             # React UI
│       └── src/
│           ├── bridge/       # Tauri invoke API (window.primeLauncher)
│           ├── design-system/
│           ├── layouts/
│           └── pages/
├── src-tauri/                # Rust backend (auth, launch, store, AI, …)
└── resources/launch-bridge/  # Node helper for Minecraft process spawn
```

## Development

```bash
cd launcher
npm install
npm run dev          # Tauri + Vite UI
```

```bash
npm run build:ui     # Frontend only
npm run dist         # Full Tauri NSIS installer (Windows)
npm run typecheck
```

## Auth note

Microsoft accounts must use Azure/Prism OAuth (built into Tauri). Accounts created with the old Electron/msmc flow need a one-time re-login.

## Relation to Prime Client

The launcher installs and updates the Fabric mod jars published with each GitHub release, manages instances, Prime Store / cosmetics, friends, and launches Minecraft via Fabric + Prime Client.
