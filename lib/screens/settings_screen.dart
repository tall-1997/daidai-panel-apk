import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../main.dart';
import '../services/auth_service.dart';
import '../services/notification_service.dart';
import '../services/root/magisk_helper.dart';
import 'home_screen.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> with RefreshableScreen {
  bool _isRooted = false;
  MagiskModuleInfo? _moduleInfo;
  String _appVersion = '';
  bool _notificationEnabled = false;

  @override
  void initState() {
    super.initState();
    _checkRootStatus();
    _loadAppVersion();
    _loadNotificationSetting();
  }

  @override
  void refresh() {
    _checkRootStatus();
  }

  Future<void> _loadNotificationSetting() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _notificationEnabled = prefs.getBool('notification_enabled') ?? false;
    });
  }

  Future<void> _toggleNotification(bool value) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('notification_enabled', value);
    setState(() {
      _notificationEnabled = value;
    });
    if (value) {
      await NotificationService().showSimpleNotification(
        id: 0,
        title: '通知已开启',
        body: '您将收到任务执行状态的通知',
      );
    }
  }

  Future<void> _loadAppVersion() async {
    final packageInfo = await PackageInfo.fromPlatform();
    if (mounted) {
      setState(() {
        _appVersion = '${packageInfo.version}+${packageInfo.buildNumber}';
      });
    }
  }

  Future<void> _checkRootStatus() async {
    final isRooted = await MagiskHelper.isDaidaiModuleInstalled();
    MagiskModuleInfo? moduleInfo;

    if (isRooted) {
      moduleInfo = await MagiskHelper.getModuleInfo();
    }

    if (mounted) {
      setState(() {
        _isRooted = isRooted;
        _moduleInfo = moduleInfo;
      });
    }
  }

  void _showChangeUsernameDialog() {
    final usernameController = TextEditingController();
    
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('修改用户名'),
        content: TextField(
          controller: usernameController,
          decoration: const InputDecoration(
            labelText: '新用户名',
            border: OutlineInputBorder(),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () async {
              if (usernameController.text.isEmpty) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('请输入新用户名'), backgroundColor: Colors.red),
                );
                return;
              }
              
              try {
                final authService = context.read<AuthService>();
                final result = await authService.apiService.changeUsername(usernameController.text);
                
                if (result['code'] == 0 || result['code'] == 200 || result['success'] == true) {
                  Navigator.pop(context);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('用户名修改成功'), backgroundColor: Colors.green),
                  );
                } else {
                  throw Exception(result['message'] ?? '修改失败');
                }
              } catch (e) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('修改失败: $e'), backgroundColor: Colors.red),
                );
              }
            },
            child: const Text('确认'),
          ),
        ],
      ),
    );
  }

  void _showChangePasswordDialog() {
    final oldPasswordController = TextEditingController();
    final newPasswordController = TextEditingController();
    final confirmPasswordController = TextEditingController();
    
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('修改密码'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: oldPasswordController,
                decoration: const InputDecoration(
                  labelText: '当前密码',
                  border: OutlineInputBorder(),
                ),
                obscureText: true,
              ),
              const SizedBox(height: 16),
              TextField(
                controller: newPasswordController,
                decoration: const InputDecoration(
                  labelText: '新密码',
                  border: OutlineInputBorder(),
                ),
                obscureText: true,
              ),
              const SizedBox(height: 16),
              TextField(
                controller: confirmPasswordController,
                decoration: const InputDecoration(
                  labelText: '确认新密码',
                  border: OutlineInputBorder(),
                ),
                obscureText: true,
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
              if (oldPasswordController.text.isEmpty || 
                  newPasswordController.text.isEmpty ||
                  confirmPasswordController.text.isEmpty) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('请填写所有密码字段'), backgroundColor: Colors.red),
                );
                return;
              }
              
              if (newPasswordController.text != confirmPasswordController.text) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('两次输入的新密码不一致'), backgroundColor: Colors.red),
                );
                return;
              }
              
              try {
                final authService = context.read<AuthService>();
                final result = await authService.apiService.changePassword(
                  oldPasswordController.text,
                  newPasswordController.text,
                );
                
                if (result['code'] == 0 || result['code'] == 200 || result['success'] == true) {
                  Navigator.pop(context);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('密码修改成功'), backgroundColor: Colors.green),
                  );
                } else {
                  throw Exception(result['message'] ?? '修改失败');
                }
              } catch (e) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('修改失败: $e'), backgroundColor: Colors.red),
                );
              }
            },
            child: const Text('确认'),
          ),
        ],
      ),
    );
  }

  Future<void> _exportAppLogs() async {
    try {
      final authService = context.read<AuthService>();
      final result = await authService.apiService.getSystemLogs(page: 1, pageSize: 1000);
      
      if (result['code'] == 0 || result['code'] == 200 || result['success'] == true) {
        final logs = result['data'] ?? result['logs'] ?? [];
        final StringBuffer logBuffer = StringBuffer();
        
        logBuffer.writeln('=== 呆呆面板应用日志 ===');
        logBuffer.writeln('导出时间: ${DateTime.now().toIso8601String()}');
        logBuffer.writeln('用户: ${authService.username}');
        logBuffer.writeln('服务器: ${authService.serverUrl}');
        logBuffer.writeln('========================\n');
        
        for (var log in logs) {
          logBuffer.writeln('[${log['created_at'] ?? ''}] ${log['task_name'] ?? '未知任务'}');
          logBuffer.writeln('状态: ${log['status'] == 0 ? '成功' : log['status'] == 1 ? '失败' : '运行中'}');
          if (log['content'] != null && log['content'].toString().isNotEmpty) {
            logBuffer.writeln('内容: ${log['content']}');
          }
          if (log['error'] != null && log['error'].toString().isNotEmpty) {
            logBuffer.writeln('错误: ${log['error']}');
          }
          logBuffer.writeln('---');
        }
        
        if (mounted) {
          showDialog(
            context: context,
            builder: (context) => AlertDialog(
              title: const Text('导出应用日志'),
              content: SingleChildScrollView(
                child: Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.grey[100],
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: SelectableText(
                    logBuffer.toString(),
                    style: const TextStyle(fontFamily: 'monospace', fontSize: 11),
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text('关闭'),
                ),
                FilledButton.icon(
                  onPressed: () {
                    Navigator.pop(context);
                    Clipboard.setData(ClipboardData(text: logBuffer.toString()));
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('日志已复制到剪贴板'), backgroundColor: Colors.green),
                    );
                  },
                  icon: const Icon(Icons.copy),
                  label: const Text('复制'),
                ),
              ],
            ),
          );
        }
      } else {
        throw Exception(result['message'] ?? '获取日志失败');
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('导出日志失败: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final authService = context.watch<AuthService>();
    final themeProvider = context.watch<ThemeProvider>();

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        // Account switching
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('账户管理', style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                const SizedBox(height: 16),
                ListTile(
                  leading: const Icon(Icons.person),
                  title: const Text('当前用户'),
                  subtitle: Text(authService.username ?? '未登录'),
                ),
                ListTile(
                  leading: const Icon(Icons.dns),
                  title: const Text('服务器地址'),
                  subtitle: Text(authService.serverUrl),
                ),
                const Divider(),
                ListTile(
                  leading: const Icon(Icons.edit),
                  title: const Text('修改用户名'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => _showChangeUsernameDialog(),
                ),
                ListTile(
                  leading: const Icon(Icons.lock),
                  title: const Text('修改密码'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => _showChangePasswordDialog(),
                ),
                if (authService.savedAccounts.length > 1) ...[
                  const Divider(),
                  const Text('切换账户', style: TextStyle(fontWeight: FontWeight.bold)),
                  const SizedBox(height: 8),
                  ...authService.savedAccounts.map((account) => ListTile(
                    dense: true,
                    leading: CircleAvatar(
                      radius: 16,
                      backgroundColor: account.username == authService.username
                          ? Theme.of(context).colorScheme.primary
                          : Colors.grey,
                      child: Text(account.username[0].toUpperCase(),
                        style: TextStyle(color: Theme.of(context).colorScheme.onPrimary, fontSize: 14)),
                    ),
                    title: Text(account.username),
                    subtitle: Text(account.serverUrl, style: const TextStyle(fontSize: 12)),
                    trailing: account.username == authService.username
                        ? Icon(Icons.check, color: Theme.of(context).colorScheme.primary)
                        : null,
                    onTap: account.username == authService.username
                        ? null
                        : () async {
                            await authService.switchAccount(account);
                            if (mounted) {
                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(content: Text('已切换到 ${account.username}')),
                              );
                            }
                          },
                  )),
                ],
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),

        // Root status
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(_isRooted ? Icons.check_circle : Icons.cancel,
                      color: _isRooted ? Colors.green : Colors.orange),
                    const SizedBox(width: 8),
                    Text('Root 状态', style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                  ],
                ),
                const SizedBox(height: 16),
                ListTile(
                  leading: const Icon(Icons.security),
                  title: const Text('Root 权限'),
                  subtitle: Text(_isRooted ? '已获取' : '未获取'),
                  trailing: Icon(_isRooted ? Icons.check : Icons.close,
                    color: _isRooted ? Colors.green : Colors.red),
                ),
                if (_isRooted && _moduleInfo != null) ...[
                  const Divider(),
                  ListTile(
                    leading: const Icon(Icons.extension, color: Colors.purple),
                    title: const Text('Magisk 模块'),
                    subtitle: Text('${_moduleInfo!.name} v${_moduleInfo!.version}'),
                  ),
                  ListTile(
                    leading: const Icon(Icons.person),
                    title: const Text('模块作者'),
                    subtitle: Text(_moduleInfo!.author),
                  ),
                ],
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),

        // App settings
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('应用设置', style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                const SizedBox(height: 16),
                ListTile(
                  leading: const Icon(Icons.dark_mode),
                  title: const Text('深色模式'),
                  subtitle: Text(_getThemeModeText(themeProvider.themeMode)),
                  trailing: DropdownButton<ThemeMode>(
                    value: themeProvider.themeMode,
                    onChanged: (mode) {
                      if (mode != null) {
                        themeProvider.setThemeMode(mode);
                      }
                    },
                    items: const [
                      DropdownMenuItem(
                        value: ThemeMode.system,
                        child: Text('跟随系统'),
                      ),
                      DropdownMenuItem(
                        value: ThemeMode.light,
                        child: Text('浅色模式'),
                      ),
                      DropdownMenuItem(
                        value: ThemeMode.dark,
                        child: Text('深色模式'),
                      ),
                    ],
                  ),
                ),
                SwitchListTile(
                  secondary: const Icon(Icons.notifications_active),
                  title: const Text('App 通知推送'),
                  subtitle: const Text('通过 App 通道接收任务通知'),
                  value: _notificationEnabled,
                  onChanged: _toggleNotification,
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),

        // About
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('关于', style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                const SizedBox(height: 16),
                ListTile(
                  leading: const Icon(Icons.info),
                  title: const Text('版本'),
                  subtitle: Text('v$_appVersion-flutter'),
                ),
                const ListTile(
                  leading: Icon(Icons.code),
                  title: Text('技术栈'),
                  subtitle: Text('Flutter + Dart + Provider'),
                ),
                const ListTile(
                  leading: Icon(Icons.phone_android),
                  title: Text('支持平台'),
                  subtitle: Text('Android, iOS'),
                ),
                const Divider(),
                ListTile(
                  leading: const Icon(Icons.bug_report),
                  title: const Text('导出应用日志'),
                  subtitle: const Text('导出日志用于问题排查'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => _exportAppLogs(),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 24),
        FilledButton.tonal(
          onPressed: () => authService.logout(),
          style: FilledButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 16)),
          child: const Text('退出登录'),
        ),
      ],
    );
  }

  String _getThemeModeText(ThemeMode mode) {
    switch (mode) {
      case ThemeMode.system:
        return '跟随系统设置';
      case ThemeMode.light:
        return '浅色模式';
      case ThemeMode.dark:
        return '深色模式';
    }
  }
}
