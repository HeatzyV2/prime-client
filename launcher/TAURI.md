# Prime Launcher — Tauri 2

Pixel-identical React UI. Shell is **Rust + WebView2**.

## Run

```bash
cd launcher
npm install
npm install --prefix resources/launch-bridge
npm run dev
```

## Features

| Feature | Implementation |
|---------|----------------|
| Same UI (React/CSS) | Vite + renderer |
| Window chrome / settings / accounts | Rust |
| Microsoft OAuth (Azure / Prism) | Rust |
| Instances CRUD + import | Rust |
| Play (Fabric) | Rust + short-lived Node `launch-bridge` |
| Modrinth + CurseForge | Rust |
| Skin library / wallpaper | Rust |
| Friends / chat / party | Rust → Prime backend |
| Store / promos / history (cloud + local) | Rust |
| AI assistant (Groq proxy) | Rust |
| Crash analysis DTO | Rust |
| GitHub updates (mod + NSIS) | Rust |

## Data

`%APPDATA%\prime-launcher\`

**Note:** Microsoft accounts created with the old Electron/msmc flow need a **one-time re-login**.

## Scripts

| Command | Role |
|---------|------|
| `npm run dev` | Tauri + Vite UI |
| `npm run dist` | Native NSIS installer |
| `npm run build:ui` | Frontend only |
