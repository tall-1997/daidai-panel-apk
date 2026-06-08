import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/auth_service.dart';
import '../theme/miuix_theme.dart';
import '../widgets/miuix_widgets.dart';
import 'home_screen.dart';

class SubscriptionsScreen extends StatefulWidget {
  const SubscriptionsScreen({super.key});

  @override
  State<SubscriptionsScreen> createState() => _SubscriptionsScreenState();
}

class _SubscriptionsScreenState extends State<SubscriptionsScreen> with RefreshableScreen {
  List<Map<String, dynamic>> _subscriptions = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadSubscriptions();
  }

  @override
  void refresh() {
    _loadSubscriptions();
  }

  Future<void> _loadSubscriptions() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final authService = context.read<AuthService>();
      final result = await authService.apiService.getScriptSubscriptions();

      if (mounted) {
        setState(() {
          _subscriptions = List<Map<String, dynamic>>.from(result['data'] ?? []);
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = '加载失败: $e';
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _syncSubscription(int id) async {
    try {
      final authService = context.read<AuthService>();
      await authService.apiService.syncScriptSubscription(id);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('同步已开始'), backgroundColor: Colors.green),
        );
        _loadSubscriptions();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('同步失败: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  Future<void> _deleteSubscription(int id, String name) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认删除'),
        content: Text('确定要删除订阅 "$name" 吗？'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('取消')),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            style: FilledButton.styleFrom(backgroundColor: Colors.red),
            child: const Text('删除'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      try {
        final authService = context.read<AuthService>();
        await authService.apiService.deleteScriptSubscription(id);
        _loadSubscriptions();
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('订阅已删除'), backgroundColor: Colors.green),
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

  void _showCreateDialog() {
    _showEditDialog(null);
  }

  void _showEditDialog(Map<String, dynamic>? subscription) {
    final isCreate = subscription == null;
    final nameController = TextEditingController(text: subscription?['name'] ?? '');
    final urlController = TextEditingController(text: subscription?['url'] ?? '');
    final branchController = TextEditingController(text: subscription?['branch'] ?? 'main');
    final scheduleController = TextEditingController(text: subscription?['schedule'] ?? '0 0 * * *');
    final whitelistController = TextEditingController(text: subscription?['whitelist'] ?? '');
    final blacklistController = TextEditingController(text: subscription?['blacklist'] ?? '');
    String type = subscription?['type'] ?? 'git-repo';
    bool enabled = subscription?['enabled'] ?? true;
    bool autoAddTask = subscription?['auto_add_task'] ?? false;
    bool autoDelTask = subscription?['auto_del_task'] ?? false;

    showDialog(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: Text(isCreate ? '创建订阅' : '编辑订阅'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                TextField(
                  controller: nameController,
                  decoration: const InputDecoration(
                    labelText: '订阅名称',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 16),
                DropdownButtonFormField<String>(
                  value: type,
                  decoration: const InputDecoration(
                    labelText: '订阅类型',
                    border: OutlineInputBorder(),
                  ),
                  items: const [
                    DropdownMenuItem(value: 'git-repo', child: Text('Git 仓库')),
                    DropdownMenuItem(value: 'single-file', child: Text('单文件')),
                  ],
                  onChanged: (value) {
                    setDialogState(() => type = value!);
                  },
                ),
                const SizedBox(height: 16),
                TextField(
                  controller: urlController,
                  decoration: const InputDecoration(
                    labelText: '仓库地址',
                    border: OutlineInputBorder(),
                    hintText: 'https://github.com/user/repo',
                  ),
                ),
                const SizedBox(height: 16),
                if (type == 'git-repo') ...[
                  TextField(
                    controller: branchController,
                    decoration: const InputDecoration(
                      labelText: '分支',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 16),
                ],
                TextField(
                  controller: scheduleController,
                  decoration: const InputDecoration(
                    labelText: '定时规则 (Cron)',
                    border: OutlineInputBorder(),
                    helperText: '格式: 分 时 日 月 周',
                  ),
                ),
                const SizedBox(height: 16),
                SwitchListTile(
                  title: const Text('启用'),
                  value: enabled,
                  onChanged: (value) {
                    setDialogState(() => enabled = value);
                  },
                  contentPadding: EdgeInsets.zero,
                ),
                SwitchListTile(
                  title: const Text('自动创建任务'),
                  value: autoAddTask,
                  onChanged: (value) {
                    setDialogState(() => autoAddTask = value);
                  },
                  contentPadding: EdgeInsets.zero,
                ),
                SwitchListTile(
                  title: const Text('自动删除任务'),
                  value: autoDelTask,
                  onChanged: (value) {
                    setDialogState(() => autoDelTask = value);
                  },
                  contentPadding: EdgeInsets.zero,
                ),
                const SizedBox(height: 16),
                TextField(
                  controller: whitelistController,
                  decoration: const InputDecoration(
                    labelText: '白名单（可选）',
                    border: OutlineInputBorder(),
                    helperText: '只同步匹配的文件，多个用逗号分隔',
                  ),
                ),
                const SizedBox(height: 16),
                TextField(
                  controller: blacklistController,
                  decoration: const InputDecoration(
                    labelText: '黑名单（可选）',
                    border: OutlineInputBorder(),
                    helperText: '排除匹配的文件，多个用逗号分隔',
                  ),
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
                  final authService = context.read<AuthService>();
                  final body = <String, dynamic>{
                    'name': nameController.text,
                    'type': type,
                    'url': urlController.text,
                    'branch': branchController.text,
                    'schedule': scheduleController.text,
                    'enabled': enabled,
                    'auto_add_task': autoAddTask,
                    'auto_del_task': autoDelTask,
                    if (whitelistController.text.isNotEmpty) 'whitelist': whitelistController.text,
                    if (blacklistController.text.isNotEmpty) 'blacklist': blacklistController.text,
                  };

                  if (isCreate) {
                    await authService.apiService.addScriptSubscription(body);
                  } else {
                    await authService.apiService.updateScriptSubscription(subscription['id'], body);
                  }

                  Navigator.pop(context);
                  _loadSubscriptions();
                  if (mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: Text(isCreate ? '订阅已创建' : '订阅已更新'),
                        backgroundColor: Colors.green,
                      ),
                    );
                  }
                } catch (e) {
                  if (mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text('操作失败: $e'), backgroundColor: Colors.red),
                    );
                  }
                }
              },
              child: Text(isCreate ? '创建' : '保存'),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: const Text('订阅管理'),
      ),
      body: _isLoading
          ? const MiuixLoadingState()
          : _error != null
              ? MiuixErrorState(message: _error!, onRetry: _loadSubscriptions)
              : _subscriptions.isEmpty
                  ? MiuixEmptyState(
                      icon: Icons.subscriptions,
                      title: '暂无订阅',
                      action: ElevatedButton.icon(
                        onPressed: _showCreateDialog,
                        icon: const Icon(Icons.add),
                        label: const Text('创建订阅'),
                      ),
                    )
                  : RefreshIndicator(
                      onRefresh: _loadSubscriptions,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: _subscriptions.length,
                        itemBuilder: (context, index) => _buildSubscriptionCard(_subscriptions[index], isDark),
                      ),
                    ),
      floatingActionButton: FloatingActionButton(
        onPressed: _showCreateDialog,
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildSubscriptionCard(Map<String, dynamic> subscription, bool isDark) {
    final id = subscription['id'] ?? 0;
    final name = subscription['name'] ?? '未命名';
    final url = subscription['url'] ?? '';
    final type = subscription['type'] ?? 'git-repo';
    final enabled = subscription['enabled'] ?? true;
    final lastPullAt = subscription['last_pull_at'] ?? '';
    final status = subscription['status'] ?? 0;
    final schedule = subscription['schedule'] ?? '';

    Color statusColor;
    String statusText;
    switch (status) {
      case 0:
        statusColor = Colors.green;
        statusText = '正常';
        break;
      case 1:
        statusColor = Colors.red;
        statusText = '失败';
        break;
      case 2:
        statusColor = Colors.orange;
        statusText = '同步中';
        break;
      default:
        statusColor = Colors.grey;
        statusText = '未知';
    }

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: InkWell(
        onTap: () => _showEditDialog(subscription),
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(
                    type == 'git-repo' ? Icons.cloud : Icons.file_present,
                    color: enabled ? MiuixColors.primary : Colors.grey,
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          name,
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                            color: isDark ? MiuixColors.darkOnSurface : MiuixColors.onSurface,
                          ),
                        ),
                        Text(
                          url,
                          style: TextStyle(
                            fontSize: 12,
                            color: isDark ? MiuixColors.darkOnSurfaceVariantSummary : MiuixColors.onSurfaceVariantSummary,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: statusColor.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: statusColor.withOpacity(0.3)),
                    ),
                    child: Text(
                      statusText,
                      style: TextStyle(
                        fontSize: 12,
                        color: statusColor,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  _buildInfoChip(Icons.schedule, schedule, isDark),
                  const SizedBox(width: 8),
                  if (lastPullAt.isNotEmpty)
                    _buildInfoChip(Icons.update, lastPullAt, isDark),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  TextButton.icon(
                    onPressed: () => _syncSubscription(id),
                    icon: const Icon(Icons.sync, size: 18),
                    label: const Text('同步'),
                  ),
                  TextButton.icon(
                    onPressed: () => _showEditDialog(subscription),
                    icon: const Icon(Icons.edit, size: 18),
                    label: const Text('编辑'),
                  ),
                  TextButton.icon(
                    onPressed: () => _deleteSubscription(id, name),
                    icon: const Icon(Icons.delete, size: 18, color: Colors.red),
                    label: const Text('删除', style: TextStyle(color: Colors.red)),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildInfoChip(IconData icon, String text, bool isDark) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: isDark ? MiuixColors.darkSurfaceContainerHighest : Colors.grey[100],
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: isDark ? MiuixColors.darkOnSurfaceVariantSummary : MiuixColors.onSurfaceVariantSummary),
          const SizedBox(width: 4),
          Text(
            text,
            style: TextStyle(
              fontSize: 11,
              color: isDark ? MiuixColors.darkOnSurfaceVariantSummary : MiuixColors.onSurfaceVariantSummary,
            ),
          ),
        ],
      ),
    );
  }
}
