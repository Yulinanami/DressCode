package com.example.dresscode.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.dresscode.data.repository.AuthResult
import com.example.dresscode.data.repository.UserRepository
import com.example.dresscode.model.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

enum class AuthMode { LOGIN, REGISTER }

data class LoginUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val isLoading: Boolean = false,
    val error: String? = null,
    val info: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    val authState: LiveData<AuthState> = repository.authState().asLiveData()

    private val _uiState = MutableLiveData(LoginUiState())
    val uiState: LiveData<LoginUiState> = _uiState

    fun toggleMode() {
        val current = _uiState.value ?: LoginUiState()
        val nextMode = if (current.mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN
        _uiState.value = current.copy(mode = nextMode, error = null, info = null)
    }

    fun submit(email: String, password: String, confirmPassword: String?) {
        if (_uiState.value?.isLoading == true) return
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        if (trimmedEmail.isEmpty()) {
            _uiState.value = (_uiState.value ?: LoginUiState()).copy(error = "请输入邮箱")
            return
        }
        if (trimmedPassword.length < 6) {
            _uiState.value = (_uiState.value ?: LoginUiState()).copy(error = "密码至少 6 位")
            return
        }
        val activeMode = _uiState.value?.mode ?: AuthMode.LOGIN
        if (activeMode == AuthMode.REGISTER) {
            val confirm = confirmPassword?.trim().orEmpty()
            if (confirm != trimmedPassword) {
                _uiState.value = (_uiState.value ?: LoginUiState()).copy(error = "两次输入的密码不一致")
                return
            }
        }
        _uiState.value = (_uiState.value ?: LoginUiState()).copy(isLoading = true, error = null, info = null)
        viewModelScope.launch {
            val result = if (activeMode == AuthMode.LOGIN) {
                repository.login(trimmedEmail, trimmedPassword)
            } else {
                repository.register(trimmedEmail, trimmedPassword)
            }
            val nextState = when (result) {
                is AuthResult.Error -> (_uiState.value ?: LoginUiState()).copy(
                    isLoading = false,
                    error = result.message,
                    info = null
                )
                AuthResult.Success -> (_uiState.value ?: LoginUiState()).copy(
                    isLoading = false,
                    error = null,
                    info = if (activeMode == AuthMode.LOGIN) "登录成功" else "注册成功，已自动登录"
                )
            }
            _uiState.postValue(nextState)
        }
    }
}
