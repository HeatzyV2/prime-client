# PrimeClient (Paper / Purpur plugin)

Server-side bridge for the **Prime Client** Fabric mod.

- Channel: `primeclient:main` (protocol 1 — see [docs/SERVER_API.md](../docs/SERVER_API.md))
- Detects official clients via handshake (UUID + name + protocol checks)
- SQLite (default) or MySQL
- XP, rewards, achievements, missions, `/prime` GUI
- PlaceholderAPI: `%primeclient_status%` `%primeclient_version%` `%primeclient_level%`

## Build

```bash
cd prime-plugin
../gradlew.bat build
```

Jar: `build/libs/PrimeClient-1.0.0.jar`

## Install

1. Drop the jar into `plugins/`
2. Restart (Paper/Purpur **1.21.11**)
3. Edit `plugins/PrimeClient/config.yml`
4. Optional: PlaceholderAPI, Vault (for `eco give` reward commands)

## Commands

| Command | Permission |
|---------|------------|
| `/prime` | `primeclient.use` |
| `/prime reload` | `primeclient.reload` |
| `/prime info <player>` | `primeclient.info` |
| `/prime rewards` | `primeclient.use` |
| `/prime achievements` | `primeclient.use` |

## API (other plugins)

```java
PrimeClientAPI api = PrimeClientPlugin.api();
if (api.isPrimeClient(player)) {
    api.addPrimeXP(player.getUniqueId(), 10, "SERVER_EVENT");
}
```
