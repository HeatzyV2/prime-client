# Prime Backend

Unified **social + voice** server for Prime Client (friends / DM / party / presence).

**Version:** `2.0.0` — SQLite persistence, MS profile verify (optional), party invites, friend notes, block list.

## Run locally

```bash
cd backend
npm install
npm start
```

Default: `http://0.0.0.0:8765`

## Deploy (VPS / Pterodactyl)

Point the process at this `backend/` folder (same host as the old voice relay):

```bash
PORT=26005 npm start
```

Public example: `http://194.9.172.102:26005` — keep `/voice` unchanged for proximity voice.

After pulling `2.0.0`, restart the process once so SQLite migrates.

## Data & migration

| Path | Role |
|------|------|
| `data/prime.db` | SQLite store (users, sessions, friends, messages, parties, notes) |
| `data/prime.json` | Legacy JSON — **one-shot migrated** into SQLite on first boot if DB is empty, then kept as backup |
| `uploads/` | Chat image uploads |

No manual migrate step: start the server; if `prime.json` exists and `prime.db` has no users, migration runs automatically.

## Auth

`POST /v1/auth/session` body:

```json
{
  "uuid": "...",
  "username": "...",
  "offline": false,
  "client": "launcher" | "game",
  "accessToken": "optional Minecraft profile token"
}
```

- With `accessToken`: verified against `api.minecraftservices.com/minecraft/profile` (UUID/name must match).
- Without token: still allowed (compat) as `unverified` with tighter rate limits.
- Sessions are bound per `client`; logging in again from the same client revokes the previous session.

## Endpoints (v2 highlights)

| Path | Role |
|------|------|
| `GET /health` | `{ version: "2.0.0", db, ws }` |
| `POST /v1/auth/session` | Session token |
| `GET/POST /v1/friends…` | Friends + requests |
| `POST /v1/friends/block` · `DELETE …/block` | Block / unblock |
| `PUT /v1/friends/:uuid/note` | Server-persisted friend note |
| `GET/POST /v1/conversations…` | DMs (text + imageUrl) |
| `POST /v1/upload` | Multipart image (max 5MB) |
| `POST /v1/party` · `/invite` · `/accept` · `/decline` · `/leave` · `/kick` · `/server` | Party lifecycle (invites are pending until accept) |
| `WS /social?token=` | Presence, live chat, typing, party events; client `ping` every ~25s |
| `WS /voice` | Existing proximity voice (unchanged) |

## Smoke checklist

1. **Launcher DM ↔ in-game chat** — send from launcher Chat, see in Social Hub Chat tab (and reverse).
2. **Dual presence** — open launcher + game; leave world → presence demotes to launcher (not flash offline); close both → offline.
3. **Party invite** — invite from Friends → other client gets invite → Accept/Decline; members list updates live.
4. **Join from drawer** — friend in-game with `serverAddress` → Join uses that address (not the note text).
5. **Block** — block a user → cannot DM / party-invite them.
6. **Notes** — save a friend note in launcher → persists after restart (SQLite).
7. **Health** — `curl http://127.0.0.1:26005/health` → `version` `2.0.0`, `db.ok` true.
