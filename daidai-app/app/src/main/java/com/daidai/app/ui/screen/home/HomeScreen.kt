package com.daidai.app.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daidai.app.data.remote.model.Task
import com.daidai.app.ui.screen.env.EnvViewModel
import com.daidai.app.ui.screen.log.LogViewModel
import com.daidai.app.ui.screen.script.ScriptViewModel

sealed class HomeTab(val title: String, val icon: ImageVector) {
    object Tasks : HomeTab("任务", Icons.Default.List)
    object Environments : HomeTab("环境变量", Icons.Default.Settings)
    object Scripts : HomeTab("脚本", Icons.Default.Code)
    object Logs : HomeTab("日志", Icons.Default.Article)
    object Settings : HomeTab("设置", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToWebHelper: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf<HomeTab>(HomeTab.Tasks) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("呆呆面板") },
                actions = {
                    IconButton(onClick = { /* TODO: 搜索 */ }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = { /* TODO: 刷新 */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = onNavigateToWebHelper) {
                        Icon(Icons.Default.Web, contentDescription = "Web助手")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val tabs = listOf(
                    HomeTab.Tasks,
                    HomeTab.Environments,
                    HomeTab.Scripts,
                    HomeTab.Logs,
                    HomeTab.Settings
                )
                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                is HomeTab.Tasks -> TasksContent()
                is HomeTab.Environments -> EnvironmentsContent()
                is HomeTab.Scripts -> ScriptsContent()
                is HomeTab.Logs -> LogsContent()
                is HomeTab.Settings -> SettingsContent()
            }
        }
    }
}

@Composable
fun TasksContent(
    viewModel: TaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading && uiState.tasks.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (uiState.error != null && uiState.tasks.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.error ?: "未知错误",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.loadTasks(refresh = true) }) {
                    Text("重试")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.tasks) { task ->
                    TaskItem(
                        task = task,
                        onRun = { viewModel.runTask(task.id) },
                        onStop = { viewModel.stopTask(task.id) },
                        onEnable = { viewModel.enableTask(task.id) },
                        onDisable = { viewModel.disableTask(task.id) },
                        onDelete = { viewModel.deleteTask(task.id) }
                    )
                }
                
                if (uiState.hasMore) {
                    item {
                        LaunchedEffect(Unit) {
                            viewModel.loadMore()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EnvironmentsContent(
    viewModel: EnvViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading && uiState.envs.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (uiState.error != null && uiState.envs.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.error ?: "未知错误",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.loadEnvs(refresh = true) }) {
                    Text("重试")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.envs) { env ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = env.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = env.isEnabled,
                                    onCheckedChange = { /* TODO: 切换启用状态 */ }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = env.value,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            env.remark?.let { remark ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = remark,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                if (uiState.hasMore) {
                    item {
                        LaunchedEffect(Unit) {
                            viewModel.loadMore()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScriptsContent(
    viewModel: ScriptViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading && uiState.scripts.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (uiState.error != null && uiState.scripts.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.error ?: "未知错误",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.loadScripts() }) {
                    Text("重试")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.scripts) { script ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (script.isDir) Icons.Default.Folder else Icons.Default.Code,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = if (script.isDir) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = script.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = script.path,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!script.isDir) {
                                Text(
                                    text = formatFileSize(script.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

@Composable
fun LogsContent(
    viewModel: LogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading && uiState.logs.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (uiState.error != null && uiState.logs.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.error ?: "未知错误",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.loadLogs(refresh = true) }) {
                    Text("重试")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.logs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = log.taskName,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (log.status == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                ) {
                                    Text(
                                        text = if (log.status == 0) "成功" else "失败",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "开始时间: ${log.startedAt}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            log.finishedAt?.let { finishedAt ->
                                Text(
                                    text = "结束时间: $finishedAt",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            log.duration?.let { duration ->
                                Text(
                                    text = "耗时: ${duration}ms",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            log.output?.let { output ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = output,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                if (uiState.hasMore) {
                    item {
                        LaunchedEffect(Unit) {
                            viewModel.loadMore()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "系统设置",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                SettingItem(
                    icon = Icons.Default.Info,
                    title = "系统信息",
                    subtitle = "查看系统版本和状态"
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    icon = Icons.Default.Update,
                    title = "系统更新",
                    subtitle = "检查并更新系统"
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    icon = Icons.Default.Backup,
                    title = "备份恢复",
                    subtitle = "备份和恢复系统数据"
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    icon = Icons.Default.Security,
                    title = "安全设置",
                    subtitle = "密码修改和安全配置"
                )
            }
        }
    }
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TaskItem(
    task: Task,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (task.status) {
                            Task.STATUS_RUNNING -> MaterialTheme.colorScheme.primary
                            Task.STATUS_ENABLED -> MaterialTheme.colorScheme.tertiary
                            Task.STATUS_QUEUED -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                
                Switch(
                    checked = task.isEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) onEnable() else onDisable()
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = task.command,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "调度: ${task.schedule}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (task.isRunning) {
                    IconButton(onClick = onStop) {
                        Icon(Icons.Default.Stop, contentDescription = "停止")
                    }
                } else {
                    IconButton(onClick = onRun) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "执行")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        }
    }
}
