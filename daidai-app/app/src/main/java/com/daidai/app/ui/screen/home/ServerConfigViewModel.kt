package com.daidai.app.ui.screen.home

import androidx.lifecycle.ViewModel
import com.daidai.app.data.local.ServerConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class ServerConfigUiState(
    val serverUrl: String = "",
    val serverHistory: List<String> = emptyList(),
    val presetServers: List<String> = emptyList()
)

@HiltViewModel
class ServerConfigViewModel @Inject constructor(
    private val serverConfig: ServerConfig
) : ViewModel() {
    private val _uiState = MutableStateFlow(ServerConfigUiState())
    val uiState: StateFlow<ServerConfigUiState> = _uiState.asStateFlow()

    init {
        loadServerConfig()
    }

    fun loadServerConfig() {
        _uiState.value = _uiState.value.copy(
            serverUrl = serverConfig.serverUrl,
            serverHistory = serverConfig.getServerHistoryList(),
            presetServers = serverConfig.getPresetServers()
        )
    }

    fun updateServerUrl(url: String) {
        val trimmedUrl = url.trimEnd('/')
        serverConfig.serverUrl = trimmedUrl
        serverConfig.addServerToHistory(trimmedUrl)
        loadServerConfig()
    }

    fun removeServerFromHistory(url: String) {
        serverConfig.removeServerFromHistory(url)
        loadServerConfig()
    }

    fun clearServerHistory() {
        serverConfig.clearServerHistory()
        loadServerConfig()
    }
}
