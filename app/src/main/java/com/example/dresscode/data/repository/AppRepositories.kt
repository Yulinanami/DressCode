package com.example.dresscode.data.repository

import android.content.Context
import android.util.Patterns
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.dresscode.model.AuthState
import com.example.dresscode.model.OutfitPreview
import com.example.dresscode.model.OutfitUiState
import com.example.dresscode.model.ProfileUiState
import com.example.dresscode.model.TryOnUiState
import com.example.dresscode.model.WeatherUiState
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

class WeatherRepository {
    fun snapshot(): WeatherUiState = WeatherUiState(
        city = "上海",
        summary = "晴·微风",
        temperature = "26°"
    )
}

class OutfitRepository {
    private val featured = listOf(
        OutfitPreview(id = "look-1", title = "夏日通勤", tags = listOf("夏季", "通勤", "简约")),
        OutfitPreview(id = "look-2", title = "周末休闲", tags = listOf("休闲", "牛仔")),
        OutfitPreview(id = "look-3", title = "运动风", tags = listOf("运动", "街头"))
    )

    fun featured(): List<OutfitPreview> = featured

    fun snapshot(): OutfitUiState = OutfitUiState(
        highlight = "精选穿搭 ${featured.size} 套",
        filters = "默认筛选：全部"
    )
}

class TryOnRepository {
    fun snapshot(): TryOnUiState = TryOnUiState(
        status = "尚未提交换装任务",
        hint = "上传人像并选择收藏穿搭"
    )
}

class UserRepository(
    @ApplicationContext private val context: Context
) {

    private val dataStore = context.authDataStore
    private val emailKey = stringPreferencesKey("user_email")
    private val passwordKey = stringPreferencesKey("user_password")
    private val displayNameKey = stringPreferencesKey("display_name")
    private val tokenKey = stringPreferencesKey("auth_token")

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

    suspend fun register(email: String, password: String): AuthResult {
        val trimmedEmail = email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return AuthResult.Error("邮箱格式不正确")
        }
        if (password.length < 6) {
            return AuthResult.Error("密码至少 6 位")
        }
        dataStore.edit { prefs ->
            prefs[emailKey] = trimmedEmail
            prefs[passwordKey] = password
            prefs[displayNameKey] = displayNameFromEmail(trimmedEmail)
            prefs[tokenKey] = UUID.randomUUID().toString()
        }
        return AuthResult.Success
    }

    suspend fun login(email: String, password: String): AuthResult {
        val trimmedEmail = email.trim()
        val current = dataStore.data.first()
        val storedEmail = current[emailKey] ?: return AuthResult.Error("请先注册账号")
        val storedPassword = current[passwordKey] ?: return AuthResult.Error("请先注册账号")
        if (storedEmail != trimmedEmail) {
            return AuthResult.Error("账号不存在或未注册")
        }
        if (storedPassword != password) {
            return AuthResult.Error("密码不正确")
        }
        dataStore.edit { prefs ->
            val existingToken = prefs[tokenKey]?.takeIf { it.isNotBlank() }
            prefs[tokenKey] = existingToken ?: UUID.randomUUID().toString()
        }
        return AuthResult.Success
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
