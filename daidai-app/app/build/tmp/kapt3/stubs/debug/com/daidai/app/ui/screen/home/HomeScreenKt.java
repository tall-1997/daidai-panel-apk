package com.daidai.app.ui.screen.home;

import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material.icons.filled.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.vector.ImageVector;
import com.daidai.app.data.remote.model.Task;
import com.daidai.app.ui.screen.env.EnvViewModel;
import com.daidai.app.ui.screen.log.LogViewModel;
import com.daidai.app.ui.screen.script.ScriptViewModel;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000H\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\u001a\u0012\u0010\u0000\u001a\u00020\u00012\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u0007\u001a\u0018\u0010\u0004\u001a\u00020\u00012\u000e\b\u0002\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00010\u0006H\u0007\u001a\u0012\u0010\u0007\u001a\u00020\u00012\b\b\u0002\u0010\u0002\u001a\u00020\bH\u0007\u001a\u0012\u0010\t\u001a\u00020\u00012\b\b\u0002\u0010\u0002\u001a\u00020\nH\u0007\u001a \u0010\u000b\u001a\u00020\u00012\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u000fH\u0007\u001a\b\u0010\u0011\u001a\u00020\u0001H\u0007\u001aV\u0010\u0012\u001a\u00020\u00012\u0006\u0010\u0013\u001a\u00020\u00142\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00010\u00062\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00010\u00062\f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00010\u00062\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00010\u00062\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00010\u0006H\u0007\u001a\u0012\u0010\u001a\u001a\u00020\u00012\b\b\u0002\u0010\u0002\u001a\u00020\u001bH\u0007\u001a\u000e\u0010\u001c\u001a\u00020\u000f2\u0006\u0010\u001d\u001a\u00020\u001e\u00a8\u0006\u001f"}, d2 = {"EnvironmentsContent", "", "viewModel", "Lcom/daidai/app/ui/screen/env/EnvViewModel;", "HomeScreen", "onNavigateToWebHelper", "Lkotlin/Function0;", "LogsContent", "Lcom/daidai/app/ui/screen/log/LogViewModel;", "ScriptsContent", "Lcom/daidai/app/ui/screen/script/ScriptViewModel;", "SettingItem", "icon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "title", "", "subtitle", "SettingsContent", "TaskItem", "task", "Lcom/daidai/app/data/remote/model/Task;", "onRun", "onStop", "onEnable", "onDisable", "onDelete", "TasksContent", "Lcom/daidai/app/ui/screen/home/TaskViewModel;", "formatFileSize", "bytes", "", "app_debug"})
public final class HomeScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable
    public static final void HomeScreen(@org.jetbrains.annotations.NotNull
    kotlin.jvm.functions.Function0<kotlin.Unit> onNavigateToWebHelper) {
    }
    
    @androidx.compose.runtime.Composable
    public static final void TasksContent(@org.jetbrains.annotations.NotNull
    com.daidai.app.ui.screen.home.TaskViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable
    public static final void EnvironmentsContent(@org.jetbrains.annotations.NotNull
    com.daidai.app.ui.screen.env.EnvViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable
    public static final void ScriptsContent(@org.jetbrains.annotations.NotNull
    com.daidai.app.ui.screen.script.ScriptViewModel viewModel) {
    }
    
    @org.jetbrains.annotations.NotNull
    public static final java.lang.String formatFileSize(long bytes) {
        return null;
    }
    
    @androidx.compose.runtime.Composable
    public static final void LogsContent(@org.jetbrains.annotations.NotNull
    com.daidai.app.ui.screen.log.LogViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable
    public static final void SettingsContent() {
    }
    
    @androidx.compose.runtime.Composable
    public static final void SettingItem(@org.jetbrains.annotations.NotNull
    androidx.compose.ui.graphics.vector.ImageVector icon, @org.jetbrains.annotations.NotNull
    java.lang.String title, @org.jetbrains.annotations.NotNull
    java.lang.String subtitle) {
    }
    
    @androidx.compose.runtime.Composable
    public static final void TaskItem(@org.jetbrains.annotations.NotNull
    com.daidai.app.data.remote.model.Task task, @org.jetbrains.annotations.NotNull
    kotlin.jvm.functions.Function0<kotlin.Unit> onRun, @org.jetbrains.annotations.NotNull
    kotlin.jvm.functions.Function0<kotlin.Unit> onStop, @org.jetbrains.annotations.NotNull
    kotlin.jvm.functions.Function0<kotlin.Unit> onEnable, @org.jetbrains.annotations.NotNull
    kotlin.jvm.functions.Function0<kotlin.Unit> onDisable, @org.jetbrains.annotations.NotNull
    kotlin.jvm.functions.Function0<kotlin.Unit> onDelete) {
    }
}