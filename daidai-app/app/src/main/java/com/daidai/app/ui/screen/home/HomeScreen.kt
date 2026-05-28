package com.daidai.app.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daidai.app.data.remote.model.Task
import com.daidai.app.ui.screen.dependency.DependencyViewModel
import com.daidai.app.ui.screen.env.EnvViewModel
import com.daidai.app.ui.screen.log.LogViewModel
import com.daidai.app.ui.screen.script.ScriptViewModel
import com.daidai.app.ui.screen.system.SystemViewModel
import com.daidai.app.ui.screen.login.ServerAddressDialog

sealed class HomeTab(val title: String, val icon: ImageVector) {
    object Tasks : HomeTab("任务", Icons.Default.List)
    object Environments : HomeTab("环境变量", Icons.Default.Settings)
    object Dependencies : HomeTab("依赖管理", Icons.Default.Extension)
    object Logs : HomeTab("日志", Icons.Default.Article)
    object Settings : HomeTab("设置", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToWebHelper: () -> Unit = {},
    onLogout: () -> Unit = {}
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
                    HomeTab.Dependencies,
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
                is HomeTab.Dependencies -> DependenciesContent()
                is HomeTab.Logs -> LogsContent()
                is HomeTab.Settings -> SettingsContent(onLogout = onLogout)
            }
        }
    }
}

