package com.example.data.localserver

import android.util.Log
import com.example.data.model.AuditLogEntry
import com.example.data.model.SocietyRoles
import com.example.data.model.UserProfile
import com.example.data.prefs.LocalAuthSession
import com.example.data.prefs.ServerConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LocalHttpBackend(
    initialConfig: ServerConfig = ServerConfig(),
    initialSession: LocalAuthSession? = null
) {
    private companion object {
        const val TAG = "LocalHttpBackend"
        const val CONNECT_TIMEOUT_MS = 5000
        const val READ_TIMEOUT_MS = 8000
        const val POLL_INTERVAL_MS = 6000L
    }

    @Volatile
    var config: ServerConfig = initialConfig
        private set

    @Volatile
    var session: LocalAuthSession? = initialSession
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null

    private val _snapshot = MutableStateFlow(LocalSnapshotData())
    val snapshot: StateFlow<LocalSnapshotData> = _snapshot.asStateFlow()

    fun updateRuntime(newConfig: ServerConfig, newSession: LocalAuthSession?) {
        config = newConfig
        session = newSession
    }

    fun startSyncLoop() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (isActive) {
                runCatching { refreshSnapshot() }
                    .onFailure { if (it !is CancellationException) Log.w(TAG, "Local sync failed: ${it.message}") }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stopSyncLoop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun activeToken(): String = session?.token?.takeIf { it.isNotBlank() } ?: config.apiToken

    fun requireUid(): String = session?.userId?.takeIf { it.isNotBlank() }
        ?: throw IllegalStateException("Please sign in to the local server before editing data.")

    fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    suspend fun testConnection(rawUrl: String, token: String): ConnectionTestResult = withContext(Dispatchers.IO) {
        val baseUrl = ServerRuntimeState.normalizeUrl(rawUrl)
        val started = System.currentTimeMillis()
        try {
            val healthResp = httpRequest("GET", "$baseUrl/api/health", null, token)
            val latency = System.currentTimeMillis() - started
            if (healthResp.code in 200..299) {
                val obj = runCatching { JSONObject(healthResp.body) }.getOrNull()
                val name = obj?.optString("serverName")?.takeIf { it.isNotBlank() } ?: "Local HTTP Server"
                val version = obj?.optString("version")?.takeIf { it.isNotBlank() } ?: "1.0"
                return@withContext ConnectionTestResult(
                    ok = true,
                    message = "Connected to $name ($latency ms)",
                    latencyMs = latency,
                    serverName = name,
                    version = version
                )
            }
            if (healthResp.code == 401 || healthResp.code == 403) {
                return@withContext ConnectionTestResult(false, "Unauthorized (${healthResp.code}): Check API token", latency)
            }
            // Fallback probe for /api/data if server has no /api/health
            val dataResp = httpRequest("GET", "$baseUrl/api/data", null, token)
            val dataLatency = System.currentTimeMillis() - started
            if (dataResp.code in 200..299) {
                return@withContext ConnectionTestResult(
                    ok = true,
                    message = "Connected to Local Server ($dataLatency ms)",
                    latencyMs = dataLatency,
                    serverName = "Custom Local Server"
                )
            }
            ConnectionTestResult(false, "HTTP ${healthResp.code}: ${healthResp.body.take(120)}", latency)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ConnectionTestResult(false, e.localizedMessage ?: "Connection failed")
        }
    }

    suspend fun signInLocalServer(
        fullName: String,
        email: String,
        passwordOrPin: String
    ): Result<LocalAuthSession> = withContext(Dispatchers.IO) {
        val baseUrl = ServerRuntimeState.normalizeUrl(config.serverUrl)
        val cleanEmail = email.trim().lowercase().ifBlank { "admin@masjid.local" }
        val cleanName = fullName.trim().ifBlank { cleanEmail.substringBefore("@").ifBlank { "Admin" } }
        try {
            val body = JSONObject().apply {
                put("email", cleanEmail)
                put("fullName", cleanName)
                put("password", passwordOrPin)
                put("pin", passwordOrPin)
            }
            val resp = httpRequest("POST", "$baseUrl/api/auth/login", body.toString(), config.apiToken)
            if (resp.code in 200..299) {
                val obj = JSONObject(resp.body)
                val newSession = LocalAuthSession(
                    userId = obj.optString("userId").ifBlank { "local_${cleanEmail.hashCode()}" },
                    fullName = obj.optString("fullName").ifBlank { cleanName },
                    email = obj.optString("email").ifBlank { cleanEmail },
                    role = obj.optString("role").ifBlank { SocietyRoles.SUPER_ADMIN },
                    token = obj.optString("token").ifBlank { config.apiToken }
                )
                session = newSession
                runCatching { refreshSnapshot() }
                return@withContext Result.success(newSession)
            }
            if (resp.code == 404) {
                // Allow connecting to lightweight REST servers that only implement /api/data + /api/collections
                val health = testConnection(baseUrl, config.apiToken)
                if (health.ok) {
                    val fallback = LocalAuthSession(
                        userId = "local_${cleanEmail.hashCode().toString().replace("-", "n")}",
                        fullName = cleanName,
                        email = cleanEmail,
                        role = SocietyRoles.SUPER_ADMIN,
                        token = config.apiToken
                    )
                    session = fallback
                    runCatching { refreshSnapshot() }
                    return@withContext Result.success(fallback)
                }
            }
            val msg = runCatching { JSONObject(resp.body).optString("error") }.getOrNull()
                ?.takeIf { it.isNotBlank() } ?: "Login failed (HTTP ${resp.code})"
            Result.failure(IllegalStateException(msg))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshSnapshot(): Result<LocalSnapshotData> = withContext(Dispatchers.IO) {
        val baseUrl = ServerRuntimeState.normalizeUrl(config.serverUrl)
        try {
            val resp = httpRequest("GET", "$baseUrl/api/data", null, activeToken())
            if (resp.code in 200..299) {
                val parsed = LocalServerJsonCodec.parseSnapshot(JSONObject(resp.body))
                _snapshot.value = parsed
                return@withContext Result.success(parsed)
            }
            Result.failure(IllegalStateException("Server returned HTTP ${resp.code}"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertDoc(collection: String, id: String?, fields: Map<String, Any?>): Result<String> = withContext(Dispatchers.IO) {
        val baseUrl = ServerRuntimeState.normalizeUrl(config.serverUrl)
        val docId = id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString().replace("-", "").take(20)
        try {
            val json = JSONObject()
            json.put("id", docId)
            fields.forEach { (k, v) -> if (v != null) json.put(k, v) }
            val resp = if (id.isNullOrBlank()) {
                httpRequest("POST", "$baseUrl/api/collections/$collection", json.toString(), activeToken())
            } else {
                httpRequest("PUT", "$baseUrl/api/collections/$collection/$docId", json.toString(), activeToken())
            }
            if (resp.code in 200..299) {
                val returnedId = runCatching {
                    JSONObject(resp.body).optJSONObject("data")?.optString("id")?.takeIf { it.isNotBlank() }
                }.getOrNull() ?: docId
                refreshSnapshot()
                Result.success(returnedId)
            } else {
                Result.failure(IllegalStateException(extractError(resp)))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDoc(collection: String, id: String): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = ServerRuntimeState.normalizeUrl(config.serverUrl)
        try {
            val resp = httpRequest("DELETE", "$baseUrl/api/collections/$collection/$id", null, activeToken())
            if (resp.code in 200..299) {
                refreshSnapshot()
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException(extractError(resp)))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun putSchedule(payload: Map<String, Any?>): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = ServerRuntimeState.normalizeUrl(config.serverUrl)
        try {
            val json = JSONObject()
            payload.forEach { (k, v) -> if (v != null) json.put(k, v) }
            val resp = httpRequest("PUT", "$baseUrl/api/schedule", json.toString(), activeToken())
            if (resp.code in 200..299) {
                refreshSnapshot()
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException(extractError(resp)))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun postRestore(rootJson: JSONObject): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = ServerRuntimeState.normalizeUrl(config.serverUrl)
        try {
            val resp = httpRequest("POST", "$baseUrl/api/restore", rootJson.toString(), activeToken())
            if (resp.code in 200..299) {
                refreshSnapshot()
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException(extractError(resp)))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logAudit(
        action: String,
        tableName: String,
        recordId: String?,
        summary: String?,
        beforeSummary: String? = null,
        afterSummary: String? = null
    ) {
        val s = session ?: return
        runCatching {
            val baseUrl = ServerRuntimeState.normalizeUrl(config.serverUrl)
            val obj = JSONObject().apply {
                put("id", UUID.randomUUID().toString().replace("-", "").take(20))
                put("userId", s.userId)
                put("userEmail", s.email)
                put("action", action.take(100))
                put("tableName", tableName.take(50))
                recordId?.let { put("recordId", it.take(128)) }
                summary?.let { put("summary", it.take(250)) }
                beforeSummary?.let { put("beforeSummary", it.take(300)) }
                afterSummary?.let { put("afterSummary", it.take(300)) }
                put("createdAt", System.currentTimeMillis())
            }
            httpRequest("POST", "$baseUrl/api/collections/audit_logs", obj.toString(), activeToken())
        }
    }

    fun getCurrentProfileOrNull(uid: String): UserProfile? {
        val fromSnap = _snapshot.value.profiles.firstOrNull { it.id == uid || it.userId == uid }
        if (fromSnap != null) return fromSnap
        val s = session ?: return null
        if (s.userId != uid) return null
        return UserProfile(
            id = s.userId,
            userId = s.userId,
            fullName = s.fullName,
            email = s.email,
            role = s.role
        )
    }

    private fun extractError(resp: HttpResponse): String {
        val msg = runCatching { JSONObject(resp.body).optString("error") }.getOrNull()
        return msg?.takeIf { it.isNotBlank() } ?: "Server error (HTTP ${resp.code})"
    }

    private data class HttpResponse(val code: Int, val body: String)

    private fun httpRequest(method: String, urlStr: String, body: String?, token: String): HttpResponse {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            if (token.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        return try {
            if (body != null) {
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(body)
                    writer.flush()
                }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
            val text = stream?.let { s ->
                BufferedReader(InputStreamReader(s, Charsets.UTF_8)).use { it.readText() }
            } ?: ""
            HttpResponse(code, text)
        } finally {
            conn.disconnect()
        }
    }

    fun mapToJson(map: Map<String, Any?>): JSONObject = JSONObject().apply {
        map.forEach { (k, v) -> if (v != null) put(k, v) }
    }

    fun listToJsonArray(items: List<Map<String, Any?>>): JSONArray = JSONArray().apply {
        items.forEach { put(mapToJson(it)) }
    }
}
