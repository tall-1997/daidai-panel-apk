import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'dart:async';
import '../services/auth_service.dart';
import '../services/root/magisk_helper.dart';
import '../theme/miuix_theme.dart';
import '../widgets/miuix_widgets.dart';
import 'home_screen.dart';

class SystemScreen extends StatefulWidget {
  const SystemScreen({super.key});

  @override
  State<SystemScreen> createState() => _SystemScreenState();
}

class _SystemScreenState extends State<SystemScreen> with RefreshableScreen {
  bool _isLoading = true;
  bool _isRooted = false;
  Map<String, dynamic> _systemInfo = {};
  Map<String, dynamic> _dashboardData = {};
  String _error = '';
  
  // Trend data
  final List<double> _cpuHistory = [];
  final List<double> _memoryHistory = [];
  final List<double> _diskHistory = [];
  Timer? _trendTimer;
  static const int _maxHistoryLength = 30;

  @override
  void initState() {
    super.initState();
    _loadSystemInfo();
    _startTrendMonitoring();
  }

  @override
  void dispose() {
    _trendTimer?.cancel();
    super.dispose();
  }

  void _startTrendMonitoring() {
    _trendTimer = Timer.periodic(const Duration(seconds: 5), (timer) {
      if (_systemInfo.isNotEmpty) {
        setState(() {
          _cpuHistory.add(_systemInfo['cpu_usage']?.toDouble() ?? 0);
          _memoryHistory.add(_systemInfo['memory_usage']?.toDouble() ?? 0);
          _diskHistory.add(_systemInfo['disk_usage']?.toDouble() ?? 0);
          
          if (_cpuHistory.length > _maxHistoryLength) {
            _cpuHistory.removeAt(0);
          }
          if (_memoryHistory.length > _maxHistoryLength) {
            _memoryHistory.removeAt(0);
          }
          if (_diskHistory.length > _maxHistoryLength) {
            _diskHistory.removeAt(0);
          }
        });
      }
    });
  }

  @override
  void refresh() {
    _loadSystemInfo();
  }

