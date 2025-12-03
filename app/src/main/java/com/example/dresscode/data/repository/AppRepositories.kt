package com.example.dresscode.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.dresscode.model.AuthState
import com.example.dresscode.model.OutfitPreview
import com.example.dresscode.model.ProfileUiState
import com.example.dresscode.model.TryOnUiState
import com.example.dresscode.model.WeatherUiState
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import android.util.Patterns
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

class WeatherRepository @Inject constructor(
    private val client: OkHttpClient,
    @Named("weatherBaseUrl") private val baseUrl: String
) {

    @Volatile
    private var lastSnapshot: WeatherUiState = WeatherUiState(
        city = "杭州",
        summary = "等待天气数据"
    )

    fun snapshot(): WeatherUiState = lastSnapshot

    suspend fun fetch(
        city: String?,
        lat: Double? = null,
        lon: Double? = null
    ): WeatherUiState = withContext(Dispatchers.IO) {
        val normalizedCity: String = city?.takeIf { it.isNotBlank() } ?: lastSnapshot.city
        val encodedCity = URLEncoder.encode(normalizedCity, "UTF-8")
        val queryParams = buildList {
            if (lat != null && lon != null) {
                add("lat=$lat")
                add("lon=$lon")
            } else {
                add("city=$encodedCity")
            }
        }
        val url = buildString {
            append(baseUrl.trimEnd('/')).append("/weather/now")
            if (queryParams.isNotEmpty()) {
                append("?").append(queryParams.joinToString("&"))
            }
        }
        suspend fun requestOnce(): WeatherUiState {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("天气服务异常：HTTP ${response.code}")
                }
                val payload = response.body?.string().orEmpty()
                if (payload.isBlank()) {
                    throw IOException("天气服务返回空响应")
                }
                val json = JSONObject(payload)
                val now = json.optJSONObject("now")
                val cityName = json.optString("city").ifBlank { normalizedCity }
                val temp = now?.optString("temp")
                    ?.takeIf { it.isNotBlank() }
                    ?: json.optString("temperature").takeIf { it.isNotBlank() }
                val summaryParts = mutableListOf<String>()
                val text = now?.optString("text").orEmpty()
                if (text.isNotBlank()) summaryParts.add(text)
                val windDir = now?.optString("wind_dir")
                    ?.takeIf { it.isNotBlank() }
                    ?: now?.optString("windDir")?.takeIf { it.isNotBlank() }
                windDir?.let { summaryParts.add(it) }
                val humidity = now?.optString("humidity").orEmpty()
                if (humidity.isNotBlank()) summaryParts.add("湿度${humidity}%")
                val summary = summaryParts.joinToString(" · ")
                    .ifBlank { json.optString("summary").ifBlank { "天气数据待更新" } }
                lastSnapshot = WeatherUiState(
                    city = cityName,
                    summary = summary,
                    temperature = temp?.let { "${it}°" } ?: "--°",
                    error = null
                )
                return lastSnapshot
            }
        }

        return@withContext runCatching { requestOnce() }
            .getOrElse { firstError ->
                // 简单重试一次，避免偶发 5xx
                delay(500)
                runCatching { requestOnce() }.getOrElse { secondError ->
                    throw secondError.also { lastSnapshot = lastSnapshot.copy(error = it.message) }
                }
            }
    }
}

class TryOnRepository {
    fun snapshot(): TryOnUiState = TryOnUiState(
        status = "尚未提交换装任务",
        hint = "上传人像并选择收藏穿搭"
    )

    suspend fun submitTryOn(
        photoLabel: String?,
        outfit: OutfitPreview?
    ): TryOnUiState {
        delay(300) // placeholder for model call
        return if (photoLabel != null && outfit != null) {
            TryOnUiState(
                status = "已提交换装",
                hint = "等待后端返回效果图",
                selectedPhotoLabel = photoLabel,
                selectedOutfitTitle = outfit.title,
                resultPreview = "请求已排队：${outfit.title}"
            )
        } else {
            TryOnUiState(
                status = "信息不完整",
                hint = "请上传人像并选择收藏穿搭",
                error = "缺少人像或穿搭"
            )
        }
    }
}

