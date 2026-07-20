/**
 * Prime Backend — unified social + voice server.
 *
 * Default local:  http://0.0.0.0:8765
 * Production:     same host as before (e.g. :26005)
 *
 * Endpoints:
 *   GET  /health
 *   POST /v1/auth/session
 *   REST /v1/friends | /v1/conversations | /v1/party | /v1/upload
 *   WS   /voice   (unchanged Prime voice protocol)
 *   WS   /social  (?token= or first {t:"auth",token})
 */
const http = require('http');
const { attachVoice } = require('./lib/voice');
const { attachSocial } = require('./lib/social');
const { handleHttp } = require('./lib/http');

const PORT = Number(process.env.PORT || process.env.SERVER_PORT || 8765);
const HOST = process.env.HOST || '0.0.0.0';

const server = http.createServer(async (req, res) => {
  try {
    const handled = await handleHttp(req, res, { social });
    if (!handled) {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Not found' }));
    }
  } catch (err) {
    console.error(err);
    if (!res.headersSent) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Internal error' }));
    }
  }
});

const voice = attachVoice(server);
const social = attachSocial(server);

server.on('upgrade', (req, socket, head) => {
  const pathOnly = (req.url || '').split('?')[0];
  if (pathOnly === '/voice') {
    voice.handleUpgrade(req, socket, head);
    return;
  }
  if (pathOnly === '/social') {
    social.handleUpgrade(req, socket, head);
    return;
  }
  socket.destroy();
});

server.listen(PORT, HOST, () => {
  console.log(`Prime backend on http://${HOST}:${PORT}`);
  console.log(`  voice  ws://${HOST}:${PORT}/voice (proximity ${voice.proximity}b)`);
  console.log(`  social ws://${HOST}:${PORT}/social`);
  console.log(`  api    http://${HOST}:${PORT}/v1`);
});