  Future<void> _loadSystemInfo() async {
    setState(() {
      _isLoading = true;
      _error = '';
    });

    try {
      final authService = context.read<AuthService>();
      
      // Check root status
      _isRooted = await MagiskHelper.isDaidaiModuleInstalled();
      
      // Get system info from API
      try {
        final systemResult = await authService.apiService.getSystemInfo();
        if (systemResult['data'] != null) {
          _systemInfo = systemResult['data'];
        }
      } catch (e) {
        // API might not be available
      }
      
      // Get dashboard data
      try {
        final dashboardResult = await authService.apiService.getDashboard();
        if (dashboardResult['data'] != null) {
          _dashboardData = dashboardResult['data'];
        }
      } catch (e) {
        // API might not be available
      }
      
      // If rooted, get additional info via root
      if (_isRooted) {
        try {
          final rootInfo = await MagiskHelper.getSystemInfoViaRoot();
          if (rootInfo.isNotEmpty) {
            _systemInfo['root_info'] = rootInfo;
          }
        } catch (e) {
          // Root info not available
        }
      }
      
      setState(() {
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
        _error = '加载失败: $e';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('系统信息'),
      ),
      body: _isLoading
          ? const MiuixLoadingState()
          : _error.isNotEmpty
              ? MiuixErrorState(message: _error, onRetry: _loadSystemInfo)
              : SingleChildScrollView(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _buildRootStatusCard(),
                      const SizedBox(height: 16),
                      _buildPanelVersionCard(),
                      const SizedBox(height: 16),
                      _buildDashboardCard(),
                      const SizedBox(height: 16),
                      _buildSystemInfoCard(),
                      if (_systemInfo.containsKey('root_info')) ...[
                        const SizedBox(height: 16),
                        _buildRootSystemInfoCard(),
                      ],
                      if (_cpuHistory.length >= 3) ...[
                        const SizedBox(height: 16),
                        _buildTrendChartCard(),
                      ],
                    ],
                  ),
                ),
    );
  }

  Widget _buildRootStatusCard() {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return MiuixCard(
      child: Row(
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              color: (_isRooted ? Colors.green : Colors.orange).withOpacity(0.15),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(
              _isRooted ? Icons.check_circle : Icons.cancel,
              color: _isRooted ? Colors.green : Colors.orange,
              size: 28,
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Root 权限',
                  style: MiuixTextStyles.headline2.copyWith(
                    fontWeight: FontWeight.w600,
                    color: isDark ? MiuixColors.darkOnSurface : MiuixColors.onSurface,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  _isRooted ? '已获取 Root 权限' : '未获取 Root 权限',
                  style: MiuixTextStyles.footnote1.copyWith(
                    color: _isRooted ? Colors.green : Colors.orange,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPanelVersionCard() {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final version = _systemInfo['version'] ?? _systemInfo['panel_version'] ?? '';

    if (version.isEmpty) return const SizedBox.shrink();

    return MiuixCard(
      child: Row(
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              color: MiuixColors.primary.withOpacity(0.15),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(
              Icons.info_outline,
              color: MiuixColors.primary,
              size: 28,
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '面板版本',
                  style: MiuixTextStyles.headline2.copyWith(
                    fontWeight: FontWeight.w600,
                    color: isDark ? MiuixColors.darkOnSurface : MiuixColors.onSurface,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  version,
                  style: MiuixTextStyles.footnote1.copyWith(
                    color: MiuixColors.primary,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDashboardCard() {
    final taskCount = _dashboardData['task_count'] ?? 0;
    final enabledTasks = _dashboardData['enabled_tasks'] ?? 0;
    final runningTasks = _dashboardData['running_tasks'] ?? 0;
    final todayLogs = _dashboardData['today_logs'] ?? 0;
    final successLogs = _dashboardData['success_logs'] ?? 0;
    final failedLogs = _dashboardData['failed_logs'] ?? 0;
    final envCount = _dashboardData['env_count'] ?? 0;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return MiuixCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.dashboard, color: MiuixColors.primary, size: 22),
              const SizedBox(width: 8),
              Text(
                '面板概览',
                style: MiuixTextStyles.headline2.copyWith(
                  fontWeight: FontWeight.w600,
                  color: isDark ? MiuixColors.darkOnSurface : MiuixColors.onSurface,
                ),
              ),
            ],
          ),
          Divider(color: isDark ? MiuixColors.darkDividerLine : MiuixColors.dividerLine),
          Row(
            children: [
              _buildStatItem('任务总数', '$taskCount', MiuixColors.primary),
              _buildStatItem('已启用', '$enabledTasks', Colors.green),
              _buildStatItem('运行中', '$runningTasks', Colors.orange),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              _buildStatItem('今日日志', '$todayLogs', Colors.purple),
              _buildStatItem('成功', '$successLogs', Colors.green),
              _buildStatItem('失败', '$failedLogs', MiuixColors.error),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              _buildStatItem('环境变量', '$envCount', MiuixColors.primary),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildStatItem(String label, String value, Color color) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.all(12),
        margin: const EdgeInsets.symmetric(horizontal: 4),
        decoration: BoxDecoration(
          color: color.withOpacity(0.1),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Column(
          children: [
            Text(
              value,
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
                color: color,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              label,
              style: TextStyle(
                fontSize: 12,
                color: color,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSystemInfoCard() {
    final hostname = _systemInfo['hostname'] ?? '';
    final cpuUsage = _systemInfo['cpu_usage'] ?? 0.0;
    final memoryTotal = _systemInfo['memory_total'] ?? 0;
    final memoryUsed = _systemInfo['memory_used'] ?? 0;
    final memoryUsage = _systemInfo['memory_usage'] ?? 0.0;
    final diskTotal = _systemInfo['disk_total'] ?? 0;
    final diskUsed = _systemInfo['disk_used'] ?? 0;
    final diskUsage = _systemInfo['disk_usage'] ?? 0.0;
    final uptime = _systemInfo['uptime'] ?? '';
    final goVersion = _systemInfo['go_version'] ?? '';
    final os = _systemInfo['os'] ?? '';
    final arch = _systemInfo['arch'] ?? '';
    final numCpu = _systemInfo['num_cpu'] ?? 0;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.computer, color: Colors.green),
                const SizedBox(width: 8),
                Text(
                  '系统信息',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ],
            ),
            const Divider(),
            if (hostname.isNotEmpty)
              _buildInfoRow('主机名', hostname),
            _buildInfoRow('操作系统', '$os $arch'),
            _buildInfoRow('CPU 核心', '$numCpu 核'),
            _buildInfoRow('CPU 使用率', '${cpuUsage.toStringAsFixed(1)}%'),
            const SizedBox(height: 8),
            _buildProgressBar('CPU', cpuUsage / 100, Colors.blue),
            const SizedBox(height: 12),
            _buildInfoRow('内存', '${_formatBytes(memoryUsed)} / ${_formatBytes(memoryTotal)}'),
            _buildInfoRow('内存使用率', '${memoryUsage.toStringAsFixed(1)}%'),
            const SizedBox(height: 8),
            _buildProgressBar('内存', memoryUsage / 100, Colors.green),
            const SizedBox(height: 12),
            _buildInfoRow('磁盘', '${_formatBytes(diskUsed)} / ${_formatBytes(diskTotal)}'),
            _buildInfoRow('磁盘使用率', '${diskUsage.toStringAsFixed(1)}%'),
            const SizedBox(height: 8),
            _buildProgressBar('磁盘', diskUsage / 100, Colors.orange),
            const SizedBox(height: 12),
            _buildInfoRow('运行时间', uptime),
            _buildInfoRow('Go 版本', goVersion),
          ],
        ),
      ),
    );
  }

  Widget _buildProgressBar(String label, double progress, Color color) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        LinearProgressIndicator(
          value: progress,
          backgroundColor: color.withOpacity(0.2),
          valueColor: AlwaysStoppedAnimation<Color>(color),
          minHeight: 8,
          borderRadius: BorderRadius.circular(4),
        ),
      ],
    );
  }

  Widget _buildRootSystemInfoCard() {
    final rootInfo = _systemInfo['root_info'] as Map<String, dynamic>;
    
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.android, color: Colors.green),
                const SizedBox(width: 8),
                Text(
                  '系统信息 (Root)',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ],
            ),
            const Divider(),
            if (rootInfo.containsKey('memory'))
              _buildInfoRow('内存', rootInfo['memory']),
            if (rootInfo.containsKey('cpu'))
              _buildInfoRow('CPU', rootInfo['cpu']),
            if (rootInfo.containsKey('disk'))
              _buildInfoRow('磁盘', rootInfo['disk']),
            if (rootInfo.containsKey('uptime'))
              _buildInfoRow('运行时间', rootInfo['uptime']),
          ],
        ),
      ),
    );
  }

  Widget _buildTrendChartCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.trending_up, color: Colors.blue),
                const SizedBox(width: 8),
                Text(
                  '资源使用趋势',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ],
            ),
            const Divider(),
            const SizedBox(height: 8),
            _buildTrendChart('CPU', _cpuHistory, Colors.blue),
            const SizedBox(height: 16),
            _buildTrendChart('内存', _memoryHistory, Colors.green),
            const SizedBox(height: 16),
            _buildTrendChart('磁盘', _diskHistory, Colors.orange),
            const SizedBox(height: 8),
            Center(
              child: Text(
                '最近 ${_cpuHistory.length} 次采样 (每5秒)',
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: Colors.grey,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTrendChart(String label, List<double> data, Color color) {
    if (data.isEmpty) return const SizedBox.shrink();
    
    final currentValue = data.last;
    final maxValue = data.reduce((a, b) => a > b ? a : b);
    final minValue = data.reduce((a, b) => a < b ? a : b);
    
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              label,
              style: TextStyle(
                fontWeight: FontWeight.bold,
                color: color,
              ),
            ),
            Text(
              '${currentValue.toStringAsFixed(1)}%',
              style: TextStyle(
                fontWeight: FontWeight.bold,
                color: color,
                fontSize: 16,
              ),
            ),
          ],
        ),
        const SizedBox(height: 4),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              '最小: ${minValue.toStringAsFixed(1)}%',
              style: const TextStyle(fontSize: 12, color: Colors.grey),
            ),
            Text(
              '最大: ${maxValue.toStringAsFixed(1)}%',
              style: const TextStyle(fontSize: 12, color: Colors.grey),
            ),
          ],
        ),
        const SizedBox(height: 8),
        SizedBox(
          height: 60,
          child: CustomPaint(
            size: Size.infinite,
            painter: _TrendChartPainter(
              data: data,
              color: color,
              maxValue: 100,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 100,
            child: Text(
              label,
              style: const TextStyle(
                fontWeight: FontWeight.bold,
                color: Colors.grey,
              ),
            ),
          ),
          Expanded(child: Text(value)),
        ],
      ),
    );
  }

  String _formatBytes(int bytes) {
    if (bytes < 1024) return '$bytes B';
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(1)} KB';
    if (bytes < 1024 * 1024 * 1024) return '${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB';
    return '${(bytes / (1024 * 1024 * 1024)).toStringAsFixed(1)} GB';
  }
}