class UserRepository(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
    private val authBaseUrl: String
) {

    private val dataStore = context.authDataStore
    private val emailKey = stringPreferencesKey("user_email")
    private val passwordKey = stringPreferencesKey("user_password")
    private val displayNameKey = stringPreferencesKey("display_name")
    private val tokenKey = stringPreferencesKey("auth_token")
    private val tokenExpiresKey = stringPreferencesKey("token_expires_at")

    fun authState(): Flow<AuthState> = dataStore.data.map { prefs ->
        val email = prefs[emailKey]
        val token = prefs[tokenKey]
        val displayName = prefs[displayNameKey] ?: email?.substringBefore("@")?.takeIf { it.isNotBlank() }
        if (email != null && !token.isNullOrBlank()) {
            AuthState(
                isLoggedIn = true,
                email = email,
                displayName = displayName ?: "已登录用户",
                token = token
            )
        } else {
            AuthState()
        }
    }

    suspend fun register(email: String, password: String): AuthResult =
        performAuth("register", email, password)

    suspend fun login(email: String, password: String): AuthResult =
        performAuth("login", email, password)

    private suspend fun performAuth(
        endpoint: String,
        email: String,
        password: String
    ): AuthResult {
        val trimmedEmail = email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return AuthResult.Error("邮箱格式不正确")
        }
        if (password.length < 6) {
            return AuthResult.Error("密码至少 6 位")
        }
        val payload = JSONObject()
            .put("email", trimmedEmail)
            .put("password", password)
        val request = Request.Builder()
            .url("${authBaseUrl.trimEnd('/')}/auth/$endpoint")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val message = parseError(bodyString)
                            ?: "服务异常：HTTP ${response.code}"
                        return@withContext AuthResult.Error(message)
                    }
                    val json = JSONObject(bodyString)
                    val token = json.optString("token")
                    if (token.isBlank()) {
                        return@withContext AuthResult.Error("登录失败：服务器未返回 token")
                    }
                    val displayName = json.optString("display_name")
                        .ifBlank { displayNameFromEmail(trimmedEmail) }
                    val emailFromResponse = json.optString("email").ifBlank { trimmedEmail }
                    val expiresAt = json.optString("expires_at").ifBlank { null }
                    dataStore.edit { prefs ->
                        prefs[emailKey] = emailFromResponse
                        prefs[tokenKey] = token
                        prefs[displayNameKey] = displayName
                        expiresAt?.let { prefs[tokenExpiresKey] = it }
                        prefs[passwordKey] = "" // 清空本地旧密码存储
                    }
                    return@withContext AuthResult.Success
                }
            } catch (e: IOException) {
                return@withContext AuthResult.Error("网络错误：${e.message ?: "请稍后重试"}")
            } catch (e: Exception) {
                return@withContext AuthResult.Error("登录失败：${e.message ?: "未知错误"}")
            }
        }
    }

    private fun parseError(body: String): String? {
        return try {
            val json = JSONObject(body)
            val detail = json.opt("detail")
            when (detail) {
                is JSONObject -> detail.optString("message").ifBlank { detail.optString("code") }
                is String -> detail
                else -> null
            }?.takeIf { it.isNotBlank() }
                ?: json.optString("message").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun logout() {
        dataStore.edit { prefs ->
            // 清空 token 视为退出
            prefs[tokenKey] = ""
        }
    }

    fun profileFor(authState: AuthState): ProfileUiState {
        return if (authState.isLoggedIn) {
            ProfileUiState(
                subtitle = authState.displayName ?: "已登录用户",
                notes = authState.email ?: "已登录"
            )
        } else {
            ProfileUiState(
                subtitle = "游客模式",
                notes = "登录后可同步收藏与设置"
            )
        }
    }

    private fun displayNameFromEmail(email: String): String =
        email.substringBefore("@").ifBlank { "新用户" }
}

sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}
