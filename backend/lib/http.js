const fs = require('fs');
const path = require('path');
const { URL } = require('url');
const db = require('./db');
const rateLimit = require('./rateLimit');
const { loadNews, versionsManifest } = require('./news');

const UPLOAD_DIR = process.env.PRIME_UPLOAD_DIR || path.join(__dirname, '..', 'uploads');
const CRASH_DIR = process.env.PRIME_CRASH_DIR || path.join(UPLOAD_DIR, 'crashes');
const MAX_UPLOAD = 5 * 1024 * 1024; // 5 MB
const MAX_CRASH = 2 * 1024 * 1024;
const ALLOWED_TYPES = new Set(['image/png', 'image/jpeg', 'image/jpg', 'image/webp', 'image/gif']);

function ensureUploadDir() {
  if (!fs.existsSync(UPLOAD_DIR)) fs.mkdirSync(UPLOAD_DIR, { recursive: true });
}

function ensureCrashDir() {
  if (!fs.existsSync(CRASH_DIR)) fs.mkdirSync(CRASH_DIR, { recursive: true });
}

function clientIp(req) {
  const fwd = req.headers['x-forwarded-for'];
  if (typeof fwd === 'string' && fwd.length) return fwd.split(',')[0].trim();
  return req.socket?.remoteAddress || 'unknown';
}

function enforceRate(req, res, route, opts) {
  const result = rateLimit.check(clientIp(req), route, opts);
  if (!result.ok) {
    json(res, 429, { error: 'Too many requests', retryAfterMs: result.retryAfterMs });
    return false;
  }
  return true;
}

function json(res, status, body) {
  const raw = JSON.stringify(body);
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Access-Control-Allow-Methods': 'GET,POST,DELETE,OPTIONS',
  });
  res.end(raw);
}

function readBody(req, limit = 2 * 1024 * 1024) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let size = 0;
    req.on('data', (chunk) => {
      size += chunk.length;
      if (size > limit) {
        reject(new Error('Body too large'));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on('end', () => resolve(Buffer.concat(chunks)));
    req.on('error', reject);
  });
}

function parseMultipart(buffer, boundary) {
  const parts = [];
  const sep = Buffer.from('--' + boundary);
  let start = buffer.indexOf(sep) + sep.length + 2;
  while (start < buffer.length) {
    const next = buffer.indexOf(sep, start);
    if (next < 0) break;
    let part = buffer.subarray(start, next - 2);
    const headerEnd = part.indexOf('\r\n\r\n');
    if (headerEnd < 0) {
      start = next + sep.length + 2;
      continue;
    }
    const headers = part.subarray(0, headerEnd).toString('utf8');
    const body = part.subarray(headerEnd + 4);
    const nameMatch = /name="([^"]+)"/.exec(headers);
    const fileMatch = /filename="([^"]+)"/.exec(headers);
    const typeMatch = /Content-Type:\s*([^\r\n]+)/i.exec(headers);
    parts.push({
      name: nameMatch?.[1] || '',
      filename: fileMatch?.[1] || null,
      contentType: typeMatch?.[1]?.trim() || 'application/octet-stream',
      data: body,
    });
    start = next + sep.length + 2;
  }
  return parts;
}

function bearer(req) {
  const h = req.headers.authorization || '';
  if (h.startsWith('Bearer ')) return h.slice(7).trim();
  return null;
}

function requireAuth(req, res) {
  const token = bearer(req);
  const session = db.resolveSession(token);
  if (!session) {
    json(res, 401, { error: 'Unauthorized' });
    return null;
  }
  return session;
}

/**
 * @param {import('http').IncomingMessage} req
 * @param {import('http').ServerResponse} res
 * @param {{ social: any }} ctx
 */