@Composable
fun TasksContent(
    viewModel: TaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    
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
                        onDelete = { viewModel.deleteTask(task.id) },
                        onGetLogs = { viewModel.getTaskLogs(it) },
                        taskLogs = uiState.taskLogs[task.id] ?: emptyList()
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
        
        // 添加任务按钮
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加任务")
        }
    }
    
    // 创建任务对话框
    if (showCreateDialog) {
        CreateTaskDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, command, schedule ->
                viewModel.createTask(name, command, schedule)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit,
    onUploadScript: ((String, String) -> Unit)? = null
) {
    var name by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf("") }
    var showScriptSelector by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建任务") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("任务名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text("执行命令") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showScriptSelector = true }
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("上传脚本")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = schedule,
                    onValueChange = { schedule = it },
                    label = { Text("调度表达式 (cron)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("例如: 0 0 * * * (每天0点)") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, command, schedule) },
                enabled = name.isNotBlank() && command.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
    
    // 脚本选择器对话框
    if (showScriptSelector) {
        ScriptSelectorDialog(
            onDismiss = { showScriptSelector = false },
            onSelect = { scriptPath ->
                command = "sh $scriptPath"
                showScriptSelector = false
            }
        )
    }
}

@Composable
fun ScriptSelectorDialog(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var scriptPath by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择脚本") },
        text = {
            Column {
                Text(
                    text = "输入脚本路径或从上传的脚本中选择",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = scriptPath,
                    onValueChange = { scriptPath = it },
                    label = { Text("脚本路径") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("例如: /scripts/my_script.sh") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSelect(scriptPath) },
                enabled = scriptPath.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
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
fun DependenciesContent(
    viewModel: DependencyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 类型选择标签
        ScrollableTabRow(
            selectedTabIndex = when (uiState.selectedType) {
                "nodejs" -> 0
                "python" -> 1
                "linux" -> 2
                else -> 0
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = uiState.selectedType == "nodejs",
                onClick = { viewModel.changeType("nodejs") },
                text = { Text("Node.js") }
            )
            Tab(
                selected = uiState.selectedType == "python",
                onClick = { viewModel.changeType("python") },
                text = { Text("Python") }
            )
            Tab(
                selected = uiState.selectedType == "linux",
                onClick = { viewModel.changeType("linux") },
                text = { Text("Linux") }
            )
        }
        
        // 内容区域
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading && uiState.dependencies.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.error != null && uiState.dependencies.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.error ?: "未知错误",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadDeps() }) {
                        Text("重试")
                    }
                }
            } else if (uiState.dependencies.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Extension,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无${uiState.selectedType}依赖",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.dependencies) { dep ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Extension,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = when (dep.status) {
                                        "installed" -> MaterialTheme.colorScheme.primary
                                        "installing", "queued" -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dep.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = dep.typeText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                AssistChip(
                                    onClick = {},
                                    label = { Text(dep.statusText) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = when (dep.status) {
                                            "installed" -> MaterialTheme.colorScheme.primaryContainer
                                            "installing", "queued" -> MaterialTheme.colorScheme.tertiaryContainer
                                            else -> MaterialTheme.colorScheme.errorContainer
                                        }
                                    )
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.loadLogDetail(log.id) }
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
                                    text = log.taskName ?: "未知任务",
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
                            log.endedAt?.let { endedAt ->
                                Text(
                                    text = "结束时间: $endedAt",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            log.duration?.let { duration ->
                                Text(
                                    text = "耗时: ${String.format("%.2f", duration / 1000)}秒",
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
        
        // 日志详情弹窗
        uiState.selectedLog?.let { logDetail ->
            AlertDialog(
                onDismissRequest = { viewModel.clearSelectedLog() },
                title = { Text("日志详情") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("任务ID: ${logDetail.taskId}")
                        logDetail.taskName?.let { Text("任务名称: $it") }
                        Text("状态: ${if (logDetail.status == 0) "成功" else "失败"}")
                        Text("开始时间: ${logDetail.startedAt}")
                        logDetail.endedAt?.let { Text("结束时间: $it") }
                        logDetail.duration?.let { Text("耗时: ${String.format("%.2f", it / 1000)}秒") }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "日志内容:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (uiState.isLoadingDetail) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = logDetail.content ?: "暂无日志内容",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearSelectedLog() }) {
                        Text("关闭")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    onLogout: () -> Unit = {},
    viewModel: SystemViewModel = hiltViewModel(),
    serverConfigViewModel: ServerConfigViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val serverConfigState by serverConfigViewModel.uiState.collectAsStateWithLifecycle()
    var showSystemInfo by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showHealthCheck by remember { mutableStateOf(false) }
    var showPanelLog by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "系统设置",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 系统状态卡片
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "系统状态",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    // 健康检查状态
                    uiState.healthCheckItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when (item.status) {
                                    "ok" -> Icons.Default.CheckCircle
                                    "warning" -> Icons.Default.Warning
                                    else -> Icons.Default.Error
                                },
                                contentDescription = null,
                                tint = when (item.status) {
                                    "ok" -> MaterialTheme.colorScheme.primary
                                    "warning" -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = item.message ?: item.status,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { viewModel.runHealthCheck() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("刷新健康检查")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 服务器地址卡片
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "服务器连接",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showServerDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "服务器地址",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = serverConfigState.serverUrl,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "修改服务器",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // 服务器地址选择对话框
        if (showServerDialog) {
            ServerAddressDialog(
                currentUrl = serverConfigState.serverUrl,
                presetServers = serverConfigState.presetServers,
                historyServers = serverConfigState.serverHistory,
                onDismiss = { showServerDialog = false },
                onSelect = { url ->
                    serverConfigViewModel.updateServerUrl(url)
                    showServerDialog = false
                },
                onDeleteHistory = { url ->
                    serverConfigViewModel.removeServerFromHistory(url)
                },
                onClearHistory = {
                    serverConfigViewModel.clearServerHistory()
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 统计信息卡片
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "统计信息",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                uiState.statsData?.let { stats ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            title = "任务总数",
                            value = "${stats.tasks?.total ?: 0}",
                            icon = Icons.Default.List
                        )
                        StatItem(
                            title = "已启用",
                            value = "${stats.tasks?.enabled ?: 0}",
                            icon = Icons.Default.CheckCircle
                        )
                        StatItem(
                            title = "运行中",
                            value = "${stats.tasks?.running ?: 0}",
                            icon = Icons.Default.PlayArrow
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            title = "日志总数",
                            value = "${stats.logs?.total ?: 0}",
                            icon = Icons.Default.Article
                        )
                        StatItem(
                            title = "成功率",
                            value = "${String.format("%.1f", stats.logs?.successRate ?: 0.0)}%",
                            icon = Icons.Default.TrendingUp
                        )
                        StatItem(
                            title = "脚本数",
                            value = "${stats.scripts?.total ?: 0}",
                            icon = Icons.Default.Code
                        )
                    }
                } ?: run {
                    Text(
                        text = "加载中...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 设置选项卡片
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                SettingItem(
                    icon = Icons.Default.Info,
                    title = "系统信息",
                    subtitle = "查看系统版本和状态",
                    onClick = { showSystemInfo = true }
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    icon = Icons.Default.HealthAndSafety,
                    title = "健康检查",
                    subtitle = "查看系统健康状态详情",
                    onClick = { showHealthCheck = true }
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    icon = Icons.Default.Article,
                    title = "面板日志",
                    subtitle = "查看系统运行日志",
                    onClick = { 
                        viewModel.loadPanelLog()
                        showPanelLog = true 
                    }
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    icon = Icons.Default.Update,
                    title = "检查更新",
                    subtitle = "检查App最新版本",
                    onClick = { /* TODO: 检查更新 */ }
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    icon = Icons.Default.Backup,
                    title = "数据备份",
                    subtitle = "备份任务和配置数据",
                    onClick = { /* TODO: 数据备份 */ }
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    icon = Icons.Default.Security,
                    title = "安全设置",
                    subtitle = "密码修改和安全配置",
                    onClick = { /* TODO: 安全设置 */ }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                SettingItem(
                    icon = Icons.Default.Help,
                    title = "帮助与反馈",
                    subtitle = "使用帮助和问题反馈",
                    onClick = { /* TODO: 帮助反馈 */ }
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    icon = Icons.Default.Info,
                    title = "关于",
                    subtitle = "App版本和开发者信息",
                    onClick = { showAbout = true }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("退出登录")
        }
    }
    
    // 系统信息弹窗
    if (showSystemInfo) {
        AlertDialog(
            onDismissRequest = { showSystemInfo = false },
            title = { Text("系统信息") },
            text = {
                Column {
                    uiState.systemInfo?.let { info ->
                        Text("版本: ${info.version}")
                        Text("API版本: ${info.apiVersion}")
                        Text("框架: ${info.framework}")
                    } ?: run {
                        Text("App版本: 0.0.1")
                        Text("构建版本: 1")
                        Text("最低支持: Android 8.0")
                        Text("目标版本: Android 14")
                        Text("技术栈: Kotlin + Jetpack Compose")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSystemInfo = false }) {
                    Text("确定")
                }
            }
        )
    }
    
    // 健康检查详情弹窗
    if (showHealthCheck) {
        AlertDialog(
            onDismissRequest = { showHealthCheck = false },
            title = { Text("健康检查详情") },
            text = {
                Column {
                    Text(
                        text = "最后检查时间: ${uiState.lastHealthCheckAt ?: "未检查"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    uiState.healthCheckItems.forEach { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        when (item.status) {
                                            "ok" -> Icons.Default.CheckCircle
                                            "warning" -> Icons.Default.Warning
                                            else -> Icons.Default.Error
                                        },
                                        contentDescription = null,
                                        tint = when (item.status) {
                                            "ok" -> MaterialTheme.colorScheme.primary
                                            "warning" -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.error
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                if (item.message != null) {
                                    Text(
                                        text = item.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHealthCheck = false }) {
                    Text("确定")
                }
            }
        )
    }
    
    // 面板日志弹窗
    if (showPanelLog) {
        AlertDialog(
            onDismissRequest = { showPanelLog = false },
            title = { Text("面板日志") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (uiState.panelLogs.isEmpty()) {
                        Text(
                            text = "暂无日志",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.panelLogs.forEach { log ->
                            Text(
                                text = log,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPanelLog = false }) {
                    Text("关闭")
                }
            }
        )
    }
    
    // 关于弹窗
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("关于") },
            text = {
                Column {
                    Text("呆呆面板 Android App")
                    Text("版本: 0.0.1")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("一个功能强大的定时任务管理平台客户端")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("开源地址:")
                    Text("https://github.com/tall-1997/daidai-panel", 
                        color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text("确定")
                }
            }
        )
    }
}

@Composable
fun StatItem(
    title: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
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
}

@Composable
fun TaskItem(
    task: Task,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onDelete: () -> Unit,
    onGetLogs: ((Int) -> Unit)? = null,
    taskLogs: List<String> = emptyList()
) {
    var expanded by remember { mutableStateOf(false) }
    
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
                text = if (task.schedule.isNullOrBlank()) "调度: 未设置" else "调度: ${task.schedule}",
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
                    IconButton(onClick = {
                        onRun()
                        expanded = true
                        onGetLogs?.invoke(task.id)
                    }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "执行")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "运行日志",
                                style = MaterialTheme.typography.titleSmall
                            )
                            IconButton(
                                onClick = { expanded = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "关闭",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (task.isRunning) {
                            Text(
                                text = "任务 ${task.name} 正在运行...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (taskLogs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "最新日志:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            taskLogs.takeLast(5).forEach { log ->
                                Text(
                                    text = log,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
