package com.whattocook.app.data.remote.turso

import android.util.Patterns
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.whattocook.app.BuildConfig
import com.whattocook.app.data.auth.PasswordHasher
import com.whattocook.app.di.AuthStore
import com.whattocook.app.domain.model.AppError
import com.whattocook.app.domain.model.AppResult
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class TursoClient @Inject constructor(
    @Named("auth") private val client: OkHttpClient,
    private val json: Json,
    @AuthStore private val store: DataStore<Preferences>,
) {
    private var schemaInitialized = false
    private var cachedCustomUrl: String? = null
    private var cachedCustomToken: String? = null

    // Local in-memory store for fallback/demo mode when Turso secrets are not yet pasted
    private val demoUsers = ConcurrentHashMap<String, Pair<String, String>>().apply {
        val demoSalt = PasswordHasher.generateSalt()
        val demoHash = PasswordHasher.hash("password123", demoSalt)
        put("demo@example.com", demoHash to demoSalt)
    }

    val isConfigured: Boolean
        get() {
            val customUrl = cachedCustomUrl?.trim().orEmpty()
            val customToken = cachedCustomToken?.trim().orEmpty()
            if (customUrl.isNotEmpty() && customToken.isNotEmpty()) {
                return true
            }
            val url = getRawDatabaseUrl().trim()
            val token = getRawAuthToken().trim()
            return url.isNotEmpty() &&
                !url.contains("your-database") &&
                token.isNotEmpty() &&
                !token.contains("your_turso_auth_token")
        }

    suspend fun getEffectiveDatabaseUrl(): String {
        if (cachedCustomUrl == null) {
            loadCustomCredentials()
        }
        val custom = cachedCustomUrl?.trim().orEmpty()
        if (custom.isNotEmpty()) return custom
        return getRawDatabaseUrl()
    }

    suspend fun getEffectiveAuthToken(): String {
        if (cachedCustomToken == null) {
            loadCustomCredentials()
        }
        val custom = cachedCustomToken?.trim().orEmpty()
        if (custom.isNotEmpty()) return custom
        return getRawAuthToken()
    }

    private suspend fun loadCustomCredentials() {
        val prefs = store.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .first()
        cachedCustomUrl = prefs[TURSO_CUSTOM_URL]
        cachedCustomToken = prefs[TURSO_CUSTOM_TOKEN]
    }

    suspend fun testConnection(urlRaw: String, tokenRaw: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        val trimmedUrl = urlRaw.trim()
        val trimmedToken = tokenRaw.trim()
        if (trimmedUrl.isEmpty()) {
            return@withContext AppResult.Failure(AppError.Unknown("Database URL cannot be empty."))
        }
        if (trimmedToken.isEmpty()) {
            return@withContext AppResult.Failure(AppError.Unknown("Auth token cannot be empty."))
        }
        val endpoint = formatPipelineUrl(trimmedUrl)
            ?: return@withContext AppResult.Failure(AppError.Unknown("Invalid Turso Database URL format."))

        val requestObj = TursoPipelineRequest(
            requests = listOf(
                TursoPipelineStep(
                    type = "execute",
                    stmt = TursoStmt(sql = "SELECT 1;", args = emptyList()),
                ),
                TursoPipelineStep(type = "close"),
            ),
        )

        val bodyString = json.encodeToString(TursoPipelineRequest.serializer(), requestObj)
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $trimmedToken")
            .addHeader("Content-Type", "application/json")
            .post(bodyString.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            response.use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.code == 401 || resp.code == 403) {
                    return@withContext AppResult.Failure(AppError.Unknown("Authentication failed: Invalid Turso Auth Token."))
                }
                if (!resp.isSuccessful) {
                    return@withContext AppResult.Failure(AppError.Unknown("Turso returned HTTP ${resp.code}: $body"))
                }
                val pipelineResponse = json.decodeFromString(TursoPipelineResponse.serializer(), body)
                val firstResult = pipelineResponse.results.firstOrNull()
                if (firstResult?.type == "error" || firstResult?.error != null) {
                    val err = firstResult?.error?.message ?: "Database query failed."
                    return@withContext AppResult.Failure(AppError.Unknown(err))
                }
                AppResult.Success(Unit)
            }
        } catch (e: IOException) {
            AppResult.Failure(AppError.Unknown("Network connection error: ${e.message}"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown("Failed to connect to Turso: ${e.message}"))
        }
    }

    suspend fun saveCustomCredentials(url: String, token: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        val trimmedUrl = url.trim()
        val trimmedToken = token.trim()
        val testRes = testConnection(trimmedUrl, trimmedToken)
        if (testRes is AppResult.Failure) {
            return@withContext testRes
        }
        store.edit { prefs ->
            prefs[TURSO_CUSTOM_URL] = trimmedUrl
            prefs[TURSO_CUSTOM_TOKEN] = trimmedToken
        }
        cachedCustomUrl = trimmedUrl
        cachedCustomToken = trimmedToken
        schemaInitialized = false
        AppResult.Success(Unit)
    }

    suspend fun clearCustomCredentials() = withContext(Dispatchers.IO) {
        store.edit { prefs ->
            prefs.remove(TURSO_CUSTOM_URL)
            prefs.remove(TURSO_CUSTOM_TOKEN)
        }
        cachedCustomUrl = null
        cachedCustomToken = null
        schemaInitialized = false
    }

    suspend fun signUp(emailRaw: String, password: String): AppResult<TursoUser> = withContext(Dispatchers.IO) {
        val email = emailRaw.trim().lowercase()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return@withContext AppResult.Failure(AppError.Unknown("Please enter a valid email address."))
        }
        if (password.length < 6) {
            return@withContext AppResult.Failure(AppError.Unknown("Password must be at least 6 characters long."))
        }

        if (!isConfigured) {
            // Local demo simulation mode
            if (demoUsers.containsKey(email)) {
                return@withContext AppResult.Failure(AppError.Unknown("An account with this email already exists."))
            }
            val salt = PasswordHasher.generateSalt()
            val hash = PasswordHasher.hash(password, salt)
            demoUsers[email] = hash to salt
            return@withContext AppResult.Success(TursoUser(id = "user_${UUID.randomUUID()}", email = email))
        }

        ensureSchema()

        // 1. Check if user already exists
        val existingCheck = query(
            sql = "SELECT id FROM users WHERE lower(email) = lower(?) LIMIT 1;",
            args = listOf(TursoArg(type = "text", value = email)),
        )
        when (existingCheck) {
            is AppResult.Failure -> return@withContext existingCheck
            is AppResult.Success -> {
                if (existingCheck.data.isNotEmpty()) {
                    return@withContext AppResult.Failure(AppError.Unknown("An account with this email already exists."))
                }
            }
        }

        // 2. Hash password with secure salt
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hash(password, salt)
        val userId = "usr_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()

        // 3. Insert into Turso database
        val insertResult = execute(
            sql = "INSERT INTO users (id, email, password_hash, salt, created_at) VALUES (?, ?, ?, ?, ?);",
            args = listOf(
                TursoArg(type = "text", value = userId),
                TursoArg(type = "text", value = email),
                TursoArg(type = "text", value = hash),
                TursoArg(type = "text", value = salt),
                TursoArg(type = "integer", value = now.toString()),
            ),
        )

        when (insertResult) {
            is AppResult.Success -> AppResult.Success(TursoUser(id = userId, email = email))
            is AppResult.Failure -> insertResult
        }
    }

    suspend fun signIn(emailRaw: String, password: String): AppResult<TursoUser> = withContext(Dispatchers.IO) {
        val email = emailRaw.trim().lowercase()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return@withContext AppResult.Failure(AppError.Unknown("Please enter a valid email address."))
        }
        if (password.isEmpty()) {
            return@withContext AppResult.Failure(AppError.Unknown("Password cannot be empty."))
        }

        if (!isConfigured) {
            // Local demo simulation mode
            val userRecord = demoUsers[email]
                ?: return@withContext AppResult.Failure(AppError.Unknown("No account found for $email. (Tip: Demo account is demo@example.com / password123)"))
            val (hash, salt) = userRecord
            return@withContext if (PasswordHasher.verify(password, salt, hash)) {
                AppResult.Success(TursoUser(id = "user_${UUID.nameUUIDFromBytes(email.toByteArray())}", email = email))
            } else {
                AppResult.Failure(AppError.Unknown("Incorrect password."))
            }
        }

        ensureSchema()

        val result = query(
            sql = "SELECT id, email, password_hash, salt FROM users WHERE lower(email) = lower(?) LIMIT 1;",
            args = listOf(TursoArg(type = "text", value = email)),
        )

        when (result) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                val row = result.data.firstOrNull()
                    ?: return@withContext AppResult.Failure(AppError.Unknown("No account found with this email."))

                val id = row["id"] ?: return@withContext AppResult.Failure(AppError.Unknown("Invalid user record in database."))
                val expectedHash = row["password_hash"] ?: ""
                val salt = row["salt"] ?: ""

                if (!PasswordHasher.verify(password, salt, expectedHash)) {
                    return@withContext AppResult.Failure(AppError.Unknown("Incorrect password."))
                }

                AppResult.Success(TursoUser(id = id, email = email))
            }
        }
    }

    suspend fun deleteUser(userId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext AppResult.Success(Unit)
        }
        val result = execute(
            sql = "DELETE FROM users WHERE id = ?;",
            args = listOf(TursoArg(type = "text", value = userId)),
        )
        when (result) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }
    }

    private suspend fun ensureSchema() {
        if (schemaInitialized) return
        val createSql = """
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                email TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                salt TEXT NOT NULL,
                created_at INTEGER NOT NULL
            );
        """.trimIndent()

        execute(createSql)
        schemaInitialized = true
    }

    private suspend fun query(sql: String, args: List<TursoArg> = emptyList()): AppResult<List<Map<String, String?>>> {
        val result = execute(sql, args)
        return when (result) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                val data = result.data
                val cols = data.cols.map { it.name }
                val rows = data.rows.map { rowValues ->
                    cols.indices.associate { index ->
                        val colName = cols[index]
                        val cellValue = rowValues.getOrNull(index)?.extractString()
                        colName to cellValue
                    }
                }
                AppResult.Success(rows)
            }
        }
    }

    private suspend fun execute(sql: String, args: List<TursoArg> = emptyList()): AppResult<TursoResultData> {
        val endpoint = getPipelineUrl() ?: return AppResult.Failure(AppError.NotConfigured)
        val authToken = getEffectiveAuthToken().trim()

        val requestObj = TursoPipelineRequest(
            requests = listOf(
                TursoPipelineStep(
                    type = "execute",
                    stmt = TursoStmt(sql = sql, args = args),
                ),
                TursoPipelineStep(type = "close"),
            ),
        )

        val bodyString = json.encodeToString(TursoPipelineRequest.serializer(), requestObj)
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .post(bodyString.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.use { resp ->
                val body = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    return AppResult.Failure(AppError.Server(resp.code))
                }
                val pipelineResponse = json.decodeFromString(TursoPipelineResponse.serializer(), body)
                val firstResult = pipelineResponse.results.firstOrNull()
                    ?: return AppResult.Failure(AppError.Unknown("Empty response from database pipeline."))

                if (firstResult.type == "error" || firstResult.error != null) {
                    val msg = firstResult.error?.message ?: "Database query error"
                    return AppResult.Failure(AppError.Unknown(msg))
                }

                val resultData = firstResult.response?.result ?: TursoResultData()
                AppResult.Success(resultData)
            }
        } catch (e: IOException) {
            AppResult.Failure(AppError.NoNetwork)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.localizedMessage ?: "Unexpected database error"))
        }
    }

    private suspend fun getPipelineUrl(): String? {
        val raw = getEffectiveDatabaseUrl().trim()
        return formatPipelineUrl(raw)
    }

    private fun formatPipelineUrl(raw: String): String? {
        if (raw.isEmpty()) return null
        val clean = raw.replace("libsql://", "https://")
            .let { if (!it.startsWith("http://") && !it.startsWith("https://")) "https://$it" else it }
            .trimEnd('/')
            .removeSuffix("/v2/pipeline")
        return "$clean/v2/pipeline"
    }

    private fun getRawDatabaseUrl(): String {
        return try {
            BuildConfig::class.java.getField("TURSO_DATABASE_URL").get(null) as? String ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun getRawAuthToken(): String {
        return try {
            BuildConfig::class.java.getField("TURSO_AUTH_TOKEN").get(null) as? String ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private companion object {
        val TURSO_CUSTOM_URL = stringPreferencesKey("turso_custom_url")
        val TURSO_CUSTOM_TOKEN = stringPreferencesKey("turso_custom_token")
    }
}