async function handleHttp(req, res, ctx) {
  const url = new URL(req.url || '/', `http://${req.headers.host || 'localhost'}`);
  const pathname = url.pathname;

  if (req.method === 'OPTIONS') {
    json(res, 204, {});
    return true;
  }

  if (req.method === 'GET' && (pathname === '/' || pathname === '/health')) {
    json(res, 200, {
      ok: true,
      service: 'prime-backend',
      version: versionsManifest().backend,
      voice: '/voice',
      social: '/social',
      api: '/v1',
    });
    return true;
  }

  if (req.method === 'GET' && pathname === '/v1/news') {
    json(res, 200, { items: loadNews() });
    return true;
  }

  if (req.method === 'GET' && pathname === '/v1/versions') {
    json(res, 200, versionsManifest());
    return true;
  }

  if (req.method === 'GET' && pathname.startsWith('/uploads/')) {
    ensureUploadDir();
    const file = path.basename(pathname);
    const full = path.join(UPLOAD_DIR, file);
    if (!full.startsWith(UPLOAD_DIR) || !fs.existsSync(full)) {
      json(res, 404, { error: 'Not found' });
      return true;
    }
    const ext = path.extname(file).toLowerCase();
    const types = {
      '.png': 'image/png',
      '.jpg': 'image/jpeg',
      '.jpeg': 'image/jpeg',
      '.webp': 'image/webp',
      '.gif': 'image/gif',
    };
    res.writeHead(200, {
      'Content-Type': types[ext] || 'application/octet-stream',
      'Access-Control-Allow-Origin': '*',
      'Cache-Control': 'public, max-age=86400',
    });
    fs.createReadStream(full).pipe(res);
    return true;
  }

  if (!pathname.startsWith('/v1/')) return false;

  try {
    // Auth — Microsoft / offline UUID from launcher or game (unchanged client auth model)
    if (req.method === 'POST' && pathname === '/v1/auth/session') {
      if (!enforceRate(req, res, 'auth', { limit: 30, windowMs: 60_000 })) return true;
      const body = JSON.parse((await readBody(req)).toString('utf8') || '{}');
      const uuid = String(body.uuid || '').trim();
      const username = String(body.username || '').trim();
      const offline = !!body.offline;
      const client = String(body.client || 'unknown');
      if (!uuid || !username) {
        json(res, 400, { error: 'uuid and username required' });
        return true;
      }
      const user = db.upsertUser(uuid, username, offline);
      const session = db.createSession(uuid, client);
      json(res, 200, {
        token: session.token,
        expiresAt: session.expiresAt,
        user,
      });
      return true;
    }

    if (req.method === 'GET' && pathname === '/v1/me') {
      const session = requireAuth(req, res);
      if (!session) return true;
      json(res, 200, { user: db.getUser(session.uuid), presence: ctx.social.presence.get(session.uuid) || null });
      return true;
    }

    // Friends
    if (req.method === 'GET' && pathname === '/v1/friends') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const list = db.listFriendships(session.uuid).map((f) => {
        const other = f.fromUuid === session.uuid ? f.toUuid : f.fromUuid;
        const user = db.getUser(other);
        const p = ctx.social.presence.get(other);
        return {
          friendshipId: f.id,
          status: f.status,
          incoming: f.status === 'pending' && f.toUuid === session.uuid,
          uuid: other,
          username: user?.username || 'Player',
          presence: p
            ? { status: p.status, activity: p.activity, serverAddress: p.serverAddress }
            : { status: 'offline', activity: '', serverAddress: null },
        };
      });
      json(res, 200, { friends: list });
      return true;
    }

    if (req.method === 'POST' && pathname === '/v1/friends/request') {
      if (!enforceRate(req, res, 'friend-request', { limit: 20, windowMs: 60_000 })) return true;
      const session = requireAuth(req, res);
      if (!session) return true;
      const body = JSON.parse((await readBody(req)).toString('utf8') || '{}');
      let targetUuid = body.uuid ? String(body.uuid) : null;
      const username = body.username ? String(body.username).trim() : null;
      if (!targetUuid && username) {
        const found = db.findUserByName(username);
        if (found) targetUuid = found.uuid;
        else {
          // Allow requesting by Mojang username even if never logged into Prime yet:
          // create a placeholder user once they come online they'll match by name.
          json(res, 404, {
            error: 'User has never opened Prime yet. They must launch Prime Client or Launcher once.',
          });
          return true;
        }
      }
      if (!targetUuid) {
        json(res, 400, { error: 'uuid or username required' });
        return true;
      }
      const friendship = db.requestFriend(session.uuid, targetUuid);
      ctx.social.sendToUser(targetUuid, {
        t: 'friend_request',
        fromUuid: session.uuid,
        fromUsername: db.getUser(session.uuid)?.username,
        friendship,
      });
      ctx.social.sendToUser(session.uuid, { t: 'friend_update', friendship });
      json(res, 200, { friendship });
      return true;
    }

    if (req.method === 'POST' && pathname === '/v1/friends/accept') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const body = JSON.parse((await readBody(req)).toString('utf8') || '{}');
      const otherUuid = String(body.uuid || '');
      const friendship = db.acceptFriend(session.uuid, otherUuid);
      ctx.social.sendToUser(otherUuid, { t: 'friend_accepted', uuid: session.uuid, friendship });
      ctx.social.sendToUser(session.uuid, { t: 'friend_accepted', uuid: otherUuid, friendship });
      ctx.social.sendToUser(session.uuid, ctx.social.snapshotFor(session.uuid));
      ctx.social.sendToUser(otherUuid, ctx.social.snapshotFor(otherUuid));
      json(res, 200, { friendship });
      return true;
    }

    if (req.method === 'DELETE' && pathname.startsWith('/v1/friends/')) {
      const session = requireAuth(req, res);
      if (!session) return true;
      const otherUuid = pathname.slice('/v1/friends/'.length);
      db.removeFriend(session.uuid, otherUuid);
      ctx.social.sendToUser(otherUuid, { t: 'friend_removed', uuid: session.uuid });
      ctx.social.sendToUser(session.uuid, { t: 'friend_removed', uuid: otherUuid });
      json(res, 200, { ok: true });
      return true;
    }

    // Chat
    if (req.method === 'GET' && pathname === '/v1/conversations') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const conversations = db.listConversations(session.uuid).map((c) => {
        const others = c.participantUuids.filter((u) => u !== session.uuid);
        return {
          ...c,
          participants: others.map((u) => ({
            uuid: u,
            username: db.getUser(u)?.username || 'Player',
          })),
        };
      });
      json(res, 200, { conversations });
      return true;
    }

    if (req.method === 'POST' && pathname === '/v1/conversations/dm') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const body = JSON.parse((await readBody(req)).toString('utf8') || '{}');
      const otherUuid = String(body.uuid || '');
      if (!db.areFriends(session.uuid, otherUuid)) {
        json(res, 403, { error: 'Must be friends to DM' });
        return true;
      }
      const conversation = db.getOrCreateDm(session.uuid, otherUuid);
      json(res, 200, { conversation });
      return true;
    }

    if (req.method === 'GET' && pathname.startsWith('/v1/conversations/') && pathname.endsWith('/messages')) {
      const session = requireAuth(req, res);
      if (!session) return true;
      const conversationId = pathname.slice('/v1/conversations/'.length, -'/messages'.length);
      const conversation = db.getConversation(conversationId);
      if (!conversation || !conversation.participantUuids.includes(session.uuid)) {
        json(res, 404, { error: 'Not found' });
        return true;
      }
      const limit = Math.min(100, parseInt(url.searchParams.get('limit') || '50', 10));
      const before = url.searchParams.get('before') || undefined;
      const messages = db.listMessages(conversationId, limit, before).map((m) => ({
        ...m,
        senderUsername: db.getUser(m.senderUuid)?.username || null,
      }));
      json(res, 200, { messages });
      return true;
    }

    if (req.method === 'POST' && pathname.startsWith('/v1/conversations/') && pathname.endsWith('/messages')) {
      if (!enforceRate(req, res, 'chat-send', { limit: 40, windowMs: 60_000 })) return true;
      const session = requireAuth(req, res);
      if (!session) return true;
      const conversationId = pathname.slice('/v1/conversations/'.length, -'/messages'.length);
      const conversation = db.getConversation(conversationId);
      if (!conversation || !conversation.participantUuids.includes(session.uuid)) {
        json(res, 404, { error: 'Not found' });
        return true;
      }
      const body = JSON.parse((await readBody(req)).toString('utf8') || '{}');
      const text = String(body.text || '').slice(0, 2000);
      const imageUrl = body.imageUrl ? String(body.imageUrl).slice(0, 500) : null;
      if (!text && !imageUrl) {
        json(res, 400, { error: 'text or imageUrl required' });
        return true;
      }
      const message = db.addMessage(conversationId, session.uuid, text, imageUrl);
      const senderUsername = db.getUser(session.uuid)?.username || null;
      const enriched = { ...message, senderUsername };
      const event = {
        t: 'message',
        message: enriched,
      };
      for (const u of conversation.participantUuids) {
        ctx.social.sendToUser(u, event);
      }
      json(res, 200, { message: enriched });
      return true;
    }

    // Upload image (launcher / desktop clients)
    if (req.method === 'POST' && pathname === '/v1/upload') {
      if (!enforceRate(req, res, 'upload', { limit: 15, windowMs: 60_000 })) return true;
      const session = requireAuth(req, res);
      if (!session) return true;
      const contentType = req.headers['content-type'] || '';
      const match = /boundary=(.+)$/i.exec(contentType);
      if (!match) {
        json(res, 400, { error: 'multipart/form-data required' });
        return true;
      }
      const raw = await readBody(req, MAX_UPLOAD + 64 * 1024);
      const parts = parseMultipart(raw, match[1].trim());
      const file = parts.find((p) => p.filename && p.name === 'file');
      if (!file) {
        json(res, 400, { error: 'file field required' });
        return true;
      }
      if (!ALLOWED_TYPES.has(file.contentType)) {
        json(res, 400, { error: 'Only png/jpeg/webp/gif allowed' });
        return true;
      }
      if (file.data.length > MAX_UPLOAD) {
        json(res, 400, { error: 'Max 5MB' });
        return true;
      }
      ensureUploadDir();
      const ext =
        file.contentType === 'image/png'
          ? '.png'
          : file.contentType === 'image/webp'
            ? '.webp'
            : file.contentType === 'image/gif'
              ? '.gif'
              : '.jpg';
      const name = `${require('crypto').randomUUID()}${ext}`;
      fs.writeFileSync(path.join(UPLOAD_DIR, name), file.data);
      const urlPath = `/uploads/${name}`;
      json(res, 200, { url: urlPath, fullUrl: urlPath });
      return true;
    }

    // Crash report upload (optional, launcher / mod)
    if (req.method === 'POST' && pathname === '/v1/crash') {
      if (!enforceRate(req, res, 'crash', { limit: 8, windowMs: 60_000 })) return true;
      const session = requireAuth(req, res);
      if (!session) return true;
      const body = JSON.parse((await readBody(req, MAX_CRASH + 64 * 1024)).toString('utf8') || '{}');
      const text = String(body.text || body.report || '').slice(0, MAX_CRASH);
      const title = String(body.title || 'crash').slice(0, 120);
      if (!text.trim()) {
        json(res, 400, { error: 'text required' });
        return true;
      }
      ensureCrashDir();
      const id = require('crypto').randomUUID();
      const fileName = `${id}.log`;
      const meta = {
        id,
        uuid: session.uuid,
        username: db.getUser(session.uuid)?.username || 'Player',
        title,
        createdAt: new Date().toISOString(),
        client: session.client,
      };
      fs.writeFileSync(path.join(CRASH_DIR, fileName), text, 'utf8');
      fs.writeFileSync(path.join(CRASH_DIR, `${id}.json`), JSON.stringify(meta, null, 2));
      json(res, 200, { ok: true, id, fileName });
      return true;
    }

    // Party
    if (req.method === 'GET' && pathname === '/v1/party') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const party = db.getPartyForUser(session.uuid);
      if (!party) {
        json(res, 200, { party: null });
        return true;
      }
      json(res, 200, {
        party: {
          ...party,
          members: party.memberUuids.map((u) => ({
            uuid: u,
            username: db.getUser(u)?.username || 'Player',
            leader: u === party.leaderUuid,
          })),
        },
      });
      return true;
    }

    if (req.method === 'POST' && pathname === '/v1/party') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const party = db.createParty(session.uuid);
      ctx.social.sendToUser(session.uuid, { t: 'party', party });
      json(res, 200, { party });
      return true;
    }

    if (req.method === 'POST' && pathname === '/v1/party/invite') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const body = JSON.parse((await readBody(req)).toString('utf8') || '{}');
      const targetUuid = String(body.uuid || '');
      if (!db.areFriends(session.uuid, targetUuid)) {
        json(res, 403, { error: 'Can only invite friends' });
        return true;
      }
      let party = db.getPartyForUser(session.uuid);
      if (!party) party = db.createParty(session.uuid);
      party = db.inviteToParty(party.id, session.uuid, targetUuid);
      for (const u of party.memberUuids) {
        ctx.social.sendToUser(u, {
          t: 'party',
          party: {
            ...party,
            members: party.memberUuids.map((id) => ({
              uuid: id,
              username: db.getUser(id)?.username || 'Player',
              leader: id === party.leaderUuid,
            })),
          },
        });
      }
      ctx.social.sendToUser(targetUuid, {
        t: 'party_invite',
        fromUuid: session.uuid,
        fromUsername: db.getUser(session.uuid)?.username,
        partyId: party.id,
        serverAddress: party.serverAddress,
      });
      json(res, 200, { party });
      return true;
    }

    if (req.method === 'POST' && pathname === '/v1/party/leave') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const before = db.getPartyForUser(session.uuid);
      const party = db.leaveParty(session.uuid);
      if (before) {
        for (const u of before.memberUuids) {
          if (u === session.uuid) continue;
          ctx.social.sendToUser(u, {
            t: 'party',
            party: party
              ? {
                  ...party,
                  members: party.memberUuids.map((id) => ({
                    uuid: id,
                    username: db.getUser(id)?.username || 'Player',
                    leader: id === party.leaderUuid,
                  })),
                }
              : null,
          });
        }
      }
      ctx.social.sendToUser(session.uuid, { t: 'party', party: null });
      json(res, 200, { ok: true });
      return true;
    }

    if (req.method === 'POST' && pathname === '/v1/party/server') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const body = JSON.parse((await readBody(req)).toString('utf8') || '{}');
      const party = db.setPartyServer(session.uuid, body.serverAddress || null);
      for (const u of party.memberUuids) {
        ctx.social.sendToUser(u, {
          t: 'party',
          party: {
            ...party,
            members: party.memberUuids.map((id) => ({
              uuid: id,
              username: db.getUser(id)?.username || 'Player',
              leader: id === party.leaderUuid,
            })),
          },
        });
        if (u !== session.uuid && party.serverAddress) {
          ctx.social.sendToUser(u, {
            t: 'party_join_server',
            serverAddress: party.serverAddress,
            fromUuid: session.uuid,
            fromUsername: db.getUser(session.uuid)?.username,
          });
        }
      }
      json(res, 200, { party });
      return true;
    }

    json(res, 404, { error: 'Not found' });
    return true;
  } catch (err) {
    json(res, 400, { error: err instanceof Error ? err.message : 'Bad request' });
    return true;
  }
}

module.exports = { handleHttp, UPLOAD_DIR };
