import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'dart:convert';
import '../services/auth_service.dart';
import 'home_screen.dart';

class DependenciesScreen extends StatefulWidget {
  const DependenciesScreen({super.key});

  @override
  State<DependenciesScreen> createState() => _DependenciesScreenState();
}

class _DependenciesScreenState extends State<DependenciesScreen> with RefreshableScreen {
  List<Map<String, dynamic>> _dependencies = [];
  bool _isLoading = true;
  String? _error;
  String _filterType = 'all'; // all, nodejs, python, linux
  Map<int, String> _installingDeps = {}; // depId -> status message
  Map<int, bool> _loadingLogs = {}; // depId -> loading

  @override
  void initState() {
    super.initState();
    _loadDependencies();
  }

  @override
  void refresh() {
    _loadDependencies();
  }

  Future<void> _loadDependencies() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final authService = context.read<AuthService>();
      final result = await authService.apiService.getDependencies();

      if (mounted) {
        List<dynamic> deps = [];
        if (result['data'] is List) {
          deps = result['data'];
        } else if (result['data'] is Map && result['data']['data'] is List) {
          deps = result['data']['data'];
        } else if (result['deps'] is List) {
          deps = result['deps'];
        } else if (result['items'] is List) {
          deps = result['items'];
        }
        
        setState(() {
          _dependencies = List<Map<String, dynamic>>.from(deps);
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = '获取依赖失败: $e';
          _isLoading = false;
        });
      }
    }
  }

  List<Map<String, dynamic>> _getFilteredDeps() {
    if (_filterType == 'all') return _dependencies;
    // Map filter type to QingLong type code
    int filterCode;
    switch (_filterType) {
      case 'nodejs': filterCode = 0; break;
      case 'python': filterCode = 1; break;
      case 'linux': filterCode = 2; break;
      default: return _dependencies;
    }
    return _dependencies.where((dep) {
      final type = dep['type'] ?? dep['typeCode'] ?? 0;
      return type == filterCode;
    }).toList();
  }

  // Install dependency: POST /dependencies with [{name, type}]
  Future<void> _installDep(String type, List<String> names) async {
    final depKey = '${type}_${names.join(",")}';
    setState(() {
      _installingDeps[depKey.hashCode] = '正在安装...';
    });

    try {
      final authService = context.read<AuthService>();
      await authService.apiService.installDependency(type, names);

      if (mounted) {
        setState(() {
          _installingDeps.remove(depKey.hashCode);
        });
        _loadDependencies();
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('依赖安装请求已提交: ${names.join(", ")}'), backgroundColor: Colors.green),
        );
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _installingDeps[depKey.hashCode] = '安装失败';
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('安装失败: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  // Uninstall dependency: DELETE /dependencies/force with [id]
  Future<void> _uninstallDep(int id, String name) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认卸载'),
        content: Text('确定要卸载依赖 "$name" 吗？'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('取消')),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            style: FilledButton.styleFrom(backgroundColor: Colors.red),
            child: const Text('卸载'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      try {
        final authService = context.read<AuthService>();
        await authService.apiService.uninstallDependency(id);
        _loadDependencies();
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('依赖已卸载'), backgroundColor: Colors.green),
          );
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('卸载失败: $e'), backgroundColor: Colors.red),
          );
        }
      }
    }
  }

  // Reinstall dependency: PUT /dependencies/reinstall with [id]
  Future<void> _reinstallDep(int id) async {
    setState(() {
      _installingDeps[id] = '正在重新安装...';
    });

    try {
      final authService = context.read<AuthService>();
      await authService.apiService.reinstallDependency(id);
      _loadDependencies();
      if (mounted) {
        setState(() {
          _installingDeps.remove(id);
        });
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('重新安装请求已提交'), backgroundColor: Colors.green),
        );
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _installingDeps[id] = '重装失败';
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('重新安装失败: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  // View dependency log: GET /dependencies/{id}
  Future<void> _viewDepLog(int id, String name) async {
    setState(() => _loadingLogs[id] = true);
    try {
      final authService = context.read<AuthService>();
      final result = await authService.apiService.getDependencyLog(id);
      if (mounted) {
        setState(() => _loadingLogs.remove(id));
        // Extract log content
        String logContent = '';
        final data = result['data'];
        if (data is Map && data['log'] is List) {
          logContent = (data['log'] as List).join('\n');
        } else if (data is String) {
          logContent = data;
        } else if (result['log'] is List) {
          logContent = (result['log'] as List).join('\n');
        }
        _showLogDialog(name, logContent);
      }
    } catch (e) {
      if (mounted) {
        setState(() => _loadingLogs.remove(id));
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('获取日志失败: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  void _showLogDialog(String name, String content) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('$name 安装日志'),
        content: SizedBox(
          width: double.maxFinite,
          height: 400,
          child: SingleChildScrollView(
            child: SelectableText(
              content.isEmpty ? '暂无日志' : content,
              style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
            ),
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('关闭')),
        ],
      ),
    );
  }

  void _showInstallDialog() {
    String selectedType = 'nodejs';
    final nameController = TextEditingController();

    showDialog(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Text('安装依赖'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('依赖类型', style: TextStyle(fontWeight: FontWeight.bold)),
                const SizedBox(height: 8),
                SegmentedButton<String>(
                  segments: const [
                    ButtonSegment(value: 'nodejs', label: Text('Node.js')),
                    ButtonSegment(value: 'python', label: Text('Python')),
                    ButtonSegment(value: 'linux', label: Text('Linux')),
                  ],
                  selected: {selectedType},
                  onSelectionChanged: (values) {
                    setDialogState(() => selectedType = values.first);
                  },
                ),
                const SizedBox(height: 16),
                const Text('依赖名称', style: TextStyle(fontWeight: FontWeight.bold)),
                const SizedBox(height: 8),
                TextField(
                  controller: nameController,
                  decoration: const InputDecoration(
                    hintText: '例如: axios, requests, curl',
                    border: OutlineInputBorder(),
                    helperText: '多个依赖用逗号分隔',
                  ),
                  maxLines: 3,
                ),
                const SizedBox(height: 16),
                _buildPresetChips(selectedType, nameController),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(context), child: const Text('取消')),
            FilledButton.icon(
              onPressed: () {
                if (nameController.text.isEmpty) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('请输入依赖名称'), backgroundColor: Colors.red),
                  );
                  return;
                }
                Navigator.pop(context);
                final names = nameController.text.split(',').map((e) => e.trim()).where((e) => e.isNotEmpty).toList();
                _installDep(selectedType, names);
              },
              icon: const Icon(Icons.download),
              label: const Text('安装'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPresetChips(String type, TextEditingController controller) {
    List<Map<String, String>> presets;
    switch (type) {
      case 'nodejs':
        presets = [
          {'label': 'axios', 'value': 'axios'},
          {'label': 'express', 'value': 'express'},
          {'label': 'lodash', 'value': 'lodash'},
          {'label': 'moment', 'value': 'moment'},
          {'label': 'typescript', 'value': 'typescript'},
          {'label': 'pm2', 'value': 'pm2'},
        ];
        break;
      case 'python':
        presets = [
          {'label': 'requests', 'value': 'requests'},
          {'label': 'flask', 'value': 'flask'},
          {'label': 'django', 'value': 'django'},
          {'label': 'numpy', 'value': 'numpy'},
          {'label': 'pandas', 'value': 'pandas'},
          {'label': 'pillow', 'value': 'pillow'},
        ];
        break;
      case 'linux':
        presets = [
          {'label': 'curl', 'value': 'curl'},
          {'label': 'wget', 'value': 'wget'},
          {'label': 'git', 'value': 'git'},
          {'label': 'vim', 'value': 'vim'},
          {'label': 'htop', 'value': 'htop'},
          {'label': 'unzip', 'value': 'unzip'},
        ];
        break;
      default:
        presets = [];
    }

    return Wrap(
      spacing: 8,
      runSpacing: 4,
      children: presets.map((preset) => ActionChip(
        label: Text(preset['label']!, style: const TextStyle(fontSize: 12)),
        onPressed: () {
          if (controller.text.isNotEmpty) {
            controller.text += ', ';
          }
          controller.text += preset['value']!;
        },
      )).toList(),
    );
  }

  @override
  Widget build(BuildContext context) {
    final filteredDeps = _getFilteredDeps();

    return Scaffold(
      appBar: AppBar(
        title: const Text('依赖管理'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadDependencies,
            tooltip: '刷新',
          ),
        ],
      ),
      body: Column(
        children: [
          // Filter chips
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: [
                  _buildFilterChip('全部', 'all'),
                  const SizedBox(width: 8),
                  _buildFilterChip('Node.js', 'nodejs'),
                  const SizedBox(width: 8),
                  _buildFilterChip('Python', 'python'),
                  const SizedBox(width: 8),
                  _buildFilterChip('Linux', 'linux'),
                ],
              ),
            ),
          ),
          // Stats
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Row(
              children: [
                _buildStatCard('Node.js', _dependencies.where((d) => (d['type'] ?? 0) == 0).length, Colors.green),
                const SizedBox(width: 8),
                _buildStatCard('Python', _dependencies.where((d) => (d['type'] ?? 0) == 1).length, Colors.blue),
                const SizedBox(width: 8),
                _buildStatCard('Linux', _dependencies.where((d) => (d['type'] ?? 0) == 2).length, Colors.orange),
              ],
            ),
          ),
          const SizedBox(height: 8),
          // Dependencies list
          Expanded(
            child: _buildBody(filteredDeps),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _showInstallDialog,
        icon: const Icon(Icons.add),
        label: const Text('安装依赖'),
      ),
    );
  }

  Widget _buildFilterChip(String label, String value) {
    final isSelected = _filterType == value;
    return FilterChip(
      label: Text(label),
      selected: isSelected,
      onSelected: (selected) {
        setState(() => _filterType = value);
      },
    );
  }

  Widget _buildStatCard(String label, int count, Color color) {
    return Expanded(
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            children: [
              Text(
                '$count',
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                  color: color,
                ),
              ),
              Text(label, style: TextStyle(color: color)),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildBody(List<Map<String, dynamic>> deps) {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_error != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.error_outline, size: 64, color: Theme.of(context).colorScheme.error),
            const SizedBox(height: 16),
            Text(_error!),
            const SizedBox(height: 16),
            FilledButton(onPressed: _loadDependencies, child: const Text('重试')),
          ],
        ),
      );
    }

    if (deps.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.extension_off, size: 64, color: Theme.of(context).colorScheme.onSurfaceVariant),
            const SizedBox(height: 16),
            const Text('暂无依赖'),
            const SizedBox(height: 16),
            FilledButton.icon(
              onPressed: _showInstallDialog,
              icon: const Icon(Icons.add),
              label: const Text('安装依赖'),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadDependencies,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: deps.length,
        itemBuilder: (context, index) => _buildDepCard(deps[index]),
      ),
    );
  }

  Widget _buildDepCard(Map<String, dynamic> dep) {
    final id = dep['id'] ?? 0;
    final name = dep['name'] ?? '未知';
    final type = dep['type'] ?? 0;
    final status = dep['status'] ?? 0;
    // QingLong status: 0=installing, 1=installed, 2=failed, 3=uninstalling, 5=uninstall failed
    final createdAt = dep['createdAt'] ?? dep['created_at'] ?? '';

    Color statusColor;
    String statusText;
    IconData statusIcon;
    switch (status) {
      case 0:
        statusColor = Colors.orange;
        statusText = '安装中';
        statusIcon = Icons.hourglass_empty;
        break;
      case 1:
        statusColor = Colors.green;
        statusText = '已安装';
        statusIcon = Icons.check_circle;
        break;
      case 2:
        statusColor = Colors.red;
        statusText = '安装失败';
        statusIcon = Icons.error;
        break;
      case 3:
        statusColor = Colors.orange;
        statusText = '卸载中';
        statusIcon = Icons.hourglass_empty;
        break;
      case 5:
        statusColor = Colors.red;
        statusText = '卸载失败';
        statusIcon = Icons.error;
        break;
      default:
        statusColor = Colors.grey;
        statusText = '未知';
        statusIcon = Icons.help;
    }

    Color typeColor;
    String typeText;
    IconData typeIcon;
    switch (type) {
      case 0:
        typeColor = Colors.green;
        typeText = 'Node.js';
        typeIcon = Icons.javascript;
        break;
      case 1:
        typeColor = Colors.blue;
        typeText = 'Python';
        typeIcon = Icons.code;
        break;
      case 2:
        typeColor = Colors.orange;
        typeText = 'Linux';
        typeIcon = Icons.terminal;
        break;
      default:
        typeColor = Colors.grey;
        typeText = '未知';
        typeIcon = Icons.extension;
    }

    final isInstalling = _installingDeps.containsKey(id);
    final isLoadingLog = _loadingLogs.containsKey(id);

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(typeIcon, color: typeColor, size: 20),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    name,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: statusColor.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: statusColor),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      if (isInstalling)
                        SizedBox(
                          width: 12,
                          height: 12,
                          child: CircularProgressIndicator(strokeWidth: 2, color: statusColor),
                        )
                      else
                        Icon(statusIcon, size: 14, color: statusColor),
                      const SizedBox(width: 4),
                      Text(
                        isInstalling ? _installingDeps[id]! : statusText,
                        style: TextStyle(
                          color: statusColor,
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 4),
            Text('类型: $typeText', style: Theme.of(context).textTheme.bodySmall),
            if (createdAt.isNotEmpty)
              Text('创建: $createdAt', style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Colors.grey, fontSize: 11)),
            const SizedBox(height: 12),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                // View log button
                OutlinedButton.icon(
                  onPressed: isLoadingLog ? null : () => _viewDepLog(id, name),
                  icon: isLoadingLog
                      ? const SizedBox(width: 14, height: 14, child: CircularProgressIndicator(strokeWidth: 2))
                      : const Icon(Icons.article, size: 16),
                  label: const Text('日志'),
                ),
                const SizedBox(width: 8),
                if (status == 0 || status == 2) ...[
                  // Installing or failed: show reinstall
                  FilledButton.icon(
                    onPressed: isInstalling ? null : () => _reinstallDep(id),
                    icon: const Icon(Icons.refresh, size: 16),
                    label: const Text('重装'),
                  ),
                ],
                if (status == 1) ...[
                  // Installed: show reinstall and uninstall
                  OutlinedButton.icon(
                    onPressed: isInstalling ? null : () => _reinstallDep(id),
                    icon: const Icon(Icons.refresh, size: 16),
                    label: const Text('重装'),
                  ),
                  const SizedBox(width: 8),
                  FilledButton.icon(
                    onPressed: isInstalling ? null : () => _uninstallDep(id, name),
                    icon: const Icon(Icons.delete, size: 16),
                    label: const Text('卸载'),
                    style: FilledButton.styleFrom(backgroundColor: Colors.red),
                  ),
                ],
              ],
            ),
          ],
        ),
      ),
    );
  }
}
