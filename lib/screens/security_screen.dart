import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../services/auth_service.dart';
import '../theme/miuix_theme.dart';
import '../widgets/miuix_widgets.dart';
import 'home_screen.dart';

class SecurityScreen extends StatefulWidget {
  const SecurityScreen({super.key});

  @override
  State<SecurityScreen> createState() => _SecurityScreenState();
}

class _SecurityScreenState extends State<SecurityScreen> with RefreshableScreen {
  bool _isLoading = true;
  String? _error;
  
  // Login logs
  List<Map<String, dynamic>> _loginLogs = [];
  int _loginLogsPage = 1;
  int _loginLogsTotal = 0;
  
  // Sessions
  List<Map<String, dynamic>> _sessions = [];
  
  // IP Whitelist
  List<Map<String, dynamic>> _ipWhitelist = [];
  
  // 2FA status
  bool _is2FAEnabled = false;
  
  // Current tab
  int _currentTab = 0;

  @override
  void initState() {
    super.initState();
    _loadAllData();
  }

  @override
  void refresh() {
    _loadAllData();
  }

  Future<void> _loadAllData() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      await Future.wait([
        _loadLoginLogs(),
        _loadSessions(),
        _loadIPWhitelist(),
        _load2FAStatus(),
      ]);
      setState(() => _isLoading = false);
    } catch (e) {
      setState(() {
        _error = '加载失败: $e';
        _isLoading = false;
      });
    }
  }

  Future<void> _loadLoginLogs() async {
    try {
      final authService = context.read<AuthService>();
      final result = await authService.apiService.getLoginLogs(page: _loginLogsPage, pageSize: 20);
      if (mounted) {
        setState(() {
          _loginLogs = List<Map<String, dynamic>>.from(result['data'] ?? []);
          _loginLogsTotal = result['total'] ?? 0;
        });
      }
    } catch (e) {
      // ignore
    }
  }

  Future<void> _loadSessions() async {
    try {
      final authService = context.read<AuthService>();
      final result = await authService.apiService.getSessions();
      if (mounted) {
        setState(() {
          _sessions = List<Map<String, dynamic>>.from(result['data'] ?? []);
        });
      }
    } catch (e) {
      // ignore
    }
  }

  Future<void> _loadIPWhitelist() async {
    try {
      final authService = context.read<AuthService>();
      final result = await authService.apiService.getIPWhitelist();
      if (mounted) {
        setState(() {
          _ipWhitelist = List<Map<String, dynamic>>.from(result['data'] ?? []);
        });
      }
    } catch (e) {
      // ignore
    }
  }

  Future<void> _load2FAStatus() async {
    try {
      final authService = context.read<AuthService>();
      final result = await authService.apiService.get2FAStatus();
      if (mounted) {
        setState(() {
          _is2FAEnabled = result['data']?['enabled'] ?? false;
        });
      }
    } catch (e) {
      // ignore
    }
  }

  Future<void> _revokeSession(int id) async {
    try {
      final authService = context.read<AuthService>();
      await authService.apiService.revokeSession(id);
      _loadSessions();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('会话已撤销'), backgroundColor: Colors.green),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('撤销失败: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  Future<void> _revokeOtherSessions() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('撤销其他会话'),
        content: const Text('确定要撤销所有其他设备的会话吗？'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('取消')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('确定')),
        ],
      ),
    );

    if (confirmed == true) {
      try {
        final authService = context.read<AuthService>();
        await authService.apiService.revokeOtherSessions();
        _loadSessions();
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('其他会话已撤销'), backgroundColor: Colors.green),
          );
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('撤销失败: $e'), backgroundColor: Colors.red),
          );
        }
      }
    }
  }

  Future<void> _addIPWhitelist() async {
    final ipController = TextEditingController();
    final remarksController = TextEditingController();

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('添加 IP 白名单'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: ipController,
              decoration: const InputDecoration(
                labelText: 'IP 地址',
                hintText: '192.168.1.0/24',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: remarksController,
              decoration: const InputDecoration(
                labelText: '备注（可选）',
                border: OutlineInputBorder(),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('取消')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('添加')),
        ],
      ),
    );

    if (confirmed == true && ipController.text.isNotEmpty) {
      try {
        final authService = context.read<AuthService>();
        await authService.apiService.addIPWhitelist(
          ipController.text.trim(),
          remarks: remarksController.text.trim(),
        );
        _loadIPWhitelist();
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('IP 已添加'), backgroundColor: Colors.green),
          );
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('添加失败: $e'), backgroundColor: Colors.red),
          );
        }
      }
    }
  }

  Future<void> _removeIPWhitelist(int id) async {
    try {
      final authService = context.read<AuthService>();
      await authService.apiService.removeIPWhitelist(id);
      _loadIPWhitelist();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('IP 已删除'), backgroundColor: Colors.green),
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

  Future<void> _setup2FA() async {
    try {
      final authService = context.read<AuthService>();
      final result = await authService.apiService.setup2FA();
      final secret = result['data']?['secret'] ?? '';
      final uri = result['data']?['uri'] ?? '';

      if (mounted) {
        showDialog(
          context: context,
          builder: (context) => AlertDialog(
            title: const Text('设置两步验证'),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text('请使用身份验证器 App 扫描以下信息：'),
                const SizedBox(height: 16),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.grey[100],
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: SelectableText(
                    '密钥: $secret',
                    style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
                  ),
                ),
                const SizedBox(height: 16),
                const Text('或手动复制密钥到验证器 App'),
              ],
            ),
            actions: [
              TextButton(
                onPressed: () {
                  Clipboard.setData(ClipboardData(text: secret));
                  Navigator.pop(context);
                },
                child: const Text('复制密钥'),
              ),
              FilledButton(
                onPressed: () {
                  Navigator.pop(context);
                  _verify2FA();
                },
                child: const Text('下一步'),
              ),
            ],
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('设置失败: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  Future<void> _verify2FA() async {
    final codeController = TextEditingController();

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('验证两步验证码'),
        content: TextField(
          controller: codeController,
          decoration: const InputDecoration(
            labelText: '验证码',
            hintText: '6位数字',
            border: OutlineInputBorder(),
          ),
          keyboardType: TextInputType.number,
          inputFormatters: [
            FilteringTextInputFormatter.digitsOnly,
            LengthLimitingTextInputFormatter(6),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('取消')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('验证')),
        ],
      ),
    );

    if (confirmed == true && codeController.text.isNotEmpty) {
      try {
        final authService = context.read<AuthService>();
        await authService.apiService.verify2FA(codeController.text);
        _load2FAStatus();
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('两步验证已启用'), backgroundColor: Colors.green),
          );
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('验证失败: $e'), backgroundColor: Colors.red),
          );
        }
      }
    }
  }

  Future<void> _disable2FA() async {
    final codeController = TextEditingController();

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('禁用两步验证'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('请输入当前的动态验证码以禁用两步验证'),
            const SizedBox(height: 16),
            TextField(
              controller: codeController,
              decoration: const InputDecoration(
                labelText: '验证码',
                hintText: '6位数字',
                border: OutlineInputBorder(),
              ),
              keyboardType: TextInputType.number,
              inputFormatters: [
                FilteringTextInputFormatter.digitsOnly,
                LengthLimitingTextInputFormatter(6),
              ],
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('取消')),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            style: FilledButton.styleFrom(backgroundColor: Colors.red),
            child: const Text('禁用'),
          ),
        ],
      ),
    );

    if (confirmed == true && codeController.text.isNotEmpty) {
      try {
        final authService = context.read<AuthService>();
        await authService.apiService.disable2FA(codeController.text);
        _load2FAStatus();
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('两步验证已禁用'), backgroundColor: Colors.green),
          );
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('禁用失败: $e'), backgroundColor: Colors.red),
          );
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 4,
      child: Column(
        children: [
          Container(
            decoration: BoxDecoration(
              color: Theme.of(context).scaffoldBackgroundColor,
              border: Border(
                bottom: BorderSide(color: Theme.of(context).dividerColor),
              ),
            ),
            child: TabBar(
              tabs: const [
                Tab(text: '登录日志'),
                Tab(text: '会话管理'),
                Tab(text: 'IP 白名单'),
                Tab(text: '两步验证'),
              ],
              onTap: (index) => setState(() => _currentTab = index),
            ),
          ),
          Expanded(
            child: _isLoading
                ? const MiuixLoadingState()
                : _error != null
                    ? MiuixErrorState(message: _error!, onRetry: _loadAllData)
                    : _buildCurrentTab(),
          ),
        ],
      ),
    );
  }

  Widget _buildCurrentTab() {
    switch (_currentTab) {
      case 0:
        return _buildLoginLogsTab();
      case 1:
        return _buildSessionsTab();
      case 2:
        return _buildIPWhitelistTab();
      case 3:
        return _build2FATab();
      default:
        return const SizedBox.shrink();
    }
  }

  Widget _buildLoginLogsTab() {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Text('共 $_loginLogsTotal 条记录', style: MiuixTextStyles.body2),
              const Spacer(),
              TextButton.icon(
                onPressed: () async {
                  final confirmed = await showDialog<bool>(
                    context: context,
                    builder: (context) => AlertDialog(
                      title: const Text('清除登录日志'),
                      content: const Text('确定要清除所有登录日志吗？'),
                      actions: [
                        TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('取消')),
                        FilledButton(
                          onPressed: () => Navigator.pop(context, true),
                          style: FilledButton.styleFrom(backgroundColor: Colors.red),
                          child: const Text('清除'),
                        ),
                      ],
                    ),
                  );
                  if (confirmed == true) {
                    try {
                      final authService = context.read<AuthService>();
                      await authService.apiService.clearLoginLogs();
                      _loadLoginLogs();
                      if (mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('日志已清除'), backgroundColor: Colors.green),
                        );
                      }
                    } catch (e) {
                      if (mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text('清除失败: $e'), backgroundColor: Colors.red),
                        );
                      }
                    }
                  }
                },
                icon: const Icon(Icons.delete_sweep, size: 18),
                label: const Text('清除日志'),
              ),
            ],
          ),
        ),
        Expanded(
          child: _loginLogs.isEmpty
              ? const MiuixEmptyState(icon: Icons.history, title: '暂无登录日志')
              : ListView.builder(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  itemCount: _loginLogs.length,
                  itemBuilder: (context, index) {
                    final log = _loginLogs[index];
                    final isSuccess = log['success'] ?? true;
                    return Card(
                      margin: const EdgeInsets.only(bottom: 8),
                      child: ListTile(
                        leading: Icon(
                          isSuccess ? Icons.check_circle : Icons.cancel,
                          color: isSuccess ? Colors.green : Colors.red,
                        ),
                        title: Text(log['username'] ?? '未知用户'),
                        subtitle: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('IP: ${log['ip'] ?? '未知'}'),
                            Text('时间: ${log['created_at'] ?? ''}'),
                            if (log['user_agent'] != null)
                              Text(
                                '设备: ${log['user_agent']}',
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(fontSize: 12),
                              ),
                          ],
                        ),
                        isThreeLine: true,
                      ),
                    );
                  },
                ),
        ),
        if (_loginLogsTotal > 20)
          Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                TextButton(
                  onPressed: _loginLogsPage > 1
                      ? () {
                          setState(() => _loginLogsPage--);
                          _loadLoginLogs();
                        }
                      : null,
                  child: const Text('上一页'),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  child: Text('$_loginLogsPage / ${(_loginLogsTotal / 20).ceil()}'),
                ),
                TextButton(
                  onPressed: _loginLogsPage < (_loginLogsTotal / 20).ceil()
                      ? () {
                          setState(() => _loginLogsPage++);
                          _loadLoginLogs();
                        }
                      : null,
                  child: const Text('下一页'),
                ),
              ],
            ),
          ),
      ],
    );
  }

  Widget _buildSessionsTab() {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Text('共 ${_sessions.length} 个会话', style: MiuixTextStyles.body2),
              const Spacer(),
              TextButton.icon(
                onPressed: _revokeOtherSessions,
                icon: const Icon(Icons.logout, size: 18),
                label: const Text('撤销其他会话'),
              ),
            ],
          ),
        ),
        Expanded(
          child: _sessions.isEmpty
              ? const MiuixEmptyState(icon: Icons.devices, title: '暂无活跃会话')
              : ListView.builder(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  itemCount: _sessions.length,
                  itemBuilder: (context, index) {
                    final session = _sessions[index];
                    return Card(
                      margin: const EdgeInsets.only(bottom: 8),
                      child: ListTile(
                        leading: Icon(
                          _getSessionIcon(session['client_type'] ?? ''),
                          color: MiuixColors.primary,
                        ),
                        title: Text(session['client_name'] ?? session['client_type_label'] ?? '未知设备'),
                        subtitle: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('IP: ${session['ip'] ?? '未知'}'),
                            Text('创建: ${session['created_at'] ?? ''}'),
                            Text('过期: ${session['expires_at'] ?? ''}'),
                          ],
                        ),
                        trailing: IconButton(
                          icon: const Icon(Icons.close, color: Colors.red),
                          onPressed: () => _revokeSession(session['id']),
                        ),
                        isThreeLine: true,
                      ),
                    );
                  },
                ),
        ),
      ],
    );
  }

  IconData _getSessionIcon(String clientType) {
    switch (clientType) {
      case 'web':
        return Icons.web;
      case 'android':
        return Icons.phone_android;
      case 'ios':
        return Icons.phone_iphone;
      case 'desktop':
        return Icons.desktop_windows;
      default:
        return Icons.device_unknown;
    }
  }

  Widget _buildIPWhitelistTab() {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Text('共 ${_ipWhitelist.length} 条规则', style: MiuixTextStyles.body2),
              const Spacer(),
              FilledButton.icon(
                onPressed: _addIPWhitelist,
                icon: const Icon(Icons.add, size: 18),
                label: const Text('添加 IP'),
              ),
            ],
          ),
        ),
        Expanded(
          child: _ipWhitelist.isEmpty
              ? const MiuixEmptyState(
                  icon: Icons.security,
                  title: '暂无 IP 白名单',
                  subtitle: '添加 IP 地址或网段以限制访问',
                )
              : ListView.builder(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  itemCount: _ipWhitelist.length,
                  itemBuilder: (context, index) {
                    final entry = _ipWhitelist[index];
                    return Card(
                      margin: const EdgeInsets.only(bottom: 8),
                      child: ListTile(
                        leading: const Icon(Icons.language, color: MiuixColors.primary),
                        title: Text(entry['ip'] ?? ''),
                        subtitle: Text(entry['remarks'] ?? '无备注'),
                        trailing: IconButton(
                          icon: const Icon(Icons.delete, color: Colors.red),
                          onPressed: () => _removeIPWhitelist(entry['id']),
                        ),
                      ),
                    );
                  },
                ),
        ),
      ],
    );
  }

  Widget _build2FATab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(
                        _is2FAEnabled ? Icons.security : Icons.security_outlined,
                        color: _is2FAEnabled ? Colors.green : Colors.grey,
                        size: 32,
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '两步验证 (2FA)',
                              style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                            ),
                            Text(
                              _is2FAEnabled ? '已启用' : '未启用',
                              style: TextStyle(
                                color: _is2FAEnabled ? Colors.green : Colors.grey,
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  const Text(
                    '两步验证为您的账户提供额外的安全保护。启用后，登录时需要输入身份验证器 App 生成的动态验证码。',
                    style: TextStyle(color: Colors.grey),
                  ),
                  const SizedBox(height: 16),
                  if (_is2FAEnabled)
                    SizedBox(
                      width: double.infinity,
                      child: OutlinedButton.icon(
                        onPressed: _disable2FA,
                        icon: const Icon(Icons.lock_open, color: Colors.red),
                        label: const Text('禁用两步验证', style: TextStyle(color: Colors.red)),
                        style: OutlinedButton.styleFrom(
                          side: const BorderSide(color: Colors.red),
                        ),
                      ),
                    )
                  else
                    SizedBox(
                      width: double.infinity,
                      child: FilledButton.icon(
                        onPressed: _setup2FA,
                        icon: const Icon(Icons.lock),
                        label: const Text('启用两步验证'),
                      ),
                    ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('支持的身份验证器', style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold)),
                  const SizedBox(height: 8),
                  const ListTile(
                    leading: Icon(Icons.phone_android),
                    title: Text('Google Authenticator'),
                    dense: true,
                  ),
                  const ListTile(
                    leading: Icon(Icons.phone_iphone),
                    title: Text('Microsoft Authenticator'),
                    dense: true,
                  ),
                  const ListTile(
                    leading: Icon(Icons.shield),
                    title: Text('Authy'),
                    dense: true,
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