class _TrendChartPainter extends CustomPainter {
  final List<double> data;
  final Color color;
  final double maxValue;

  _TrendChartPainter({
    required this.data,
    required this.color,
    required this.maxValue,
  });

  @override
  void paint(Canvas canvas, Size size) {
    if (data.length < 2) return;

    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.0
      ..strokeCap = StrokeCap.round;

    final fillPaint = Paint()
      ..color = color.withOpacity(0.1)
      ..style = PaintingStyle.fill;

    final path = Path();
    final fillPath = Path();
    
    final stepX = size.width / (data.length - 1);
    final scaleY = size.height / maxValue;

    // Start fill path
    fillPath.moveTo(0, size.height);

    for (int i = 0; i < data.length; i++) {
      final x = i * stepX;
      final y = size.height - (data[i] * scaleY);
      
      if (i == 0) {
        path.moveTo(x, y);
        fillPath.lineTo(x, y);
      } else {
        path.lineTo(x, y);
        fillPath.lineTo(x, y);
      }
    }

    // Close fill path
    fillPath.lineTo(size.width, size.height);
    fillPath.close();

    // Draw fill
    canvas.drawPath(fillPath, fillPaint);

    // Draw line
    canvas.drawPath(path, paint);

    // Draw dots
    final dotPaint = Paint()
      ..color = color
      ..style = PaintingStyle.fill;

    for (int i = 0; i < data.length; i++) {
      final x = i * stepX;
      final y = size.height - (data[i] * scaleY);
      canvas.drawCircle(Offset(x, y), 3, dotPaint);
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => true;
}
