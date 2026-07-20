# Prime Backend

Unified **social + voice** server for Prime Client (Lunar-style friends / DM / party / presence).

## Run locally

```bash
cd backend
npm install
npm start
```

Default: `http://0.0.0.0:8765`

## Deploy (same machine as old voice)

Point your Pterodactyl / process to this folder instead of `voice-relay`.

```bash
PORT=26005 npm start
```

Keep the same public host. Voice clients already use `/voice` — no protocol change.

## Endpoints

| Path | Role |
|------|------|
| `GET /health` | Health JSON |
| `POST /v1/auth/session` | `{ uuid, username, offline, client: "launcher"\|"game" }` → token |
| `GET/POST /v1/friends…` | Friends + requests |
| `GET/POST /v1/conversations…` | DMs (text + imageUrl) |
| `POST /v1/upload` | multipart image (png/jpeg/webp/gif, max 5MB) |
| `GET/POST /v1/party…` | Party + join-server |
| `WS /social?token=` | Presence, live chat, party events |
| `WS /voice` | Existing proximity voice |

## Auth

Unchanged client model: **Microsoft UUID** or **offline UUID** from launcher/game. No email/password. Session token after `/v1/auth/session`.

## Data

JSON store in `data/prime.json`, uploads in `uploads/`.
