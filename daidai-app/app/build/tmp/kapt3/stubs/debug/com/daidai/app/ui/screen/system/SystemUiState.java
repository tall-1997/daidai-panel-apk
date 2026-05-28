package com.daidai.app.ui.screen.system;

import androidx.lifecycle.ViewModel;
import com.daidai.app.data.remote.model.*;
import com.daidai.app.data.repository.SystemRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u001e\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001Bw\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u0012\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u0007\u0012\u000e\b\u0002\u0010\b\u001a\b\u0012\u0004\u0012\u00020\n0\t\u0012\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\u0007\u0012\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\r\u0012\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u000f\u0012\u000e\b\u0002\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00070\t\u0012\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\u0002\u0010\u0012J\t\u0010!\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010\"\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003J\u000b\u0010#\u001a\u0004\u0018\u00010\u0007H\u00c6\u0003J\u000f\u0010$\u001a\b\u0012\u0004\u0012\u00020\n0\tH\u00c6\u0003J\u000b\u0010%\u001a\u0004\u0018\u00010\u0007H\u00c6\u0003J\u000b\u0010&\u001a\u0004\u0018\u00010\rH\u00c6\u0003J\u000b\u0010\'\u001a\u0004\u0018\u00010\u000fH\u00c6\u0003J\u000f\u0010(\u001a\b\u0012\u0004\u0012\u00020\u00070\tH\u00c6\u0003J\u000b\u0010)\u001a\u0004\u0018\u00010\u0007H\u00c6\u0003J{\u0010*\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u00052\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u00072\u000e\b\u0002\u0010\b\u001a\b\u0012\u0004\u0012\u00020\n0\t2\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\u00072\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\r2\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u000f2\u000e\b\u0002\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00070\t2\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u0007H\u00c6\u0001J\u0013\u0010+\u001a\u00020\u00032\b\u0010,\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010-\u001a\u00020.H\u00d6\u0001J\t\u0010/\u001a\u00020\u0007H\u00d6\u0001R\u0013\u0010\f\u001a\u0004\u0018\u00010\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014R\u0013\u0010\u0011\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u0017\u0010\b\u001a\b\u0012\u0004\u0012\u00020\n0\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018R\u0013\u0010\u0006\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0016R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0002\u0010\u001aR\u0013\u0010\u000b\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u0016R\u0017\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00070\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u0018R\u0013\u0010\u000e\u001a\u0004\u0018\u00010\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001eR\u0013\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010 \u00a8\u00060"}, d2 = {"Lcom/daidai/app/ui/screen/system/SystemUiState;", "", "isLoading", "", "systemInfo", "Lcom/daidai/app/data/remote/model/SystemInfo;", "healthStatus", "", "healthCheckItems", "", "Lcom/daidai/app/data/remote/model/HealthCheckItem;", "lastHealthCheckAt", "dashboardData", "Lcom/daidai/app/data/remote/model/DashboardData;", "statsData", "Lcom/daidai/app/data/remote/model/StatsData;", "panelLogs", "error", "(ZLcom/daidai/app/data/remote/model/SystemInfo;Ljava/lang/String;Ljava/util/List;Ljava/lang/String;Lcom/daidai/app/data/remote/model/DashboardData;Lcom/daidai/app/data/remote/model/StatsData;Ljava/util/List;Ljava/lang/String;)V", "getDashboardData", "()Lcom/daidai/app/data/remote/model/DashboardData;", "getError", "()Ljava/lang/String;", "getHealthCheckItems", "()Ljava/util/List;", "getHealthStatus", "()Z", "getLastHealthCheckAt", "getPanelLogs", "getStatsData", "()Lcom/daidai/app/data/remote/model/StatsData;", "getSystemInfo", "()Lcom/daidai/app/data/remote/model/SystemInfo;", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "other", "hashCode", "", "toString", "app_debug"})
public final class SystemUiState {
    private final boolean isLoading = false;
    @org.jetbrains.annotations.Nullable
    private final com.daidai.app.data.remote.model.SystemInfo systemInfo = null;
    @org.jetbrains.annotations.Nullable
    private final java.lang.String healthStatus = null;
    @org.jetbrains.annotations.NotNull
    private final java.util.List<com.daidai.app.data.remote.model.HealthCheckItem> healthCheckItems = null;
    @org.jetbrains.annotations.Nullable
    private final java.lang.String lastHealthCheckAt = null;
    @org.jetbrains.annotations.Nullable
    private final com.daidai.app.data.remote.model.DashboardData dashboardData = null;
    @org.jetbrains.annotations.Nullable
    private final com.daidai.app.data.remote.model.StatsData statsData = null;
    @org.jetbrains.annotations.NotNull
    private final java.util.List<java.lang.String> panelLogs = null;
    @org.jetbrains.annotations.Nullable
    private final java.lang.String error = null;
    
