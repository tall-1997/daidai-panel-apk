package com.daidai.app.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daidai.app.data.remote.model.Task
import com.daidai.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskListUiState(
    val isLoading: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val error: String? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = true
)

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    fun loadTasks(refresh: Boolean = false) {
        if (refresh) {
            _uiState.value = _uiState.value.copy(currentPage = 1)
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            taskRepository.getTasks(page = _uiState.value.currentPage)
                .onSuccess { taskListData ->
                    val newTasks = if (refresh) {
                        taskListData.items
                    } else {
                        _uiState.value.tasks + taskListData.items
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        tasks = newTasks,
                        hasMore = newTasks.size < taskListData.total
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
        loadTasks()
    }

    fun runTask(taskId: Int) {
        viewModelScope.launch {
            taskRepository.runTask(taskId)
                .onSuccess {
                    // 刷新任务列表
                    loadTasks(refresh = true)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(error = exception.message)
                }
        }
    }

    fun stopTask(taskId: Int) {
        viewModelScope.launch {
            taskRepository.stopTask(taskId)
                .onSuccess {
                    // 刷新任务列表
                    loadTasks(refresh = true)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(error = exception.message)
                }
        }
    }

    fun enableTask(taskId: Int) {
        viewModelScope.launch {
            taskRepository.enableTask(taskId)
                .onSuccess {
                    // 刷新任务列表
                    loadTasks(refresh = true)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(error = exception.message)
                }
        }
    }

    fun disableTask(taskId: Int) {
        viewModelScope.launch {
            taskRepository.disableTask(taskId)
                .onSuccess {
                    // 刷新任务列表
                    loadTasks(refresh = true)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(error = exception.message)
                }
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
                .onSuccess {
                    // 刷新任务列表
                    loadTasks(refresh = true)
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
