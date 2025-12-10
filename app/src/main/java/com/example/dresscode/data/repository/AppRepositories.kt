package com.example.dresscode.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.dresscode.model.AuthState
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
import okhttp3.MultipartBody
import org.json.JSONObject
import android.util.Patterns
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.net.SocketTimeoutException
import android.util.Base64

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

data class TryOnImage(
    val fileName: String,
    val bytes: ByteArray,
    val mimeType: String = "image/*"
)

class TryOnRepository @Inject constructor(
    private val client: OkHttpClient,
    @Named("apiBaseUrl") private val baseUrl: String
) {
    @Volatile
    private var pendingRecommendedOutfit: Pair<String, String>? = null

    fun snapshot(): TryOnUiState = TryOnUiState(
        status = "尚未提交换装任务",
        hint = "上传人像与穿搭"
    )

    suspend fun submitTryOn(
        portrait: TryOnImage,
        outfit: TryOnImage,
        model: String? = null
    ): TryOnUiState = withContext(Dispatchers.IO) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "user_image",
                portrait.fileName,
                portrait.bytes.toRequestBody(portrait.mimeType.toMediaType())
            )
            .addFormDataPart(
                "outfit_image",
                outfit.fileName,
                outfit.bytes.toRequestBody(outfit.mimeType.toMediaType())
            )
            .apply {
                model?.let { addFormDataPart("model", it) }
            }
            .build()
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/tryon")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val payload = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = parseError(payload) ?: "换装失败：HTTP ${response.code}"
                    throw IOException(message)
                }
                if (payload.isBlank()) {
                    throw IOException("换装失败：服务器未返回内容，请稍后重试")
                }
                val json = JSONObject(payload)
                val resultBase64 = json.optString("resultImageBase64")
                    .ifBlank { json.optString("result_image_base64") }
                var finalBase64 = resultBase64
                if (finalBase64.isBlank()) {
                    val imageUrl = json.optString("imageUrl").ifBlank { null }
                    if (imageUrl != null) {
                        val bytes = downloadImage(imageUrl)
                        if (bytes != null) {
                            finalBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        }
                    }
                }
                if (finalBase64.isBlank()) {
                    throw IOException("换装失败：未返回结果图")
                }
                return@withContext TryOnUiState(
                    status = "换装完成",
                    hint = "如需调整，可重新提交任务",
                    selectedPhotoLabel = portrait.fileName,
                    selectedOutfitTitle = outfit.fileName,
                    resultPreview = "换装完成",
                    selectedPhotoBytes = portrait.bytes,
                    selectedOutfitBytes = outfit.bytes,
                    resultImageBase64 = finalBase64
                )
            }
        } catch (e: SocketTimeoutException) {
            throw IOException("换装超时，请稍后重试或检查网络", e)
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("换装失败：${e.message ?: "未知错误"}", e)
        }
    }

    fun setPendingRecommendedOutfit(title: String, imageUrl: String) {
        pendingRecommendedOutfit = title to imageUrl
    }

    fun consumePendingRecommendedOutfit(): Pair<String, String>? {
        val value = pendingRecommendedOutfit
        pendingRecommendedOutfit = null
        return value
    }

    suspend fun downloadImage(url: String): ByteArray? = withContext(Dispatchers.IO) {
        val resolvedUrl = if (url.startsWith("http")) url else buildString {
            append(baseUrl.trimEnd('/'))
            if (!url.startsWith("/")) append("/")
            append(url)
        }
        val request = Request.Builder().url(resolvedUrl).get().build()
        return@withContext try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    null
                } else {
                    resp.body?.bytes()
                }
            }
        } catch (_: Exception) {
            null
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

    fun authState(): Flow<AuthState> = dataStore.data.map { prefs ->
        val email = prefs[emailKey]
        val password = prefs[passwordKey]
        val displayName = prefs[displayNameKey] ?: email?.substringBefore("@")?.takeIf { it.isNotBlank() }
        if (!email.isNullOrBlank() && !password.isNullOrBlank()) {
            AuthState(
                isLoggedIn = true,
                email = email,
                displayName = displayName ?: "已登录用户",
                password = password
            )
        } else {
            AuthState()
        }
    }

    suspend fun register(email: String, password: String, displayName: String): AuthResult =
        performAuth("register", email, password, displayName)

    suspend fun login(email: String, password: String): AuthResult =
        performAuth("login", email, password, null)

    private suspend fun performAuth(
        endpoint: String,
        email: String,
        password: String,
        displayName: String?
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
        displayName?.takeIf { it.isNotBlank() }?.let { payload.put("display_name", it) }
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
                    val displayNameFromResponse = json.optString("display_name")
                        .ifBlank { displayNameFromEmail(trimmedEmail) }
                    val emailFromResponse = json.optString("email").ifBlank { trimmedEmail }
                    dataStore.edit { prefs ->
                        prefs[emailKey] = emailFromResponse
                        prefs[passwordKey] = password
                        prefs[displayNameKey] = displayNameFromResponse
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
            prefs[emailKey] = ""
            prefs[passwordKey] = ""
            prefs[displayNameKey] = ""
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