    public SystemUiState(boolean isLoading, @org.jetbrains.annotations.Nullable
    com.daidai.app.data.remote.model.SystemInfo systemInfo, @org.jetbrains.annotations.Nullable
    java.lang.String healthStatus, @org.jetbrains.annotations.NotNull
    java.util.List<com.daidai.app.data.remote.model.HealthCheckItem> healthCheckItems, @org.jetbrains.annotations.Nullable
    java.lang.String lastHealthCheckAt, @org.jetbrains.annotations.Nullable
    com.daidai.app.data.remote.model.DashboardData dashboardData, @org.jetbrains.annotations.Nullable
    com.daidai.app.data.remote.model.StatsData statsData, @org.jetbrains.annotations.NotNull
    java.util.List<java.lang.String> panelLogs, @org.jetbrains.annotations.Nullable
    java.lang.String error) {
        super();
    }
    
    public final boolean isLoading() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable
    public final com.daidai.app.data.remote.model.SystemInfo getSystemInfo() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.String getHealthStatus() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.util.List<com.daidai.app.data.remote.model.HealthCheckItem> getHealthCheckItems() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.String getLastHealthCheckAt() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final com.daidai.app.data.remote.model.DashboardData getDashboardData() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final com.daidai.app.data.remote.model.StatsData getStatsData() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.util.List<java.lang.String> getPanelLogs() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.String getError() {
        return null;
    }
    
    public SystemUiState() {
        super();
    }
    
    public final boolean component1() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable
    public final com.daidai.app.data.remote.model.SystemInfo component2() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.String component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.util.List<com.daidai.app.data.remote.model.HealthCheckItem> component4() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.String component5() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final com.daidai.app.data.remote.model.DashboardData component6() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final com.daidai.app.data.remote.model.StatsData component7() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.util.List<java.lang.String> component8() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.String component9() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final com.daidai.app.ui.screen.system.SystemUiState copy(boolean isLoading, @org.jetbrains.annotations.Nullable
    com.daidai.app.data.remote.model.SystemInfo systemInfo, @org.jetbrains.annotations.Nullable
    java.lang.String healthStatus, @org.jetbrains.annotations.NotNull
    java.util.List<com.daidai.app.data.remote.model.HealthCheckItem> healthCheckItems, @org.jetbrains.annotations.Nullable
    java.lang.String lastHealthCheckAt, @org.jetbrains.annotations.Nullable
    com.daidai.app.data.remote.model.DashboardData dashboardData, @org.jetbrains.annotations.Nullable
    com.daidai.app.data.remote.model.StatsData statsData, @org.jetbrains.annotations.NotNull
    java.util.List<java.lang.String> panelLogs, @org.jetbrains.annotations.Nullable
    java.lang.String error) {
        return null;
    }
    
    @java.lang.Override
    public boolean equals(@org.jetbrains.annotations.Nullable
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override
    @org.jetbrains.annotations.NotNull
    public java.lang.String toString() {
        return null;
    }
}