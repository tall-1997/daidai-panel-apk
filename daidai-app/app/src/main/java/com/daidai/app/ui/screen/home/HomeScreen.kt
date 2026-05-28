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
fun EnvironmentsContent() {
    // TODO: 实现环境变量列表
    Box(modifier = Modifier.fillMaxSize()) {
        Text("环境变量", modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun ScriptsContent() {
    // TODO: 实现脚本管理
    Box(modifier = Modifier.fillMaxSize()) {
        Text("脚本管理", modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun LogsContent() {
    // TODO: 实现日志查看
    Box(modifier = Modifier.fillMaxSize()) {
        Text("执行日志", modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun SettingsContent() {
    // TODO: 实现设置页面
    Box(modifier = Modifier.fillMaxSize()) {
        Text("系统设置", modifier = Modifier.padding(16.dp))
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
