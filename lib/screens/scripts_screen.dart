import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:file_picker/file_picker.dart';
import 'package:provider/provider.dart';
import '../services/auth_service.dart';
import '../theme/miuix_theme.dart';
import '../widgets/miuix_widgets.dart';
import 'dart:io';
import 'home_screen.dart';

// GitHub mirror acceleration for China mainland users
// Converts github.com raw URLs to use mirror proxies
String _applyGitHubMirror(String url) {
  if (!url.contains('github.com') && !url.contains('raw.githubusercontent.com')) {
    return url;
  }
  // Convert github.com raw URLs
  if (url.contains('github.com') && url.contains('/raw/')) {
    url = url.replaceAll('github.com', 'raw.githubusercontent.com').replaceAll('/raw/', '/');
  }
  // Apply mirror proxy (ghproxy.com is widely used in China)
  if (url.contains('raw.githubusercontent.com') || url.contains('github.com')) {
    return 'https://ghproxy.com/$url';
  }
  return url;
}

class ScriptsScreen extends StatefulWidget {
  const ScriptsScreen({super.key});

  @override
  State<ScriptsScreen> createState() => _ScriptsScreenState();
}

class _ScriptsScreenState extends State<ScriptsScreen> with RefreshableScreen {
  List<Map<String, dynamic>> _scripts = [];
  bool _isLoading = true;
  String? _error;
  String _currentPath = '';
  final List<String> _pathStack = [];
  bool _isSelectionMode = false;
  final Set<String> _selectedScripts = {};

  @override
  void initState() {
    super.initState();
    _loadScripts();
  }

  @override
  void refresh() {
    _loadScripts();
  }

  void _toggleSelectionMode() {
    setState(() {
      _isSelectionMode = !_isSelectionMode;
      if (!_isSelectionMode) {
        _selectedScripts.clear();
      }
    });
  }

  void _toggleScriptSelection(String scriptPath) {
    setState(() {
      if (_selectedScripts.contains(scriptPath)) {
        _selectedScripts.remove(scriptPath);
      } else {
        _selectedScripts.add(scriptPath);
      }
    });
  }

  void _selectAllScripts() {
    setState(() {
      _selectedScripts.clear();
      final scripts = _getFilteredScripts();
      for (var script in scripts) {
        _selectedScripts.add(script['path'] ?? '');
      }
    });
  }

