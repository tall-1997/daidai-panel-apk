package com.daidai.app.ui.screen.system

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daidai.app.data.remote.model.*
import com.daidai.app.data.repository.SystemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SystemUiState(
    val isLoading: Boolean = false,
    val systemInfo: SystemInfo? = null,
    val healthStatus: String? = null,
    val healthCheckItems: List<HealthCheckItem> = emptyList(),
    val lastHealthCheckAt: String? = null,
    val dashboardData: DashboardData? = null,
    val statsData: StatsData? = null,
    val panelLogs: List<String> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class SystemViewModel @Inject constructor(
    private val systemRepository: SystemRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SystemUiState())
    val uiState: StateFlow<SystemUiState> = _uiState.asStateFlow()

    init {
        loadSystemInfo()
        loadHealthCheck()
        loadDashboard()
        loadStats()
    }

    fun loadSystemInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            systemRepository.getSystemInfo()
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        systemInfo = response.data
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

    fun loadHealthCheck() {
        viewModelScope.launch {
            systemRepository.getHealthCheck()
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        healthCheckItems = response.items ?: emptyList(),
                        lastHealthCheckAt = response.lastCheckedAt
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(error = exception.message)
                }
        }
    }

    fun runHealthCheck() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            systemRepository.runHealthCheck()
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        healthCheckItems = response.items ?: emptyList(),
                        lastHealthCheckAt = response.lastCheckedAt
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

    fun loadDashboard() {
        viewModelScope.launch {
            systemRepository.getDashboard()
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        dashboardData = response.data
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(error = exception.message)
                }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            systemRepository.getStats()
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        statsData = response.data
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(error = exception.message)
                }
        }
    }

    fun loadPanelLog(lines: Int = 100) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            systemRepository.getPanelLog(lines)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        panelLogs = response.data?.logs ?: emptyList()
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
