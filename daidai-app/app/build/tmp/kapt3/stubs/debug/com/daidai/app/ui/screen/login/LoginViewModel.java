package com.daidai.app.ui.screen.login;

import androidx.lifecycle.ViewModel;
import com.daidai.app.data.local.ServerConfig;
import com.daidai.app.data.remote.TokenManager;
import com.daidai.app.data.repository.AuthRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0010 \n\u0002\b\b\b\u0007\u0018\u00002\u00020\u0001B\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u000e\u0010\u001c\u001a\u00020\u001d2\u0006\u0010\u001e\u001a\u00020\rJ\u0006\u0010\u001f\u001a\u00020\u001dJ\u0006\u0010 \u001a\u00020\u001dJ\f\u0010!\u001a\b\u0012\u0004\u0012\u00020\r0\"J\f\u0010#\u001a\b\u0012\u0004\u0012\u00020\r0\"J \u0010$\u001a\u00020\u001d2\u0006\u0010%\u001a\u00020\r2\u0006\u0010&\u001a\u00020\r2\b\b\u0002\u0010\'\u001a\u00020\u0011J\u000e\u0010(\u001a\u00020\u001d2\u0006\u0010\u001e\u001a\u00020\rJ\u000e\u0010)\u001a\u00020\u001d2\u0006\u0010\u001e\u001a\u00020\rR\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0011\u0010\f\u001a\u00020\r8F\u00a2\u0006\u0006\u001a\u0004\b\u000e\u0010\u000fR\u0011\u0010\u0010\u001a\u00020\u00118F\u00a2\u0006\u0006\u001a\u0004\b\u0012\u0010\u0013R\u0011\u0010\u0014\u001a\u00020\r8F\u00a2\u0006\u0006\u001a\u0004\b\u0015\u0010\u000fR\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0011\u0010\u0016\u001a\u00020\r8F\u00a2\u0006\u0006\u001a\u0004\b\u0017\u0010\u000fR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0019\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u001b\u00a8\u0006*"}, d2 = {"Lcom/daidai/app/ui/screen/login/LoginViewModel;", "Landroidx/lifecycle/ViewModel;", "authRepository", "Lcom/daidai/app/data/repository/AuthRepository;", "tokenManager", "Lcom/daidai/app/data/remote/TokenManager;", "serverConfig", "Lcom/daidai/app/data/local/ServerConfig;", "(Lcom/daidai/app/data/repository/AuthRepository;Lcom/daidai/app/data/remote/TokenManager;Lcom/daidai/app/data/local/ServerConfig;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/daidai/app/ui/screen/login/LoginUiState;", "savedPassword", "", "getSavedPassword", "()Ljava/lang/String;", "savedRememberMe", "", "getSavedRememberMe", "()Z", "savedUsername", "getSavedUsername", "serverUrl", "getServerUrl", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "addServerToHistory", "", "url", "clearError", "clearServerHistory", "getPresetServers", "", "getServerHistoryList", "login", "username", "password", "rememberMe", "removeServerFromHistory", "updateServerUrl", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel
public final class LoginViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull
    private final com.daidai.app.data.repository.AuthRepository authRepository = null;
    @org.jetbrains.annotations.NotNull
    private final com.daidai.app.data.remote.TokenManager tokenManager = null;
    @org.jetbrains.annotations.NotNull
    private final com.daidai.app.data.local.ServerConfig serverConfig = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.MutableStateFlow<com.daidai.app.ui.screen.login.LoginUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.StateFlow<com.daidai.app.ui.screen.login.LoginUiState> uiState = null;
    
    @javax.inject.Inject
    public LoginViewModel(@org.jetbrains.annotations.NotNull
    com.daidai.app.data.repository.AuthRepository authRepository, @org.jetbrains.annotations.NotNull
    com.daidai.app.data.remote.TokenManager tokenManager, @org.jetbrains.annotations.NotNull
    com.daidai.app.data.local.ServerConfig serverConfig) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<com.daidai.app.ui.screen.login.LoginUiState> getUiState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getServerUrl() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getSavedUsername() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getSavedPassword() {
        return null;
    }
    
    public final boolean getSavedRememberMe() {
        return false;
    }
    
    public final void updateServerUrl(@org.jetbrains.annotations.NotNull
    java.lang.String url) {
    }
    
    public final void addServerToHistory(@org.jetbrains.annotations.NotNull
    java.lang.String url) {
    }
    
    public final void removeServerFromHistory(@org.jetbrains.annotations.NotNull
    java.lang.String url) {
    }
    
    public final void clearServerHistory() {
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.util.List<java.lang.String> getPresetServers() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.util.List<java.lang.String> getServerHistoryList() {
        return null;
    }
    
    public final void login(@org.jetbrains.annotations.NotNull
    java.lang.String username, @org.jetbrains.annotations.NotNull
    java.lang.String password, boolean rememberMe) {
    }
    
    public final void clearError() {
    }
}