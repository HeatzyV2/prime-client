/**
 * Social WebSocket — presence, friends events, chat, party.
 * Path: /social
 * Auth: first message { t: 'auth', token } OR ?token= query
 */
const { WebSocketServer } = require('ws');
const db = require('./db');

/** @type {Map<string, Set<import('ws').WebSocket>>} uuid -> sockets */
const socketsByUser = new Map();
/** @type {Map<import('ws').WebSocket, { uuid: string, client: string }>} */
const socketMeta = new WeakMap();
/** @type {Map<string, { status: string, activity: string, serverAddress: string|null, client: string, updatedAt: number }>} */
const presence = new Map();

function addSocket(uuid, ws) {
  if (!socketsByUser.has(uuid)) socketsByUser.set(uuid, new Set());
  socketsByUser.get(uuid).add(ws);
}

function removeSocket(uuid, ws) {
  const set = socketsByUser.get(uuid);
  if (!set) return;
  set.delete(ws);
  if (set.size === 0) {
    socketsByUser.delete(uuid);
    presence.delete(uuid);
  }
}

function send(ws, payload) {
  if (ws.readyState === 1) ws.send(JSON.stringify(payload));
}

function sendToUser(uuid, payload) {
  const set = socketsByUser.get(uuid);
  if (!set) return;
  const text = JSON.stringify(payload);
  for (const ws of set) {
    if (ws.readyState === 1) ws.send(text);
  }
}

function friendUuids(uuid) {
  return db.listFriendships(uuid)
    .filter((f) => f.status === 'accepted')
    .map((f) => (f.fromUuid === uuid ? f.toUuid : f.fromUuid));
}

function broadcastToFriends(uuid, payload) {
  for (const friend of friendUuids(uuid)) {
    sendToUser(friend, payload);
  }
}

function presencePayload(uuid) {
  const p = presence.get(uuid);
  const user = db.getUser(uuid);
  return {
    t: 'presence',
    uuid,
    username: user?.username || 'Player',
    status: p?.status || 'offline',
    activity: p?.activity || '',
    serverAddress: p?.serverAddress || null,
    client: p?.client || null,
  };
}

function snapshotFor(uuid) {
  const friends = friendUuids(uuid).map((id) => {
    const user = db.getUser(id);
    const p = presence.get(id);
    return {
      uuid: id,
      username: user?.username || 'Player',
      status: p?.status || 'offline',
      activity: p?.activity || '',
      serverAddress: p?.serverAddress || null,
    };
  });
  const pending = db.listFriendships(uuid)
    .filter((f) => f.status === 'pending')
    .map((f) => ({
      id: f.id,
      fromUuid: f.fromUuid,
      toUuid: f.toUuid,
      fromUsername: db.getUser(f.fromUuid)?.username || 'Player',
      toUsername: db.getUser(f.toUuid)?.username || 'Player',
      incoming: f.toUuid === uuid,
    }));
  const party = db.getPartyForUser(uuid);
  return { t: 'snapshot', friends, pending, party, self: presencePayload(uuid) };
}

function setPresence(uuid, patch) {
  const prev = presence.get(uuid) || {
    status: 'online',
    activity: '',
    serverAddress: null,
    client: 'unknown',
    updatedAt: Date.now(),
  };
  const nextClient = patch.client || prev.client || 'unknown';
  // Prefer game session over launcher when both are connected.
  const rank = (c) => (c === 'game' ? 2 : c === 'launcher' ? 1 : 0);
  if (rank(nextClient) < rank(prev.client) && Date.now() - prev.updatedAt < 15_000) {
    // Ignore stale launcher updates while in-game presence is fresh.
    if (nextClient === 'launcher' && prev.client === 'game') {
      return;
    }
  }
  const status = patch.status || prev.status || 'online';
  presence.set(uuid, {
    ...prev,
    ...patch,
    status,
    client: nextClient,
    updatedAt: Date.now(),
  });
  broadcastToFriends(uuid, presencePayload(uuid));
  sendToUser(uuid, presencePayload(uuid));
}

function sendToClient(uuid, client, payload) {
  const set = socketsByUser.get(uuid);
  if (!set) return;
  const text = JSON.stringify(payload);
  for (const ws of set) {
    const meta = socketMeta.get(ws);
    if (meta?.client === client && ws.readyState === 1) {
      ws.send(text);
    }
  }
}

function attachSocial(server) {
  const wss = new WebSocketServer({ noServer: true });

  wss.on('connection', (ws, req) => {
    let uuid = null;

    const url = new URL(req.url || '/', 'http://localhost');
    const queryToken = url.searchParams.get('token');

    function authed(token) {
      const session = db.resolveSession(token);
      if (!session) {
        send(ws, { t: 'error', message: 'Invalid session' });
        ws.close();
        return false;
      }
      uuid = session.uuid;
      socketMeta.set(ws, { uuid, client: session.client });
      addSocket(uuid, ws);
      if (!presence.has(uuid)) {
        presence.set(uuid, {
          status: 'online',
          activity: session.client === 'game' ? 'In game' : 'In launcher',
          serverAddress: null,
          client: session.client,
          updatedAt: Date.now(),
        });
      }
      send(ws, { t: 'ready', uuid });
      send(ws, snapshotFor(uuid));
      broadcastToFriends(uuid, presencePayload(uuid));
      return true;
    }

    if (queryToken) {
      if (!authed(queryToken)) return;
    }

    ws.on('message', (data) => {
      let msg;
      try {
        msg = JSON.parse(data.toString());
      } catch {
        return;
      }

      if (msg.t === 'auth') {
        if (!uuid) authed(msg.token);
        return;
      }

      if (!uuid) {
        send(ws, { t: 'error', message: 'Authenticate first' });
        return;
      }

      if (msg.t === 'presence') {
        setPresence(uuid, {
          status: msg.status || 'online',
          activity: msg.activity || '',
          serverAddress: msg.serverAddress || null,
          client: socketMeta.get(ws)?.client || 'unknown',
        });
        return;
      }

      if (msg.t === 'ping') {
        send(ws, { t: 'pong', ts: Date.now() });
        return;
      }

      if (msg.t === 'typing' && msg.conversationId) {
        const conversation = db.getConversation(msg.conversationId);
        if (!conversation || !conversation.participantUuids.includes(uuid)) return;
        for (const other of conversation.participantUuids) {
          if (other === uuid) continue;
          sendToUser(other, {
            t: 'typing',
            conversationId: msg.conversationId,
            uuid,
            username: db.getUser(uuid)?.username,
          });
        }
      }
    });

    ws.on('close', () => {
      if (!uuid) return;
      removeSocket(uuid, ws);
      if (!socketsByUser.has(uuid)) {
        broadcastToFriends(uuid, {
          t: 'presence',
          uuid,
          username: db.getUser(uuid)?.username || 'Player',
          status: 'offline',
          activity: '',
          serverAddress: null,
          client: null,
        });
      }
    });
  });

  return {
    wss,
    sendToUser,
    sendToClient,
    broadcastToFriends,
    friendUuids,
    presence,
    presencePayload,
    snapshotFor,
    setPresence,
    handleUpgrade(req, socket, head) {
      wss.handleUpgrade(req, socket, head, (ws) => wss.emit('connection', ws, req));
    },
  };
}

module.exports = { attachSocial };
