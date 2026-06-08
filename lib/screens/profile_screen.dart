import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/auth_service.dart';
import '../theme/miuix_theme.dart';
import '../widgets/miuix_widgets.dart';
import 'home_screen.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> with RefreshableScreen {
  Map<String, dynamic>? _user;
  bool _isLoading = true;
  String? _error;
  bool _is2FAEnabled = false;

  @override
  void initState() {
    super.initState();
    _loadProfile();
  }

  @override
  void refresh() {
    _loadProfile();
  }

  Future<void> _loadProfile() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final authService = context.read<AuthService>();
      final results = await Future.wait([
        authService.apiService.getCurrentUser(),
        authService.apiService.get2FAStatus(),
      ]);

      if (mounted) {
        setState(() {
          _user = results[0]['data'] ?? results[0];
          _is2FAEnabled = results[1]['data']?['enabled'] ?? false;
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
              if (newPasswordController.text != confirmPasswordController.text) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('两次密码不一致'), backgroundColor: Colors.red),
                );
                return;
              }

              try {
                final authService = context.read<AuthService>();
                await authService.apiService.changePassword(
                  oldPasswordController.text,
                  newPasswordController.text,
                );
                Navigator.pop(context);
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('密码已修改'), backgroundColor: Colors.green),
                  );
                }
              } catch (e) {
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('修改失败: $e'), backgroundColor: Colors.red),
                  );
                }
              }
            },
            child: const Text('确认'),
          ),
        ],
      ),
    );
  }

  void _showChangeUsernameDialog() {
    final usernameController = TextEditingController(text: _user?['username'] ?? '');

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
                  const SnackBar(content: Text('请输入用户名'), backgroundColor: Colors.red),
                );
                return;
              }

              try {
                final authService = context.read<AuthService>();
                await authService.apiService.changeUsername(usernameController.text);
                Navigator.pop(context);
                _loadProfile();
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('用户名已修改'), backgroundColor: Colors.green),
                  );
                }
              } catch (e) {
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('修改失败: $e'), backgroundColor: Colors.red),
                  );
                }
              }
            },
            child: const Text('确认'),
          ),
        ],
      ),
    );
  }

  void _show2FADialog() {
    if (_is2FAEnabled) {
      _showDisable2FADialog();
    } else {
      _showEnable2FADialog();
    }
  }

  void _showEnable2FADialog() {
    final codeController = TextEditingController();

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('启用双因素认证'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('请使用认证器应用扫描二维码，然后输入验证码：'),
              const SizedBox(height: 16),
              TextField(
                controller: codeController,
                decoration: const InputDecoration(
                  labelText: '验证码',
                  border: OutlineInputBorder(),
                  hintText: '6位数字',
                ),
                keyboardType: TextInputType.number,
                maxLength: 6,
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
                await authService.apiService.verify2FA(codeController.text);
                Navigator.pop(context);
                setState(() => _is2FAEnabled = true);
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('双因素认证已启用'), backgroundColor: Colors.green),
                  );
                }
              } catch (e) {
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('验证失败: $e'), backgroundColor: Colors.red),
                  );
                }
              }
            },
            child: const Text('确认'),
          ),
        ],
      ),
    );
  }

  void _showDisable2FADialog() {
    final codeController = TextEditingController();

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('禁用双因素认证'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('请输入验证码以禁用双因素认证：'),
              const SizedBox(height: 16),
              TextField(
                controller: codeController,
                decoration: const InputDecoration(
                  labelText: '验证码',
                  border: OutlineInputBorder(),
                  hintText: '6位数字',
                ),
                keyboardType: TextInputType.number,
                maxLength: 6,
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
                await authService.apiService.disable2FA(codeController.text);
                Navigator.pop(context);
                setState(() => _is2FAEnabled = false);
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('双因素认证已禁用'), backgroundColor: Colors.green),
                  );
                }
              } catch (e) {
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('验证失败: $e'), backgroundColor: Colors.red),
                  );
                }
              }
            },
            child: const Text('确认'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: const Text('个人设置'),
      ),
      body: _isLoading
          ? const MiuixLoadingState()
          : _error != null
              ? MiuixErrorState(message: _error!, onRetry: _loadProfile)
              : ListView(
                  padding: const EdgeInsets.all(16),
                  children: [
                    _buildProfileCard(isDark),
                    const SizedBox(height: 16),
                    _buildSecurityCard(isDark),
                    const SizedBox(height: 16),
                    _buildSessionCard(isDark),
                  ],
                ),
    );
  }

  Widget _buildProfileCard(bool isDark) {
    final username = _user?['username'] ?? '未知用户';
    final role = _user?['role'] ?? 'user';
    final createdAt = _user?['created_at'] ?? '';

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '基本信息',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: isDark ? MiuixColors.darkOnSurface : MiuixColors.onSurface,
              ),
            ),
            const SizedBox(height: 16),
            ListTile(
              leading: CircleAvatar(
                backgroundColor: MiuixColors.primary,
                child: Text(
                  username[0].toUpperCase(),
                  style: const TextStyle(color: Colors.white, fontSize: 20),
                ),
              ),
              title: Text(
                username,
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: isDark ? MiuixColors.darkOnSurface : MiuixColors.onSurface,
                ),
              ),
              subtitle: Text(
                '角色: ${role == 'admin' ? '管理员' : '普通用户'}',
                style: TextStyle(
                  color: isDark ? MiuixColors.darkOnSurfaceVariantSummary : MiuixColors.onSurfaceVariantSummary,
                ),
              ),
              trailing: TextButton(
                onPressed: _showChangeUsernameDialog,
                child: const Text('修改'),
              ),
            ),
            if (createdAt.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(left: 16, top: 8),
                child: Text(
                  '注册时间: $createdAt',
                  style: TextStyle(
                    fontSize: 12,
                    color: isDark ? MiuixColors.darkOnSurfaceVariantSummary : MiuixColors.onSurfaceVariantSummary,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildSecurityCard(bool isDark) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '安全设置',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: isDark ? MiuixColors.darkOnSurface : MiuixColors.onSurface,
              ),
            ),
            const SizedBox(height: 8),
            ListTile(
              leading: const Icon(Icons.lock),
              title: const Text('修改密码'),
              subtitle: const Text('定期修改密码以保护账户安全'),
              trailing: const Icon(Icons.chevron_right),
              onTap: _showChangePasswordDialog,
            ),
            const Divider(),
            ListTile(
              leading: Icon(
                Icons.security,
                color: _is2FAEnabled ? Colors.green : Colors.grey,
              ),
              title: const Text('双因素认证'),
              subtitle: Text(_is2FAEnabled ? '已启用' : '未启用'),
              trailing: Switch(
                value: _is2FAEnabled,
                onChanged: (value) => _show2FADialog(),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSessionCard(bool isDark) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '会话管理',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: isDark ? MiuixColors.darkOnSurface : MiuixColors.onSurface,
              ),
            ),
            const SizedBox(height: 8),
            ListTile(
              leading: const Icon(Icons.devices),
              title: const Text('登录日志'),
              subtitle: const Text('查看登录历史记录'),
              trailing: const Icon(Icons.chevron_right),
              onTap: () {
                // Navigate to login logs
              },
            ),
            const Divider(),
            ListTile(
              leading: const Icon(Icons.logout),
              title: const Text('退出其他会话'),
              subtitle: const Text('终止其他设备的登录'),
              trailing: const Icon(Icons.chevron_right),
              onTap: () async {
                final confirmed = await showDialog<bool>(
                  context: context,
                  builder: (context) => AlertDialog(
                    title: const Text('确认'),
                    content: const Text('确定要退出其他设备的登录吗？'),
                    actions: [
                      TextButton(
                        onPressed: () => Navigator.pop(context, false),
                        child: const Text('取消'),
                      ),
                      FilledButton(
                        onPressed: () => Navigator.pop(context, true),
                        child: const Text('确认'),
                      ),
                    ],
                  ),
                );

                if (confirmed == true) {
                  try {
                    final authService = context.read<AuthService>();
                    await authService.apiService.revokeOtherSessions();
                    if (mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('其他会话已退出'), backgroundColor: Colors.green),
                      );
                    }
                  } catch (e) {
                    if (mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text('操作失败: $e'), backgroundColor: Colors.red),
                      );
                    }
                  }
                }
              },
            ),
          ],
        ),
      ),
    );
  }
}
