package com.daidai.app.ui.screen.webhelper

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebHelperScreen(
    onBack: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    var ruleName by remember { mutableStateOf("") }
    var envName by remember { mutableStateOf("") }
    var targetKeys by remember { mutableStateOf("") }
    var mainKey by remember { mutableStateOf("") }
    var connector by remember { mutableStateOf(";") }
    var showRuleDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Web 助手") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showRuleDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "规则配置")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 网址输入
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("网址") },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 规则选择
            OutlinedTextField(
                value = ruleName,
                onValueChange = { ruleName = it },
                label = { Text("规则名称") },
                leadingIcon = { Icon(Icons.Default.Rule, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { /* TODO: 选择规则 */ }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "选择规则")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 提取按钮
            Button(
                onClick = { /* TODO: 提取 Cookie */ },
                modifier = Modifier.fillMaxWidth(),
                enabled = url.isNotBlank() && ruleName.isNotBlank()
            ) {
                Icon(Icons.Default.ContentPaste, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("提取 Cookie")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 结果展示
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "提取结果",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "暂无数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 导入按钮
            Button(
                onClick = { /* TODO: 导入到面板 */ },
                modifier = Modifier.fillMaxWidth(),
                enabled = false // 需要有提取结果才能导入
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("导入到面板")
            }
        }
    }

    // 规则配置对话框
    if (showRuleDialog) {
        RuleConfigDialog(
            onDismiss = { showRuleDialog = false },
            onSave = { rule ->
                // TODO: 保存规则
                showRuleDialog = false
            }
        )
    }
}

@Composable
fun RuleConfigDialog(
    onDismiss: () -> Unit,
    onSave: (Rule) -> Unit
) {
    var envName by remember { mutableStateOf("") }
    var ruleName by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var targetKeys by remember { mutableStateOf("") }
    var mainKey by remember { mutableStateOf("") }
    var connector by remember { mutableStateOf(";") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("规则配置") },
        text = {
            Column {
                OutlinedTextField(
                    value = envName,
                    onValueChange = { envName = it },
                    label = { Text("环境变量") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it },
                    label = { Text("规则名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("网址") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = targetKeys,
                    onValueChange = { targetKeys = it },
                    label = { Text("目标键") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = mainKey,
                    onValueChange = { mainKey = it },
                    label = { Text("主键") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = connector,
                    onValueChange = { connector = it },
                    label = { Text("连接符") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(Rule(envName, ruleName, url, targetKeys, mainKey, connector))
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

data class Rule(
    val envName: String,
    val ruleName: String,
    val url: String,
    val targetKeys: String,
    val mainKey: String,
    val connector: String
)
