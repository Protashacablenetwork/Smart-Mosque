#!/usr/bin/env node
/**
 * Masjid & Society Local Server (Zero-Dependency Node.js)
 *
 * Usage:
 *   node server.js
 *   PORT=3000 API_TOKEN=mysecret ADMIN_PIN=1234 node server.js
 */
const http = require("http");
const fs = require("fs");
const path = require("path");
const os = require("os");
const crypto = require("crypto");

const PORT = Number(process.env.PORT || 3000);
const DATA_FILE = process.env.DATA_FILE || path.join(__dirname, "masjid-data.json");
const API_TOKEN = process.env.API_TOKEN || "";
const ADMIN_PIN = process.env.ADMIN_PIN || "1234";

const COLLECTIONS = [
  "families",
  "members",
  "dues",
  "payments",
  "donations",
  "expenses",
  "bank_accounts",
  "budgets",
  "qurbani_batches",
  "meat_distributions",
  "notices",
  "committees",
  "meetings",
  "events",
  "assets",
  "documents",
  "welfare_records",
  "complaints",
  "audit_logs",
  "profiles"
];

function createEmptyState() {
  const state = {
    mosque_schedule: {
      id: "main",
      mosqueName: "বায়তুল আমান জামে মসজিদ",
      fajrTime: "05:15 AM",
      sunriseTime: "06:22 AM",
      dhuhrTime: "01:15 PM",
      asrTime: "04:15 PM",
      maghribTime: "05:30 PM",
      ishaTime: "07:30 PM",
      jumuahTime: "01:30 PM"
    }
  };
  for (const col of COLLECTIONS) state[col] = [];
  return state;
}

function loadState() {
  try {
    if (fs.existsSync(DATA_FILE)) {
      const parsed = JSON.parse(fs.readFileSync(DATA_FILE, "utf8"));
      const base = createEmptyState();
      if (parsed.mosque_schedule && typeof parsed.mosque_schedule === "object") {
        base.mosque_schedule = { id: "main", ...parsed.mosque_schedule };
      }
      for (const col of COLLECTIONS) {
        if (Array.isArray(parsed[col])) base[col] = parsed[col];
      }
      return base;
    }
  } catch (err) {
    console.error("Failed to read data file:", err.message);
  }
  return createEmptyState();
}

let db = loadState();

function persistState() {
  const tmpFile = `${DATA_FILE}.tmp`;
  fs.writeFileSync(tmpFile, JSON.stringify(db, null, 2), "utf8");
  fs.renameSync(tmpFile, DATA_FILE);
}

function sendJson(res, status, payload) {
  res.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET,POST,PUT,DELETE,OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type,Authorization"
  });
  res.end(JSON.stringify(payload));
}

function readJsonBody(req) {
  return new Promise((resolve, reject) => {
    let raw = "";
    req.on("data", chunk => {
      raw += chunk;
      if (raw.length > 15 * 1024 * 1024) {
        req.destroy();
        reject(new Error("Payload too large"));
      }
    });
    req.on("end", () => {
      if (!raw.trim()) return resolve({});
      try {
        resolve(JSON.parse(raw));
      } catch (e) {
        reject(new Error("Invalid JSON body"));
      }
    });
    req.on("error", reject);
  });
}

function isAuthorized(req) {
  if (!API_TOKEN) return true;
  const auth = req.headers.authorization || "";
  return auth === `Bearer ${API_TOKEN}`;
}

