package com.daidai.app.ui.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daidai.app.data.local.ServerConfig
import com.daidai.app.data.remote.TokenManager
import com.daidai.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    private val serverConfig: ServerConfig
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val serverUrl: String
        get() = serverConfig.serverUrl
    
    val savedUsername: String
        get() = serverConfig.username
    
    val savedPassword: String
        get() = serverConfig.password
    
    val savedRememberMe: Boolean
        get() = serverConfig.rememberMe

    fun updateServerUrl(url: String) {
        serverConfig.serverUrl = url.trimEnd('/')
    }

    fun addServerToHistory(url: String) {
        serverConfig.addServerToHistory(url)
    }

    fun removeServerFromHistory(url: String) {
        serverConfig.removeServerFromHistory(url)
    }

    fun clearServerHistory() {
        serverConfig.clearServerHistory()
    }

    fun getPresetServers(): List<String> {
        return serverConfig.getPresetServers()
    }

    fun getServerHistoryList(): List<String> {
        return serverConfig.getServerHistoryList()
    }

    fun login(username: String, password: String, rememberMe: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // 保存登录信息
            serverConfig.saveLoginInfo(username, password, rememberMe)
            
            authRepository.login(username, password)
                .onSuccess { loginResponse ->
                    tokenManager.saveTokens(loginResponse.accessToken ?: "", loginResponse.refreshToken ?: "")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
