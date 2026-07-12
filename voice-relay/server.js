/**
 * Prime Voice relay — proximity (48 blocks) + voice groups per Minecraft server room.
 * Only clients with client:"prime" are admitted.
 *
 * Default: ws://194.9.172.102:26005/voice
 */
const http = require('http');
const { WebSocketServer } = require('ws');

const PORT = process.env.PORT || process.env.SERVER_PORT || 8765;
const HOST = process.env.HOST || '0.0.0.0';
const PATH = '/voice';
const PROXIMITY_BLOCKS = 48;

/** @type {Map<string, Map<string, { ws, name, x, y, z, group }>>} */
const rooms = new Map();

function distance(a, b) {
  const dx = (a.x ?? 0) - (b.x ?? 0);
  const dy = (a.y ?? 0) - (b.y ?? 0);
  const dz = (a.z ?? 0) - (b.z ?? 0);
  return Math.sqrt(dx * dx + dy * dy + dz * dz);
}

function sameGroup(a, b) {
  return a.group && b.group && a.group === b.group;
}

function canHear(sender, recipient) {
  if (!sender || !recipient) return false;
  if (sameGroup(sender, recipient)) return true;
  return distance(sender, recipient) <= PROXIMITY_BLOCKS;
}

function broadcastRoom(roomId, json, exceptWs) {
  const room = rooms.get(roomId);
  if (!room) return;
  const text = JSON.stringify(json);
  for (const entry of room.values()) {
    if (entry.ws !== exceptWs && entry.ws.readyState === 1) {
      entry.ws.send(text);
    }
  }
}

function participantList(roomId) {
  const room = rooms.get(roomId);
  if (!room) return [];
  return [...room.entries()].map(([id, e]) => ({
    id,
    name: e.name,
    x: e.x ?? 0,
    y: e.y ?? 0,
    z: e.z ?? 0,
    group: e.group || '',
  }));
}

function sendParticipants(roomId) {
  broadcastRoom(roomId, { t: 'participants', list: participantList(roomId) });
}

const server = http.createServer((_req, res) => {
  res.writeHead(200, { 'Content-Type': 'text/plain' });
  res.end(`Prime Voice relay OK (proximity ${PROXIMITY_BLOCKS} blocks + groups)\n`);
});

const wss = new WebSocketServer({ server, path: PATH });

wss.on('connection', (ws) => {
  let roomId = null;
  let userId = null;

  ws.on('message', (data, isBinary) => {
    if (isBinary) {
      if (!roomId || !userId) return;
      const room = rooms.get(roomId);
      if (!room) return;
      const sender = room.get(userId);
      if (!sender) return;
      for (const [id, entry] of room.entries()) {
        if (id !== userId && canHear(sender, entry) && entry.ws.readyState === 1) {
          entry.ws.send(data, { binary: true });
        }
      }
      return;
    }

    let msg;
    try {
      msg = JSON.parse(data.toString());
    } catch {
      return;
    }

    if (msg.t === 'join') {
      if (msg.client !== 'prime' || !msg.room || !msg.id || !msg.name) {
        ws.send(JSON.stringify({ t: 'error', message: 'Prime Client required' }));
        ws.close();
        return;
      }
      roomId = msg.room;
      userId = msg.id;
      if (!rooms.has(roomId)) rooms.set(roomId, new Map());
      const room = rooms.get(roomId);
      const existing = room.get(userId);
      room.set(userId, {
        ws,
        name: msg.name,
        x: existing?.x ?? 0,
        y: existing?.y ?? 0,
        z: existing?.z ?? 0,
        group: msg.group || existing?.group || '',
      });
      ws.send(JSON.stringify({ t: 'participants', list: participantList(roomId) }));
      broadcastRoom(roomId, { t: 'participants', list: participantList(roomId) }, ws);
      return;
    }

    if (msg.t === 'group' && roomId && userId) {
      const room = rooms.get(roomId);
      const entry = room?.get(userId);
      if (entry) {
        entry.group = msg.group || '';
        sendParticipants(roomId);
      }
      return;
    }

    if (msg.t === 'pos' && roomId && userId) {
      const room = rooms.get(roomId);
      const entry = room?.get(userId);
      if (entry) {
        entry.x = msg.x ?? 0;
        entry.y = msg.y ?? 0;
        entry.z = msg.z ?? 0;
      }
      return;
    }

    if (msg.t === 'leave' && roomId && userId) {
      const room = rooms.get(roomId);
      room?.delete(userId);
      if (room && room.size === 0) rooms.delete(roomId);
      sendParticipants(roomId);
    }
  });

  ws.on('close', () => {
    if (!roomId || !userId) return;
    const room = rooms.get(roomId);
    room?.delete(userId);
    if (room && room.size === 0) rooms.delete(roomId);
    else sendParticipants(roomId);
  });
});

server.listen(PORT, HOST, () => {
  console.log(`Prime Voice relay on ws://${HOST}:${PORT}${PATH} (proximity ${PROXIMITY_BLOCKS}b)`);
});