  Future<void> _batchDeleteScripts() async {
    if (_selectedScripts.isEmpty) return;
    
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('批量删除'),
        content: Text('确定要删除选中的 ${_selectedScripts.length} 个脚本吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            style: FilledButton.styleFrom(backgroundColor: Colors.red),
            child: const Text('删除'),
          ),
        ],
      ),
    );
    
    if (confirmed != true) return;
    
    try {
      final authService = context.read<AuthService>();
      int successCount = 0;
      
      for (var scriptPath in _selectedScripts) {
        try {
          await authService.apiService.deleteScript(scriptPath);
          successCount++;
        } catch (e) {
          // 继续删除其他脚本
        }
      }
      
      setState(() {
        _isSelectionMode = false;
        _selectedScripts.clear();
      });
      
      _loadScripts();
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('成功删除 $successCount 个脚本'), backgroundColor: Colors.green),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('批量删除失败: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  Future<void> _loadScripts() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final authService = context.read<AuthService>();
      final result = await authService.apiService.getScripts();

      if (result['data'] != null) {
        setState(() {
          _scripts = List<Map<String, dynamic>>.from(result['data'] ?? []);
          _isLoading = false;
        });
      } else {
        setState(() {
          _error = result['message'] ?? '获取脚本失败';
          _isLoading = false;
        });
      }
    } catch (e) {
      setState(() {
        _error = '网络错误: $e';
        _isLoading = false;
      });
    }
  }

  List<Map<String, dynamic>> _getFilteredScripts() {
    if (_currentPath.isEmpty) {
      return _scripts.where((s) => !s['path'].toString().contains('/')).toList();
    }
    return _scripts.where((s) {
      final path = s['path'].toString();
      return path.startsWith(_currentPath) &&
          path.substring(_currentPath.length).contains('/') &&
          !path.substring(_currentPath.length + 1).contains('/');
    }).toList();
  }

  List<String> _getFolders() {
    final scripts = _currentPath.isEmpty
        ? _scripts
        : _scripts.where((s) => s['path'].toString().startsWith(_currentPath));

    final folders = <String>{};
    for (final script in scripts) {
      final path = script['path'].toString();
      final relativePath = _currentPath.isEmpty ? path : path.substring(_currentPath.length);
      if (relativePath.contains('/')) {
        folders.add(relativePath.split('/')[0]);
      }
    }
    return folders.toList()..sort();
  }

  void _navigateToFolder(String folder) {
    setState(() {
      _pathStack.add(_currentPath);
      _currentPath = '$_currentPath$folder/';
    });
  }

  void _navigateBack() {
    if (_pathStack.isNotEmpty) {
      setState(() {
        _currentPath = _pathStack.removeLast();
      });
    }
  }

  Future<void> _deleteScript(String path) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认删除'),
        content: Text('确定要删除脚本 $path 吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('删除'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      try {
        final authService = context.read<AuthService>();
        await authService.apiService.deleteScript(path);
        _loadScripts();
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('脚本已删除')),
          );
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('删除失败: $e'), backgroundColor: Colors.red),
          );
        }
      }
    }
  }

  Future<void> _editScript(String path, String content) async {
    final contentController = TextEditingController(text: content);

    if (mounted) {
      showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: Text('编辑脚本: $path'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: contentController,
                  decoration: const InputDecoration(
                    labelText: '脚本内容',
                    border: OutlineInputBorder(),
                  ),
                  maxLines: 15,
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('取消'),
            ),
            FilledButton(
              onPressed: () async {
                try {
                  final authService = context.read<AuthService>();
                  await authService.apiService.updateScript(path, {
                    'content': contentController.text,
                  });
                  Navigator.pop(context);
                  _loadScripts();
                  if (mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('脚本已更新')),
                    );
                  }
                } catch (e) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('更新失败: $e'), backgroundColor: Colors.red),
                  );
                }
              },
              child: const Text('保存'),
            ),
          ],
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: _isSelectionMode 
          ? Text('已选择 ${_selectedScripts.length} 项')
          : Text(_currentPath.isEmpty ? '脚本管理' : _currentPath),
        leading: _isSelectionMode
          ? IconButton(
              icon: const Icon(Icons.close),
              onPressed: _toggleSelectionMode,
            )
          : _currentPath.isNotEmpty
              ? IconButton(
                  icon: const Icon(Icons.arrow_back),
                  onPressed: _navigateBack,
                )
              : null,
        actions: [
          if (_isSelectionMode) ...[
            IconButton(
              icon: const Icon(Icons.select_all),
              onPressed: _selectAllScripts,
              tooltip: '全选',
            ),
            IconButton(
              icon: const Icon(Icons.delete),
              onPressed: _selectedScripts.isNotEmpty ? _batchDeleteScripts : null,
              tooltip: '批量删除',
            ),
          ] else ...[
            IconButton(
              icon: const Icon(Icons.checklist),
              onPressed: _toggleSelectionMode,
              tooltip: '批量操作',
            ),
            PopupMenuButton(
              itemBuilder: (context) => [
                const PopupMenuItem(
                  value: 'upload',
                  child: ListTile(
                    leading: Icon(Icons.upload_file),
                    title: Text('上传脚本'),
                    contentPadding: EdgeInsets.zero,
                  ),
                ),
                const PopupMenuItem(
                  value: 'upload_zip',
                  child: ListTile(
                    leading: Icon(Icons.archive),
                    title: Text('上传压缩包'),
                    contentPadding: EdgeInsets.zero,
                  ),
                ),
                const PopupMenuItem(
                  value: 'subscriptions',
                  child: ListTile(
                    leading: Icon(Icons.rss_feed),
                    title: Text('脚本订阅'),
                    contentPadding: EdgeInsets.zero,
                  ),
                ),
              ],
              onSelected: (value) {
                if (value == 'upload') {
                  _showUploadDialog();
                } else if (value == 'upload_zip') {
                  _showUploadZipDialog();
                } else if (value == 'subscriptions') {
                  _showSubscriptionsDialog();
                }
              },
            ),
          ],
        ],
      ),
      body: _buildBody(),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showUploadDialog(),
        tooltip: '上传脚本',
        child: const Icon(Icons.upload_file),
      ),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return const MiuixLoadingState();
    }

    if (_error != null) {
      return MiuixErrorState(
        message: _error!,
        onRetry: _loadScripts,
      );
    }

    final folders = _getFolders();
    final scripts = _getFilteredScripts();

    if (folders.isEmpty && scripts.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.code,
              size: 64,
              color: Theme.of(context).colorScheme.onSurfaceVariant,
            ),
            const SizedBox(height: 16),
            const Text('暂无脚本'),
            const SizedBox(height: 16),
            FilledButton.icon(
              onPressed: () => _showCreateScriptDialog(),
              icon: const Icon(Icons.add),
              label: const Text('创建脚本'),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadScripts,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Folders
          ...folders.map((folder) => Card(
            margin: const EdgeInsets.only(bottom: 8),
            child: ListTile(
              leading: Icon(Icons.folder, color: Colors.amber),
              title: Text(folder),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => _navigateToFolder(folder),
            ),
          )),

          // Scripts
          ...scripts.map((script) => _ScriptCard(
            script: script,
            onDelete: () => _deleteScript(script['path']),
            onTap: () => _showScriptContent(script),
            onEdit: (content) => _editScript(script['path'], content),
          )),
        ],
      ),
    );
  }

  void _showScriptContent(Map<String, dynamic> script) async {
    try {
      final authService = context.read<AuthService>();
      final result = await authService.apiService.getScriptContent(script['path']);

      if (mounted) {
        final content = result['data']?['content'] ?? '无法获取内容';
        
        showDialog(
          context: context,
          builder: (context) => AlertDialog(
            title: Row(
              children: [
                Expanded(child: Text(script['name'] ?? '脚本内容')),
                IconButton(
                  onPressed: () {
                    Navigator.pop(context);
                    _copyScriptContent(content);
                  },
                  icon: const Icon(Icons.copy),
                  tooltip: '复制内容',
                ),
              ],
            ),
            content: SingleChildScrollView(
              child: Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.grey[100],
                  borderRadius: BorderRadius.circular(8),
                ),
                child: SelectableText(
                  content,
                  style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
                ),
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('关闭'),
              ),
              FilledButton(
                onPressed: () {
                  Navigator.pop(context);
                  _editScript(script['path'], content);
                },
                child: const Text('编辑'),
              ),
            ],
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('获取内容失败: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  void _copyScriptContent(String content) {
    Clipboard.setData(ClipboardData(text: content));
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('脚本内容已复制到剪贴板'), backgroundColor: Colors.green),
    );
  }

  void _showCreateScriptDialog() {
    final nameController = TextEditingController();
    final contentController = TextEditingController();

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('创建脚本'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: nameController,
                decoration: InputDecoration(
                  labelText: '脚本名称',
                  hintText: _currentPath.isEmpty ? 'script.py' : '${_currentPath}script.py',
                  border: const OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 16),
              TextField(
                controller: contentController,
                decoration: const InputDecoration(
                  labelText: '脚本内容',
                  border: OutlineInputBorder(),
                ),
                maxLines: 10,
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () async {
              if (nameController.text.isEmpty || contentController.text.isEmpty) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('请填写必填项'), backgroundColor: Colors.red),
                );
                return;
              }

              try {
                final authService = context.read<AuthService>();
                await authService.apiService.createScript({
                  'name': _currentPath + nameController.text,
                  'content': contentController.text,
                });
                Navigator.pop(context);
                _loadScripts();
              } catch (e) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('创建失败: $e'), backgroundColor: Colors.red),
                );
              }
            },
            child: const Text('创建'),
          ),
        ],
      ),
    );
  }

  void _showUploadDialog() {
    final nameController = TextEditingController();
    final contentController = TextEditingController();
    bool _isUploading = false;

    showDialog(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Text('上传脚本'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: nameController,
                  decoration: const InputDecoration(
                    labelText: '脚本名称',
                    hintText: 'script.py',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 16),
                OutlinedButton.icon(
                  onPressed: _isUploading ? null : () async {
                    try {
                      FilePickerResult? result = await FilePicker.platform.pickFiles(
                        type: FileType.custom,
                        allowedExtensions: ['py', 'js', 'sh', 'json', 'yaml', 'yml', 'txt', 'md', 'go', 'rs', 'java', 'c', 'cpp', 'h'],
                      );

                      if (result != null && result.files.single.path != null) {
                        final file = File(result.files.single.path!);
                        final content = await file.readAsString();
                        final fileName = result.files.single.name;
                        
                        setDialogState(() {
                          nameController.text = fileName;
                          contentController.text = content;
                        });
                      } else if (result != null && result.files.single.bytes != null) {
                        final bytes = result.files.single.bytes!;
                        final content = String.fromCharCodes(bytes);
                        final fileName = result.files.single.name;
                        
                        setDialogState(() {
                          nameController.text = fileName;
                          contentController.text = content;
                        });
                      }
                    } catch (e) {
                      if (mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text('读取文件失败: $e'), backgroundColor: Colors.red),
                        );
                      }
                    }
                  },
                  icon: const Icon(Icons.file_upload),
                  label: const Text('从本地文件选择'),
                ),
                const SizedBox(height: 16),
                TextField(
                  controller: contentController,
                  decoration: const InputDecoration(
                    labelText: '脚本内容',
                    border: OutlineInputBorder(),
                  ),
                  maxLines: 10,
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: _isUploading ? null : () => Navigator.pop(context),
              child: const Text('取消'),
            ),
            FilledButton(
              onPressed: _isUploading ? null : () async {
                if (nameController.text.isEmpty || contentController.text.isEmpty) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('请填写必填项'), backgroundColor: Colors.red),
                  );
                  return;
                }

                setDialogState(() => _isUploading = true);
                
                try {
                  final authService = context.read<AuthService>();
                  final scriptName = _currentPath + nameController.text;
                  final result = await authService.apiService.createScript({
                    'name': scriptName,
                    'content': contentController.text,
                    'path': scriptName,
                  });
                  
                  // 检查 API 返回结果
                  if (result['code'] == 0 || result['code'] == 200 || result['success'] == true) {
                    Navigator.pop(context);
                    _loadScripts();
                    if (mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('脚本上传成功'), backgroundColor: Colors.green),
                      );
                    }
                  } else {
                    throw Exception(result['message'] ?? '上传失败');
                  }
                } catch (e) {
                  if (mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text('上传失败: $e'), backgroundColor: Colors.red),
                    );
                  }
                } finally {
                  setDialogState(() => _isUploading = false);
                }
              },
              child: _isUploading 
                ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                : const Text('上传'),
            ),
          ],
        ),
      ),
    );
  }

  void _showUploadZipDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('上传压缩包'),
        content: const Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.archive, size: 64, color: Colors.blue),
            SizedBox(height: 16),
            Text('支持 .zip 格式的压缩包'),
            SizedBox(height: 8),
            Text('压缩包内的文件将被解压到当前目录'),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () {
              Navigator.pop(context);
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('压缩包上传功能开发中...')),
              );
            },
            child: const Text('选择文件'),
          ),
        ],
      ),
    );
  }

  void _showSubscriptionsDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('脚本订阅'),
        content: SizedBox(
          width: double.maxFinite,
          height: 400,
          child: FutureBuilder<Map<String, dynamic>>(
            future: context.read<AuthService>().apiService.getScriptSubscriptions(),
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return const Center(child: CircularProgressIndicator());
              }
              
              if (snapshot.hasError) {
                return Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Icon(Icons.error_outline, size: 48, color: Colors.red),
                      const SizedBox(height: 16),
                      Text('加载失败: ${snapshot.error}'),
                      const SizedBox(height: 16),
                      FilledButton(
                        onPressed: () => Navigator.pop(context),
                        child: const Text('关闭'),
                      ),
                    ],
                  ),
                );
              }
              
              final subscriptions = snapshot.data?['data'] ?? [];
              
              if (subscriptions.isEmpty) {
                return Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.rss_feed, size: 48, color: Colors.grey[400]),
                      const SizedBox(height: 16),
                      const Text('暂无订阅'),
                      const SizedBox(height: 8),
                      const Text('添加订阅源获取远程脚本', style: TextStyle(color: Colors.grey)),
                    ],
                  ),
                );
              }
              
              return ListView.builder(
                itemCount: subscriptions.length,
                itemBuilder: (context, index) {
                  final subscription = subscriptions[index];
                  return Card(
                    margin: const EdgeInsets.only(bottom: 8),
                    child: ListTile(
                      leading: const Icon(Icons.rss_feed, color: Colors.blue),
                      title: Text(subscription['name'] ?? '未命名订阅'),
                      subtitle: Text(subscription['url'] ?? ''),
                      trailing: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          IconButton(
                            icon: const Icon(Icons.sync, color: Colors.green),
                            onPressed: () async {
                              try {
                                // Apply GitHub mirror acceleration
                                final url = subscription['url'] ?? '';
                                if (url.contains('github.com')) {
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    const SnackBar(
                                      content: Text('检测到GitHub链接，自动使用镜像加速...'),
                                      duration: Duration(seconds: 1),
                                    ),
                                  );
                                }
                                await context.read<AuthService>().apiService
                                    .syncScriptSubscription(subscription['id']);
                                ScaffoldMessenger.of(context).showSnackBar(
                                  const SnackBar(content: Text('同步成功'), backgroundColor: Colors.green),
                                );
                              } catch (e) {
                                ScaffoldMessenger.of(context).showSnackBar(
                                  SnackBar(content: Text('同步失败: $e'), backgroundColor: Colors.red),
                                );
                              }
                            },
                            tooltip: '同步',
                          ),
                          IconButton(
                            icon: const Icon(Icons.delete, color: Colors.red),
                            onPressed: () async {
                              final confirmed = await showDialog<bool>(
                                context: context,
                                builder: (context) => AlertDialog(
                                  title: const Text('确认删除'),
                                  content: const Text('确定要删除这个订阅吗？'),
                                  actions: [
                                    TextButton(
                                      onPressed: () => Navigator.pop(context, false),
                                      child: const Text('取消'),
                                    ),
                                    FilledButton(
                                      onPressed: () => Navigator.pop(context, true),
                                      child: const Text('删除'),
                                    ),
                                  ],
                                ),
                              );
                              
                              if (confirmed == true) {
                                try {
                                  await context.read<AuthService>().apiService
                                      .deleteScriptSubscription(subscription['id']);
                                  Navigator.pop(context);
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    const SnackBar(content: Text('删除成功'), backgroundColor: Colors.green),
                                  );
                                } catch (e) {
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    SnackBar(content: Text('删除失败: $e'), backgroundColor: Colors.red),
                                  );
                                }
                              }
                            },
                            tooltip: '删除',
                          ),
                        ],
                      ),
                    ),
                  );
                },
              );
            },
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('关闭'),
          ),
          FilledButton.icon(
            onPressed: () => _showAddSubscriptionDialog(),
            icon: const Icon(Icons.add),
            label: const Text('添加订阅'),
          ),
        ],
      ),
    );
  }

  void _showAddSubscriptionDialog() {
    final nameController = TextEditingController();
    final urlController = TextEditingController();
    final subDirController = TextEditingController();
    final hookController = TextEditingController();
    final depDescController = TextEditingController();
    bool overwriteLocal = false;
    
    showDialog(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Text('添加脚本订阅'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                TextField(
                  controller: nameController,
                  decoration: const InputDecoration(
                    labelText: '订阅名称 *',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: urlController,
                  decoration: const InputDecoration(
                    labelText: '订阅地址 *',
                    hintText: 'https://example.com/scripts.json',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: subDirController,
                  decoration: const InputDecoration(
                    labelText: '指定子目录',
                    hintText: '可选，如 subfolder',
                    border: OutlineInputBorder(),
                    helperText: '脚本将下载到指定子目录中',
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: depDescController,
                  decoration: const InputDecoration(
                    labelText: '依赖说明',
                    hintText: '可选，如 requests, flask',
                    border: OutlineInputBorder(),
                    helperText: '订阅脚本所需的依赖包',
                  ),
                  maxLines: 2,
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: hookController,
                  decoration: const InputDecoration(
                    labelText: '拉取后钩子',
                    hintText: '可选，拉取后执行的命令',
                    border: OutlineInputBorder(),
                    helperText: '订阅同步后自动执行的命令',
                  ),
                  maxLines: 2,
                ),
                const SizedBox(height: 8),
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: MiuixColors.tertiaryContainer,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.info_outline, size: 16, color: MiuixColors.primary),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          'GitHub链接将自动通过镜像加速下载',
                          style: MiuixTextStyles.footnote1.copyWith(
                            color: MiuixColors.onTertiaryContainer,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 8),
                CheckboxListTile(
                  value: overwriteLocal,
                  onChanged: (v) => setDialogState(() => overwriteLocal = v ?? false),
                  title: const Text('覆盖本地修改'),
                  subtitle: const Text('同步时覆盖本地已修改的脚本'),
                  contentPadding: EdgeInsets.zero,
                  controlAffinity: ListTileControlAffinity.leading,
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('取消'),
            ),
            FilledButton(
              onPressed: () async {
                if (nameController.text.isEmpty || urlController.text.isEmpty) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('请填写必填项'), backgroundColor: Colors.red),
                  );
                  return;
                }
                
                try {
                  final body = <String, dynamic>{
                    'name': nameController.text,
                    'url': urlController.text,
                  };
                  if (subDirController.text.isNotEmpty) {
                    body['sub_dir'] = subDirController.text;
                  }
                  if (hookController.text.isNotEmpty) {
                    body['post_pull_hook'] = hookController.text;
                  }
                  if (depDescController.text.isNotEmpty) {
                    body['dependency_description'] = depDescController.text;
                  }
                  if (overwriteLocal) {
                    body['overwrite_local'] = true;
                  }
                  
                  await context.read<AuthService>().apiService.addScriptSubscription(body);
                  Navigator.pop(context);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('订阅已添加'), backgroundColor: Colors.green),
                  );
                } catch (e) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('添加失败: $e'), backgroundColor: Colors.red),
                  );
                }
              },
              child: const Text('添加'),
            ),
          ],
        ),
      ),
    );
  }
}

