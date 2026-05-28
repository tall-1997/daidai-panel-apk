package com.daidai.app.ui.screen.system;

import androidx.lifecycle.ViewModel;
import com.daidai.app.data.remote.model.*;
import com.daidai.app.data.repository.SystemRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0010\b\n\u0002\b\u0004\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\f\u001a\u00020\rJ\u0006\u0010\u000e\u001a\u00020\rJ\u0006\u0010\u000f\u001a\u00020\rJ\u0010\u0010\u0010\u001a\u00020\r2\b\b\u0002\u0010\u0011\u001a\u00020\u0012J\u0006\u0010\u0013\u001a\u00020\rJ\u0006\u0010\u0014\u001a\u00020\rJ\u0006\u0010\u0015\u001a\u00020\rR\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00070\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000b\u00a8\u0006\u0016"}, d2 = {"Lcom/daidai/app/ui/screen/system/SystemViewModel;", "Landroidx/lifecycle/ViewModel;", "systemRepository", "Lcom/daidai/app/data/repository/SystemRepository;", "(Lcom/daidai/app/data/repository/SystemRepository;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/daidai/app/ui/screen/system/SystemUiState;", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "clearError", "", "loadDashboard", "loadHealthCheck", "loadPanelLog", "lines", "", "loadStats", "loadSystemInfo", "runHealthCheck", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel
public final class SystemViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull
    private final com.daidai.app.data.repository.SystemRepository systemRepository = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.MutableStateFlow<com.daidai.app.ui.screen.system.SystemUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.StateFlow<com.daidai.app.ui.screen.system.SystemUiState> uiState = null;
    
    @javax.inject.Inject
    public SystemViewModel(@org.jetbrains.annotations.NotNull
    com.daidai.app.data.repository.SystemRepository systemRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<com.daidai.app.ui.screen.system.SystemUiState> getUiState() {
        return null;
    }
    
    public final void loadSystemInfo() {
    }
    
    public final void loadHealthCheck() {
    }
    
    public final void runHealthCheck() {
    }
    
    public final void loadDashboard() {
    }
    
    public final void loadStats() {
    }
    
    public final void loadPanelLog(int lines) {
    }
    
    public final void clearError() {
    }
}