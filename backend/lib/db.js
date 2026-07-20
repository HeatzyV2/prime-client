const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const DATA_DIR = process.env.PRIME_DATA_DIR || path.join(__dirname, '..', 'data');
const DB_PATH = path.join(DATA_DIR, 'prime.json');

/** @typedef {{ uuid: string, username: string, offline: boolean, createdAt: string, lastSeenAt: string }} User */
/** @typedef {{ id: string, fromUuid: string, toUuid: string, status: 'pending'|'accepted'|'blocked', createdAt: string }} Friendship */
/** @typedef {{ id: string, type: 'dm'|'party', participantUuids: string[], createdAt: string, updatedAt: string }} Conversation */
/** @typedef {{ id: string, conversationId: string, senderUuid: string, text: string, imageUrl: string|null, createdAt: string }} ChatMessage */
/** @typedef {{ id: string, leaderUuid: string, memberUuids: string[], serverAddress: string|null, createdAt: string }} Party */

function emptyDb() {
  return {
    users: {},
    friendships: {},
    conversations: {},
    messages: {},
    parties: {},
    sessions: {},
  };
}

function ensureDir(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
}

function load() {
  ensureDir(DATA_DIR);
  if (!fs.existsSync(DB_PATH)) {
    const db = emptyDb();
    save(db);
    return db;
  }
  try {
    return { ...emptyDb(), ...JSON.parse(fs.readFileSync(DB_PATH, 'utf8')) };
  } catch {
    return emptyDb();
  }
}

function save(db) {
  ensureDir(DATA_DIR);
  const tmp = DB_PATH + '.tmp';
  fs.writeFileSync(tmp, JSON.stringify(db, null, 2));
  fs.renameSync(tmp, DB_PATH);
}

let db = load();
let dirty = false;
let flushTimer = null;

function markDirty() {
  dirty = true;
  if (flushTimer) return;
  flushTimer = setTimeout(() => {
    flushTimer = null;
    if (!dirty) return;
    dirty = false;
    save(db);
  }, 250);
}

function flushNow() {
  if (flushTimer) {
    clearTimeout(flushTimer);
    flushTimer = null;
  }
  dirty = false;
  save(db);
}

function id() {
  return crypto.randomUUID();
}

function upsertUser(uuid, username, offline) {
  const now = new Date().toISOString();
  const existing = db.users[uuid];
  db.users[uuid] = {
    uuid,
    username: username || existing?.username || 'Player',
    offline: !!offline,
    createdAt: existing?.createdAt || now,
    lastSeenAt: now,
  };
  markDirty();
  return db.users[uuid];
}

function getUser(uuid) {
  return db.users[uuid] || null;
}

function findUserByName(username) {
  const lower = username.toLowerCase();
  return Object.values(db.users).find((u) => u.username.toLowerCase() === lower) || null;
}

function createSession(uuid, client) {
  const token = crypto.randomBytes(32).toString('hex');
  const expiresAt = Date.now() + 7 * 24 * 60 * 60 * 1000;
  db.sessions[token] = { uuid, client: client || 'unknown', expiresAt };
  markDirty();
  return { token, expiresAt };
}

function resolveSession(token) {
  if (!token) return null;
  const session = db.sessions[token];
  if (!session) return null;
  if (session.expiresAt < Date.now()) {
    delete db.sessions[token];
    markDirty();
    return null;
  }
  return session;
}

function friendshipKey(a, b) {
  return [a, b].sort().join(':');
}

function getFriendship(a, b) {
  return db.friendships[friendshipKey(a, b)] || null;
}

function listFriendships(uuid) {
  return Object.values(db.friendships).filter(
    (f) => f.fromUuid === uuid || f.toUuid === uuid
  );
}

function requestFriend(fromUuid, toUuid) {
  if (fromUuid === toUuid) throw new Error('Cannot friend yourself');
  const key = friendshipKey(fromUuid, toUuid);
  const existing = db.friendships[key];
  if (existing?.status === 'accepted') throw new Error('Already friends');
  if (existing?.status === 'pending') {
    if (existing.toUuid === fromUuid) {
      existing.status = 'accepted';
      markDirty();
      return existing;
    }
    throw new Error('Request already pending');
  }
  const friendship = {
    id: id(),
    fromUuid,
    toUuid,
    status: 'pending',
    createdAt: new Date().toISOString(),
  };
  db.friendships[key] = friendship;
  markDirty();
  return friendship;
}

function acceptFriend(uuid, otherUuid) {
  const f = getFriendship(uuid, otherUuid);
  if (!f || f.status !== 'pending') throw new Error('No pending request');
  if (f.toUuid !== uuid) throw new Error('Not your request to accept');
  f.status = 'accepted';
  markDirty();
  return f;
}