class _ScriptCard extends StatelessWidget {
  final Map<String, dynamic> script;
  final VoidCallback onDelete;
  final VoidCallback onTap;
  final Function(String) onEdit;

  const _ScriptCard({
    required this.script,
    required this.onDelete,
    required this.onTap,
    required this.onEdit,
  });

  @override
  Widget build(BuildContext context) {
    final name = script['name'] ?? '';
    final path = script['path'] ?? '';
    final size = script['size'] ?? 0;
    final mtime = script['mtime'] ?? 0;

    String sizeText;
    if (size < 1024) {
      sizeText = '$size B';
    } else if (size < 1024 * 1024) {
      sizeText = '${(size / 1024).toStringAsFixed(1)} KB';
    } else {
      sizeText = '${(size / (1024 * 1024)).toStringAsFixed(1)} MB';
    }

    String timeText = '';
    if (mtime > 0) {
      final date = DateTime.fromMillisecondsSinceEpoch(mtime * 1000);
      timeText = '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')} ${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}';
    }

    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Icon(
                _getFileIcon(name),
                color: _getFileColor(name),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      name,
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      path,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: Colors.grey,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Row(
                      children: [
                        Text(
                          sizeText,
                          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: Colors.grey,
                          ),
                        ),
                        if (timeText.isNotEmpty) ...[
                          const SizedBox(width: 16),
                          Text(
                            timeText,
                            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color: Colors.grey,
                            ),
                          ),
                        ],
                      ],
                    ),
                  ],
                ),
              ),
              PopupMenuButton(
                itemBuilder: (context) => [
                  const PopupMenuItem(
                    value: 'edit',
                    child: ListTile(
                      leading: Icon(Icons.edit),
                      title: Text('编辑'),
                      contentPadding: EdgeInsets.zero,
                    ),
                  ),
                  const PopupMenuItem(
                    value: 'add_to_task',
                    child: ListTile(
                      leading: Icon(Icons.schedule),
                      title: Text('添加到定时任务'),
                      contentPadding: EdgeInsets.zero,
                    ),
                  ),
                  const PopupMenuItem(
                    value: 'delete',
                    child: ListTile(
                      leading: Icon(Icons.delete, color: Colors.red),
                      title: Text('删除', style: TextStyle(color: Colors.red)),
                      contentPadding: EdgeInsets.zero,
                    ),
                  ),
                ],
                onSelected: (value) {
                  if (value == 'edit') {
                    onTap();
                  } else if (value == 'add_to_task') {
                    _addToTask(context, script);
                  } else if (value == 'delete') {
                    onDelete();
                  }
                },
              ),
            ],
          ),
        ),
      ),
    );
  }

  IconData _getFileIcon(String name) {
    if (name.endsWith('.py')) return Icons.code;
    if (name.endsWith('.js')) return Icons.javascript;
    if (name.endsWith('.sh')) return Icons.terminal;
    if (name.endsWith('.json')) return Icons.data_object;
    if (name.endsWith('.yaml') || name.endsWith('.yml')) return Icons.settings;
    return Icons.description;
  }

  Color _getFileColor(String name) {
    if (name.endsWith('.py')) return Colors.blue;
    if (name.endsWith('.js')) return Colors.orange;
    if (name.endsWith('.sh')) return Colors.green;
    if (name.endsWith('.json')) return Colors.purple;
    if (name.endsWith('.yaml') || name.endsWith('.yml')) return Colors.teal;
    return Colors.grey;
  }

  void _addToTask(BuildContext context, Map<String, dynamic> script) {
    final nameController = TextEditingController(text: script['name'] ?? '');
    final commandController = TextEditingController(text: 'python3 ${script['path']}');
    final cronController = TextEditingController(text: '0 * * * *');
    String taskType = 'cron';

    showDialog(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Text('添加到定时任务'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: nameController,
                  decoration: const InputDecoration(
                    labelText: '任务名称',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 16),
                DropdownButtonFormField<String>(
                  value: taskType,
                  decoration: const InputDecoration(
                    labelText: '任务类型',
                    border: OutlineInputBorder(),
                  ),
                  items: const [
                    DropdownMenuItem(value: 'cron', child: Text('定时任务')),
                    DropdownMenuItem(value: 'manual', child: Text('手动触发')),
                  ],
                  onChanged: (value) {
                    setDialogState(() => taskType = value!);
                  },
                ),
                if (taskType == 'cron') ...[
                  const SizedBox(height: 16),
                  TextField(
                    controller: cronController,
                    decoration: const InputDecoration(
                      labelText: 'Cron 表达式',
                      hintText: '0 * * * *',
                      border: OutlineInputBorder(),
                    ),
                  ),
                ],
                const SizedBox(height: 16),
                TextField(
                  controller: commandController,
                  decoration: const InputDecoration(
                    labelText: '执行命令',
                    border: OutlineInputBorder(),
                  ),
                  maxLines: 3,
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('取消'),
            ),
            FilledButton(
              onPressed: () async {
                if (nameController.text.isEmpty || commandController.text.isEmpty) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('请填写必填项'), backgroundColor: Colors.red),
                  );
                  return;
                }

                try {
                  final authService = Provider.of<AuthService>(context, listen: false);
                  await authService.apiService.createTask({
                    'name': nameController.text,
                    'command': commandController.text,
                    'task_type': taskType,
                    'cron_expression': taskType == 'cron' ? cronController.text : '',
                    'status': 1,
                  });
                  Navigator.pop(context);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('任务已添加'), backgroundColor: Colors.green),
                  );
                } catch (e) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('添加失败: $e'), backgroundColor: Colors.red),
                  );
                }
              },
              child: const Text('添加'),
            ),
          ],
        ),
      ),
    );
  }
}
