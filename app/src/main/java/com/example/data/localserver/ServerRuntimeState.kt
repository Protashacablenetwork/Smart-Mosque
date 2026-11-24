package com.example.data.localserver

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.prefs.ServerConfig
import com.example.data.prefs.ServerMode
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings

data class ConnectionTestResult(
    val ok: Boolean,
    val message: String,
    val latencyMs: Long = 0L,
    val serverName: String = "",
    val version: String = ""
)

object ServerRuntimeState {
    private const val TAG = "ServerRuntimeState"

    @Volatile
    private var appliedEmulatorSignature: String? = null

    @Volatile
    private var lastAppliedMode: ServerMode? = null

    fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        return when {
            trimmed.isBlank() -> "http://10.0.2.2:3000"
            trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "http://$trimmed"
        }
    }

    /**
     * Configures Firebase Firestore & Auth to target the Firebase Local Emulator Suite
     * if [config.mode] is [ServerMode.LOCAL_EMULATOR]. Must be called before first Firestore query.
     *
     * Returns true if a process restart is needed to switch cleanly between Cloud Firebase and Emulator.
     */
    @Synchronized
    fun configureEmulatorIfNeeded(
        context: Context,
        config: ServerConfig,
        auth: FirebaseAuth = Firebase.auth
    ): Boolean {
        val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        val previousMode = lastAppliedMode

        if (config.mode == ServerMode.LOCAL_EMULATOR) {
            val host = config.emulatorHost.trim().ifBlank { "10.0.2.2" }
            val sig = "$host:${config.firestorePort}:${config.authPort}"
            if (appliedEmulatorSignature == sig) {
                lastAppliedMode = ServerMode.LOCAL_EMULATOR
                return false
            }
            if (appliedEmulatorSignature != null || previousMode == ServerMode.CLOUD_FIREBASE) {
                return true
            }
            return try {
                db.useEmulator(host, config.firestorePort)
                db.firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    .build()
                auth.useEmulator(host, config.authPort)
                appliedEmulatorSignature = sig
                lastAppliedMode = ServerMode.LOCAL_EMULATOR
                Log.i(TAG, "Configured Firebase Emulator at $sig")
                false
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Firestore already started; restart required to apply emulator config", e)
                true
            }
        } else if (config.mode == ServerMode.CLOUD_FIREBASE) {
            if (appliedEmulatorSignature != null) {
                return true
            }
            lastAppliedMode = ServerMode.CLOUD_FIREBASE
            return false
        }
        return false
    }

    fun requiresRestartForSwitch(from: ServerMode, to: ServerMode): Boolean {
        if (from == to) return false
        return (from == ServerMode.LOCAL_EMULATOR && to == ServerMode.CLOUD_FIREBASE) ||
            (from == ServerMode.CLOUD_FIREBASE && to == ServerMode.LOCAL_EMULATOR) ||
            (from == ServerMode.LOCAL_EMULATOR && appliedEmulatorSignature != null && to == ServerMode.CLOUD_FIREBASE)
    }

    val companionNodeServerScript: String = """
/**
 * Masjid & Society Local Server (Zero-Dependency Node.js)
 * Run: node server.js   (or PORT=3000 API_TOKEN=secret node server.js)
 */
const http = require("http");
const fs = require("fs");
const path = require("path");
const os = require("os");
const crypto = require("crypto");

const PORT = Number(process.env.PORT || 3000);
const DATA_FILE = path.join(__dirname, "masjid-data.json");
const API_TOKEN = process.env.API_TOKEN || "";
const ADMIN_PIN = process.env.ADMIN_PIN || "1234";

const COLLECTIONS = [
  "families","members","dues","payments","donations","expenses",
  "bank_accounts","budgets","qurbani_batches","meat_distributions",
  "notices","committees","meetings","events","assets","documents",
  "welfare_records","complaints","audit_logs","profiles"
];

function emptyState() {
  const state = {
    mosque_schedule: {
      id: "main",
      mosqueName: "বায়তুল আমান জামে মসজিদ",
      fajrTime: "05:15 AM", sunriseTime: "06:22 AM",
      dhuhrTime: "01:15 PM", asrTime: "04:15 PM",
      maghribTime: "05:30 PM", ishaTime: "07:30 PM",
      jumuahTime: "01:30 PM"
    }
  };
  for (const c of COLLECTIONS) state[c] = [];
  return state;
}

function loadState() {
  try {
    if (fs.existsSync(DATA_FILE)) {
      const parsed = JSON.parse(fs.readFileSync(DATA_FILE, "utf8"));
      const base = emptyState();
      if (parsed.mosque_schedule) base.mosque_schedule = parsed.mosque_schedule;
      for (const c of COLLECTIONS) if (Array.isArray(parsed[c])) base[c] = parsed[c];
      return base;
    }
  } catch (e) { console.error("Load error:", e.message); }
  return emptyState();
}

let db = loadState();
function saveState() {
  fs.writeFileSync(DATA_FILE, JSON.stringify(db, null, 2), "utf8");
}

function send(res, code, obj) {
  res.writeHead(code, {
    "Content-Type": "application/json; charset=utf-8",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET,POST,PUT,DELETE,OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type,Authorization"
  });
  res.end(JSON.stringify(obj));
}

function readJson(req) {
  return new Promise((resolve, reject) => {
    let body = "";
    req.on("data", chunk => { body += chunk; if (body.length > 10e6) req.destroy(); });
    req.on("end", () => {
      try { resolve(body ? JSON.parse(body) : {}); } catch (e) { reject(e); }
    });
    req.on("error", reject);
  });
}

http.createServer(async (req, res) => {
  if (req.method === "OPTIONS") return send(res, 204, {});
  const url = new URL(req.url, "http://localhost");
  const p = url.pathname;

  if (API_TOKEN && p !== "/api/health") {
    const auth = req.headers.authorization || "";
    if (auth !== "Bearer " + API_TOKEN) return send(res, 401, { ok: false, error: "Invalid API Token" });
  }

  try {
    if (req.method === "GET" && p === "/api/health") {
      return send(res, 200, { ok: true, serverName: "Masjid Local Node Server", version: "1.0.0" });
    }
    if (req.method === "GET" && p === "/api/data") {
      return send(res, 200, db);
    }
    if (req.method === "POST" && p === "/api/auth/login") {
      const b = await readJson(req);
      const email = String(b.email || "admin@masjid.local").trim().toLowerCase();
      const fullName = String(b.fullName || email.split("@")[0] || "Admin").trim();
      const pin = String(b.password || b.pin || "").trim();
      const role = (!ADMIN_PIN || pin === ADMIN_PIN) ? "super_admin" : "member";
      const userId = "local_" + crypto.createHash("sha1").update(email).digest("hex").slice(0, 12);
      const prof = { id: userId, userId, fullName, email, role, updatedAt: Date.now() };
      const idx = db.profiles.findIndex(x => x.id === userId);
      if (idx >= 0) db.profiles[idx] = { ...db.profiles[idx], ...prof };
      else db.profiles.push({ ...prof, createdAt: Date.now() });
      saveState();
      return send(res, 200, { ok: true, userId, fullName, email, role, token: API_TOKEN });
    }
    if (req.method === "PUT" && p === "/api/schedule") {
      const b = await readJson(req);
      db.mosque_schedule = { ...db.mosque_schedule, ...b, id: "main", updatedAt: Date.now() };
      saveState();
      return send(res, 200, { ok: true, data: db.mosque_schedule });
    }
    if (req.method === "POST" && p === "/api/restore") {
      const b = await readJson(req);
      if (Array.isArray(b.mosque_schedule) && b.mosque_schedule[0]) db.mosque_schedule = { id: "main", ...b.mosque_schedule[0] };
      else if (b.mosque_schedule && typeof b.mosque_schedule === "object") db.mosque_schedule = { id: "main", ...b.mosque_schedule };
      for (const c of COLLECTIONS) if (Array.isArray(b[c])) db[c] = b[c];
      saveState();
      return send(res, 200, { ok: true });
    }
    const colMatch = p.match(/^\/api\/collections\/([a-z_]+)(?:\/([^/]+))?$/);
    if (colMatch) {
      const [, col, id] = colMatch;
      if (!COLLECTIONS.includes(col)) return send(res, 404, { ok: false, error: "Unknown collection" });
      if (req.method === "GET") return send(res, 200, { ok: true, items: db[col] });
      if (req.method === "POST") {
        const b = await readJson(req);
        const docId = b.id || id || crypto.randomUUID();
        const now = Date.now();
        const idx = db[col].findIndex(x => x.id === docId);
        const doc = idx >= 0 ? { ...db[col][idx], ...b, id: docId, updatedAt: now }
                             : { ...b, id: docId, createdAt: b.createdAt || now, updatedAt: now };
        if (idx >= 0) db[col][idx] = doc; else db[col].push(doc);
        saveState();
        return send(res, 200, { ok: true, data: doc });
      }
      if (req.method === "PUT" && id) {
        const b = await readJson(req);
        const idx = db[col].findIndex(x => x.id === id);
        if (idx < 0) db[col].push({ ...b, id, createdAt: Date.now(), updatedAt: Date.now() });
        else db[col][idx] = { ...db[col][idx], ...b, id, updatedAt: Date.now() };
        saveState();
        return send(res, 200, { ok: true, data: db[col].find(x => x.id === id) });
      }
      if (req.method === "DELETE" && id) {
        db[col] = db[col].filter(x => x.id !== id);
        saveState();
        return send(res, 200, { ok: true });
      }
    }
    return send(res, 404, { ok: false, error: "Not found" });
  } catch (e) {
    return send(res, 500, { ok: false, error: e.message });
  }
}).listen(PORT, "0.0.0.0", () => {
  console.log("Masjid Local Server running on port " + PORT);
  const nets = os.networkInterfaces();
  for (const name of Object.keys(nets)) {
    for (const net of nets[name] || []) {
      if (net.family === "IPv4" && !net.internal) console.log("  LAN URL: http://" + net.address + ":" + PORT);
    }
  }
});
""".trimIndent()
}