function removeFriend(uuid, otherUuid) {
  const key = friendshipKey(uuid, otherUuid);
  if (!db.friendships[key]) throw new Error('Not friends');
  delete db.friendships[key];
  markDirty();
}

function areFriends(a, b) {
  const f = getFriendship(a, b);
  return !!(f && f.status === 'accepted');
}

function getOrCreateDm(a, b) {
  const sorted = [a, b].sort();
  const existing = Object.values(db.conversations).find(
    (c) =>
      c.type === 'dm' &&
      c.participantUuids.length === 2 &&
      c.participantUuids.includes(sorted[0]) &&
      c.participantUuids.includes(sorted[1])
  );
  if (existing) return existing;
  const conversation = {
    id: id(),
    type: 'dm',
    participantUuids: sorted,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };
  db.conversations[conversation.id] = conversation;
  markDirty();
  return conversation;
}

function listConversations(uuid) {
  return Object.values(db.conversations)
    .filter((c) => c.participantUuids.includes(uuid))
    .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
}

function getConversation(id) {
  return db.conversations[id] || null;
}

function addMessage(conversationId, senderUuid, text, imageUrl) {
  const conversation = db.conversations[conversationId];
  if (!conversation) throw new Error('Conversation not found');
  if (!conversation.participantUuids.includes(senderUuid)) {
    throw new Error('Not a participant');
  }
  const message = {
    id: id(),
    conversationId,
    senderUuid,
    text: text || '',
    imageUrl: imageUrl || null,
    createdAt: new Date().toISOString(),
  };
  db.messages[message.id] = message;
  conversation.updatedAt = message.createdAt;
  markDirty();
  return message;
}

function listMessages(conversationId, limit = 50, before) {
  let list = Object.values(db.messages)
    .filter((m) => m.conversationId === conversationId)
    .sort((a, b) => a.createdAt.localeCompare(b.createdAt));
  if (before) {
    list = list.filter((m) => m.createdAt < before);
  }
  if (list.length > limit) {
    list = list.slice(list.length - limit);
  }
  return list;
}

function createParty(leaderUuid) {
  for (const p of Object.values(db.parties)) {
    if (p.memberUuids.includes(leaderUuid)) {
      delete db.parties[p.id];
    }
  }
  const party = {
    id: id(),
    leaderUuid,
    memberUuids: [leaderUuid],
    serverAddress: null,
    createdAt: new Date().toISOString(),
  };
  db.parties[party.id] = party;
  markDirty();
  return party;
}

function getParty(id) {
  return db.parties[id] || null;
}

function getPartyForUser(uuid) {
  return Object.values(db.parties).find((p) => p.memberUuids.includes(uuid)) || null;
}

function inviteToParty(partyId, leaderUuid, targetUuid) {
  const party = db.parties[partyId];
  if (!party) throw new Error('Party not found');
  if (party.leaderUuid !== leaderUuid) throw new Error('Only leader can invite');
  if (party.memberUuids.includes(targetUuid)) throw new Error('Already in party');
  if (party.memberUuids.length >= 8) throw new Error('Party full');
  party.memberUuids.push(targetUuid);
  const existing = getPartyForUser(targetUuid);
  if (existing && existing.id !== partyId) {
    leaveParty(targetUuid);
    if (!party.memberUuids.includes(targetUuid)) party.memberUuids.push(targetUuid);
  }
  markDirty();
  return party;
}

function leaveParty(uuid) {
  const party = getPartyForUser(uuid);
  if (!party) return null;
  party.memberUuids = party.memberUuids.filter((u) => u !== uuid);
  if (party.memberUuids.length === 0) {
    delete db.parties[party.id];
  } else if (party.leaderUuid === uuid) {
    party.leaderUuid = party.memberUuids[0];
  }
  markDirty();
  return party;
}

function setPartyServer(uuid, serverAddress) {
  const party = getPartyForUser(uuid);
  if (!party) throw new Error('Not in a party');
  if (party.leaderUuid !== uuid) throw new Error('Only leader can set server');
  party.serverAddress = serverAddress || null;
  markDirty();
  return party;
}

process.on('exit', flushNow);
process.on('SIGINT', () => {
  flushNow();
  process.exit(0);
});

module.exports = {
  DATA_DIR,
  upsertUser,
  getUser,
  findUserByName,
  createSession,
  resolveSession,
  listFriendships,
  requestFriend,
  acceptFriend,
  removeFriend,
  areFriends,
  getOrCreateDm,
  listConversations,
  getConversation,
  addMessage,
  listMessages,
  createParty,
  getParty,
  getPartyForUser,
  inviteToParty,
  leaveParty,
  setPartyServer,
  flushNow,
  id,
};
