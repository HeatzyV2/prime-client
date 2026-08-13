# Local Server Host (Prime Launcher)

Host Paper-family Minecraft servers from the Electron launcher (**Héberger** / **Host**).

## EULA

Mojang requires accepting the [Minecraft EULA](https://aka.ms/MinecraftEULA) before a server can start. Prime writes `eula.txt` in the server folder when you check the box at creation (or via **Accept EULA** on the detail page).

## Data location

Servers live under the launcher user data directory:

```text
{userData}/host-servers/{id}/
  prime-host.json
  server.jar
  eula.txt
  server.properties
  plugins/
  worlds/   (generated after first start)
  logs/
```

## Ports & firewall

- Default game port is **25565** (editable in Settings / `server.properties`).
- **LAN**: other PCs on your network join with your LAN IP and port (e.g. `192.168.1.20:25565`).
- **WAN**: you must open/forward that UDP/TCP port on your router and allow it in Windows Firewall. Prime does not create tunnels (no playit.gg / ngrok in v1).
- Only **one** local server process can run at a time in this version.

## Join from the launcher

With the server **Online**, click **Join**. Prime launches your active instance with Quick Play to `localhost:{port}`. You need a signed-in Minecraft account and at least one instance.

## Software & plugins

| Software | API |
|----------|-----|
| Paper / Folia | `fill.papermc.io` |
| Purpur | `api.purpurmc.org` |
| Leaf | `api.leafmc.one` |
| Canvas | `canvasmc.io/api/v2` |

Plugins can be searched/installed from **Modrinth**, **Hangar**, and **Spiget** (SpigotMC). Premium or external-only Spigot resources may fail with a clear error.

## Java

The server uses the Java path from launcher Settings (same as the client). Prefer Java 21+; newer forks (Leaf/Canvas on recent MC) may require Java 25+.
