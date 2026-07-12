# Discord Rich Presence — Asset Setup

Application ID: **1525574680994648174**

## Logo asset (one file for everything)

Use the project logo at [docs/assets/logo.png](assets/logo.png) (1024×559).

Upload it **once** in the [Discord Developer Portal](https://discord.com/developers/applications/1525574680994648174/rich-presence/assets) with this key:

| Asset key    | File to upload      |
|--------------|---------------------|
| `prime_logo` | `docs/assets/logo.png` |

The mod uses `prime_logo` for all Rich Presence images (menu, solo, multi, large icon).

## In-game assets (already bundled)

| Path | Usage |
|------|--------|
| `assets/primeclient/icon.png` | Fabric mod icon (Mod Menu / loader) |
| `assets/primeclient/textures/gui/logo.png` | ClickGUI menu, loading screen, HUD watermark, onboarding |

## Enable RPC

- **Launcher:** Settings → **Discord RPC** (enabled by default). Shows *Prime Launcher* while browsing; clears when Minecraft starts so the mod can take over.
- **In-game:** **Right Shift** → **Prime** → enable **Discord RPC** (Discord Desktop must be running).

Application ID: **1525574680994648174**
