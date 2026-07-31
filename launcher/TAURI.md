# Prime Launcher — Tauri (experimental)

Pixel-identical React UI. Shell is **Rust + WebView2** (low RAM).

**Production / release path is Electron** (`npm run dev` / `npm run dist`). Use these scripts only for the experimental Tauri shell.

## Run

```bash
cd launcher
npm install
npm install --prefix resources/launch-bridge
npm run dev:tauri
```

## Build (experimental)

```bash
npm run dist:tauri
```

## Notes

- Same React renderer; in Tauri, `window.primeLauncher` is installed from `bridge/tauriPrimeApi.ts`.
- In Electron, the preload `contextBridge` owns `window.primeLauncher` (msmc Microsoft login).
- Do not ship Tauri NSIS as the primary GitHub Release asset — CI uses `npm run dist` (electron-builder → `Prime-Launcher-Setup-*.exe`).
