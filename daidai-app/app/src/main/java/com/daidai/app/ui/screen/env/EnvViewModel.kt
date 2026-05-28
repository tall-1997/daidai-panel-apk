package com.daidai.app.ui.screen.env

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daidai.app.data.remote.model.Env
import com.daidai.app.data.repository.EnvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EnvListUiState(
    val isLoading: Boolean = false,
    val envs: List<Env> = emptyList(),
    val error: String? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = true
)

@HiltViewModel
class EnvViewModel @Inject constructor(
    private val envRepository: EnvRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(EnvListUiState())
    val uiState: StateFlow<EnvListUiState> = _uiState.asStateFlow()

    init {
        loadEnvs()
    }

    fun loadEnvs(refresh: Boolean = false) {
        if (refresh) {
            _uiState.value = _uiState.value.copy(currentPage = 1)
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            envRepository.getEnvs(page = _uiState.value.currentPage)
                .onSuccess { envListResponse ->
                    val newEnvs = if (refresh) {
                        envListResponse.data ?: emptyList()
                    } else {
                        _uiState.value.envs + (envListResponse.data ?: emptyList())
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        envs = newEnvs,
                        hasMore = newEnvs.size < envListResponse.total
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

    fun loadMore() {
        if (_uiState.value.isLoading || !_uiState.value.hasMore) return
        
        _uiState.value = _uiState.value.copy(currentPage = _uiState.value.currentPage + 1)
        loadEnvs()
    }

    fun deleteEnv(envId: Int) {
        viewModelScope.launch {
            envRepository.deleteEnv(envId)
                .onSuccess {
                    loadEnvs(refresh = true)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(error = exception.message)
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
