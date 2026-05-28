package com.daidai.app.ui.screen.home;

import androidx.lifecycle.ViewModel;
import com.daidai.app.data.remote.model.Task;
import com.daidai.app.data.repository.TaskRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0010\u000b\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\f\u001a\u00020\rJ\u000e\u0010\u000e\u001a\u00020\r2\u0006\u0010\u000f\u001a\u00020\u0010J\u000e\u0010\u0011\u001a\u00020\r2\u0006\u0010\u000f\u001a\u00020\u0010J\u000e\u0010\u0012\u001a\u00020\r2\u0006\u0010\u000f\u001a\u00020\u0010J\u0006\u0010\u0013\u001a\u00020\rJ\u0010\u0010\u0014\u001a\u00020\r2\b\b\u0002\u0010\u0015\u001a\u00020\u0016J\u000e\u0010\u0017\u001a\u00020\r2\u0006\u0010\u000f\u001a\u00020\u0010J\u000e\u0010\u0018\u001a\u00020\r2\u0006\u0010\u000f\u001a\u00020\u0010R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00070\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000b\u00a8\u0006\u0019"}, d2 = {"Lcom/daidai/app/ui/screen/home/TaskViewModel;", "Landroidx/lifecycle/ViewModel;", "taskRepository", "Lcom/daidai/app/data/repository/TaskRepository;", "(Lcom/daidai/app/data/repository/TaskRepository;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/daidai/app/ui/screen/home/TaskListUiState;", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "clearError", "", "deleteTask", "taskId", "", "disableTask", "enableTask", "loadMore", "loadTasks", "refresh", "", "runTask", "stopTask", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel
public final class TaskViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull
    private final com.daidai.app.data.repository.TaskRepository taskRepository = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.MutableStateFlow<com.daidai.app.ui.screen.home.TaskListUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.StateFlow<com.daidai.app.ui.screen.home.TaskListUiState> uiState = null;
    
    @javax.inject.Inject
    public TaskViewModel(@org.jetbrains.annotations.NotNull
    com.daidai.app.data.repository.TaskRepository taskRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<com.daidai.app.ui.screen.home.TaskListUiState> getUiState() {
        return null;
    }
    
    public final void loadTasks(boolean refresh) {
    }
    
    public final void loadMore() {
    }
    
    public final void runTask(int taskId) {
    }
    
    public final void stopTask(int taskId) {
    }
    
    public final void enableTask(int taskId) {
    }
    
    public final void disableTask(int taskId) {
    }
    
    public final void deleteTask(int taskId) {
    }
    
    public final void clearError() {
    }
}