const server = http.createServer(async (req, res) => {
  if (req.method === "OPTIONS") return sendJson(res, 204, {});

  const url = new URL(req.url, `http://127.0.0.1:${PORT}`);
  const pathname = url.pathname;

  if (req.method === "GET" && pathname === "/api/health") {
    return sendJson(res, 200, {
      ok: true,
      serverName: "Masjid & Society Local Server",
      version: "1.0.0",
      tokenRequired: Boolean(API_TOKEN),
      collections: COLLECTIONS.length
    });
  }

  if (!isAuthorized(req)) {
    return sendJson(res, 401, { ok: false, error: "Unauthorized: invalid API token" });
  }

  try {
    if (req.method === "GET" && pathname === "/api/data") {
      return sendJson(res, 200, db);
    }

    if (req.method === "POST" && pathname === "/api/auth/login") {
      const body = await readJsonBody(req);
      const email = String(body.email || "admin@masjid.local").trim().toLowerCase();
      const fullName = String(body.fullName || email.split("@")[0] || "Admin").trim();
      const pin = String(body.password || body.pin || "").trim();
      const isAdmin = !ADMIN_PIN || pin === ADMIN_PIN;
      const role = isAdmin ? "super_admin" : "member";
      const userId = `local_${crypto.createHash("sha1").update(email).digest("hex").slice(0, 12)}`;
      const now = Date.now();
      const existingIdx = db.profiles.findIndex(p => p.id === userId || (p.email && p.email.toLowerCase() === email));
      const profile = existingIdx >= 0
        ? { ...db.profiles[existingIdx], fullName, email, updatedAt: now }
        : { id: userId, userId, fullName, email, role, createdAt: now, updatedAt: now };
      if (isAdmin) profile.role = "super_admin";
      if (existingIdx >= 0) db.profiles[existingIdx] = profile;
      else db.profiles.push(profile);
      persistState();
      return sendJson(res, 200, {
        ok: true,
        userId: profile.id,
        fullName: profile.fullName,
        email: profile.email,
        role: profile.role,
        token: API_TOKEN
      });
    }

    if (req.method === "PUT" && pathname === "/api/schedule") {
      const body = await readJsonBody(req);
      db.mosque_schedule = { ...db.mosque_schedule, ...body, id: "main", updatedAt: Date.now() };
      persistState();
      return sendJson(res, 200, { ok: true, data: db.mosque_schedule });
    }

    if (req.method === "POST" && pathname === "/api/restore") {
      const body = await readJsonBody(req);
      if (Array.isArray(body.mosque_schedule) && body.mosque_schedule[0]) {
        db.mosque_schedule = { id: "main", ...body.mosque_schedule[0] };
      } else if (body.mosque_schedule && typeof body.mosque_schedule === "object") {
        db.mosque_schedule = { id: "main", ...body.mosque_schedule };
      }
      for (const col of COLLECTIONS) {
        if (Array.isArray(body[col])) {
          db[col] = body[col];
        }
      }
      persistState();
      return sendJson(res, 200, { ok: true });
    }

    const colMatch = pathname.match(/^\/api\/collections\/([a-z_]+)(?:\/([^/]+))?$/);
    if (colMatch) {
      const [, col, id] = colMatch;
      if (!COLLECTIONS.includes(col)) {
        return sendJson(res, 404, { ok: false, error: `Unknown collection: ${col}` });
      }
      if (req.method === "GET") {
        if (id) {
          const item = db[col].find(x => x.id === id);
          return item ? sendJson(res, 200, { ok: true, data: item }) : sendJson(res, 404, { ok: false, error: "Not found" });
        }
        return sendJson(res, 200, { ok: true, items: db[col] });
      }
      if (req.method === "POST") {
        const body = await readJsonBody(req);
        const docId = String(body.id || id || crypto.randomUUID());
        const now = Date.now();
        const idx = db[col].findIndex(x => x.id === docId);
        const doc = idx >= 0
          ? { ...db[col][idx], ...body, id: docId, updatedAt: now }
          : { ...body, id: docId, createdAt: body.createdAt || now, updatedAt: now };
        if (idx >= 0) db[col][idx] = doc;
        else db[col].push(doc);
        persistState();
        return sendJson(res, 200, { ok: true, data: doc });
      }
      if (req.method === "PUT" && id) {
        const body = await readJsonBody(req);
        const now = Date.now();
        const idx = db[col].findIndex(x => x.id === id);
        if (idx >= 0) {
          db[col][idx] = { ...db[col][idx], ...body, id, updatedAt: now };
        } else {
          db[col].push({ ...body, id, createdAt: now, updatedAt: now });
        }
        persistState();
        return sendJson(res, 200, { ok: true, data: db[col].find(x => x.id === id) });
      }
      if (req.method === "DELETE" && id) {
        db[col] = db[col].filter(x => x.id !== id);
        persistState();
        return sendJson(res, 200, { ok: true });
      }
    }

    return sendJson(res, 404, { ok: false, error: `Route not found: ${req.method} ${pathname}` });
  } catch (err) {
    return sendJson(res, 500, { ok: false, error: err.message });
  }
});

server.listen(PORT, "0.0.0.0", () => {
  console.log(`\n=== Masjid & Society Local Server ===`);
  console.log(`Listening on port: ${PORT}`);
  console.log(`Data file: ${DATA_FILE}`);
  console.log(`Admin PIN: ${ADMIN_PIN}`);
  console.log(`Local URL: http://127.0.0.1:${PORT}`);
  console.log(`Android Emulator URL: http://10.0.2.2:${PORT}`);
  const nets = os.networkInterfaces();
  for (const name of Object.keys(nets)) {
    for (const net of nets[name] || []) {
      if (net.family === "IPv4" && !net.internal) {
        console.log(`LAN URL (${name}): http://${net.address}:${PORT}`);
      }
    }
  }
  console.log("=====================================\n");
});
