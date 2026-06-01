import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../services/auth_service.dart';
import 'dart:convert';
import 'dart:async';
import 'home_screen.dart';

class TerminalScreen extends StatefulWidget {
  const TerminalScreen({super.key});

  @override
  State<TerminalScreen> createState() => _TerminalScreenState();
}

class _TerminalScreenState extends State<TerminalScreen> with RefreshableScreen {
  final _commandController = TextEditingController();
  final _scrollController = ScrollController();
  final List<Map<String, dynamic>> _output = [];
  final List<String> _commandHistory = [];
  int _historyIndex = -1;
  bool _isExecuting = false;

  @override
  void initState() {
    super.initState();
  }

  @override
  void dispose() {
    _commandController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  @override
  void refresh() {
    setState(() {
      _output.clear();
    });
  }

  void _scrollToBottom() {
    if (_scrollController.hasClients) {
      _scrollController.animateTo(
        _scrollController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 100),
        curve: Curves.easeOut,
      );
    }
  }

  void _navigateHistory(int direction) {
    if (_commandHistory.isEmpty) return;

    setState(() {
      _historyIndex += direction;
      if (_historyIndex < 0) _historyIndex = 0;
      if (_historyIndex >= _commandHistory.length) {
        _historyIndex = _commandHistory.length - 1;
      }
      _commandController.text = _commandHistory[_historyIndex];
      _commandController.selection = TextSelection.fromPosition(
        TextPosition(offset: _commandController.text.length),
      );
    });
  }

  Future<void> _executeCommand() async {
    final command = _commandController.text.trim();
    if (command.isEmpty) return;

    if (command == 'clear' || command == 'cls') {
      setState(() {
        _output.clear();
        _commandController.clear();
      });
      return;
    }

    if (_commandHistory.isEmpty || _commandHistory.last != command) {
      _commandHistory.add(command);
    }
    _historyIndex = _commandHistory.length;

    setState(() {
      _output.add({
        'command': command,
        'status': 'running',
        'output': '',
        'timestamp': DateTime.now().toString(),
        'startTime': DateTime.now().millisecondsSinceEpoch,
      });
      _isExecuting = true;
      _commandController.clear();
    });

    _scrollToBottom();

    try {
      final authService = context.read<AuthService>();
      final api = authService.apiService;

      final response = await api.post('/system/execute', body: {'command': command});

      if (mounted) {
        final endTime = DateTime.now().millisecondsSinceEpoch;
        final startTime = _output.last['startTime'] as int;
        final duration = endTime - startTime;

        setState(() {
          if (response.statusCode == 200) {
            final data = jsonDecode(response.body);
            final output = data['output'] ?? data['data']?['output'] ?? data['result'] ?? '命令已执行';
            _output.last['output'] = output.toString().trim();
            _output.last['status'] = 'success';
          } else if (response.statusCode == 404) {
            _output.last['output'] = '执行接口不存在 (HTTP 404)\n请检查后端是否支持 /system/execute 接口';
            _output.last['status'] = 'error';
          } else {
            final data = jsonDecode(response.body);
            _output.last['output'] = '执行失败: ${data['message'] ?? 'HTTP ${response.statusCode}'}';
            _output.last['status'] = 'error';
          }
          _output.last['duration'] = duration;
          _isExecuting = false;
        });
        _scrollToBottom();
      }
    } catch (e) {
      if (mounted) {
        final endTime = DateTime.now().millisecondsSinceEpoch;
        final startTime = _output.last['startTime'] as int;
        final duration = endTime - startTime;

        setState(() {
          _output.last['output'] = '执行错误: $e';
          _output.last['status'] = 'error';
          _output.last['duration'] = duration;
          _isExecuting = false;
        });
        _scrollToBottom();
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('在线终端'),
        actions: [
          IconButton(
            icon: const Icon(Icons.delete_sweep),
            onPressed: () => setState(() => _output.clear()),
            tooltip: '清空输出',
          ),
        ],
      ),
      body: Column(
        children: [
          // Output area
          Expanded(
            child: _output.isEmpty
                ? Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.terminal, size: 64, color: Theme.of(context).colorScheme.onSurfaceVariant),
                        const SizedBox(height: 16),
                        const Text('输入命令执行'),
                        const SizedBox(height: 8),
                        Text(
                          '支持 Linux 命令，如 ls, cat, echo 等',
                          style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Colors.grey),
                        ),
                      ],
                    ),
                  )
                : ListView.builder(
                    controller: _scrollController,
                    padding: const EdgeInsets.all(16),
                    itemCount: _output.length,
                    itemBuilder: (context, index) => _buildOutputItem(_output[index]),
                  ),
          ),
          // Input area
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.surface,
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.1),
                  blurRadius: 4,
                  offset: const Offset(0, -2),
                ),
              ],
            ),
            child: Row(
              children: [
                IconButton(
                  icon: const Icon(Icons.arrow_upward),
                  onPressed: () => _navigateHistory(-1),
                  tooltip: '上一条命令',
                ),
                IconButton(
                  icon: const Icon(Icons.arrow_downward),
                  onPressed: () => _navigateHistory(1),
                  tooltip: '下一条命令',
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: TextField(
                    controller: _commandController,
                    decoration: InputDecoration(
                      hintText: '输入命令...',
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                    ),
                    style: const TextStyle(fontFamily: 'monospace'),
                    onSubmitted: (_) => _executeCommand(),
                    textInputAction: TextInputAction.send,
                  ),
                ),
                const SizedBox(width: 8),
                IconButton(
                  icon: _isExecuting
                      ? const SizedBox(
                          width: 20,
                          height: 20,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.send),
                  onPressed: _isExecuting ? null : _executeCommand,
                  tooltip: '执行',
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildOutputItem(Map<String, dynamic> item) {
    final command = item['command'] ?? '';
    final output = item['output'] ?? '';
    final status = item['status'] ?? 'running';
    final duration = item['duration'] ?? 0;

    Color statusColor;
    IconData statusIcon;
    switch (status) {
      case 'success':
        statusColor = Colors.green;
        statusIcon = Icons.check_circle;
        break;
      case 'error':
        statusColor = Colors.red;
        statusIcon = Icons.error;
        break;
      default:
        statusColor = Colors.orange;
        statusIcon = Icons.hourglass_empty;
    }

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Command
            Row(
              children: [
                Icon(Icons.terminal, size: 16, color: Theme.of(context).colorScheme.primary),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    '\$ $command',
                    style: const TextStyle(
                      fontFamily: 'monospace',
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                if (duration > 0)
                  Text(
                    '${duration}ms',
                    style: const TextStyle(fontSize: 11, color: Colors.grey),
                  ),
                const SizedBox(width: 4),
                Icon(statusIcon, size: 16, color: statusColor),
              ],
            ),
            // Output
            if (output.isNotEmpty) ...[
              const SizedBox(height: 8),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: Colors.grey[100],
                  borderRadius: BorderRadius.circular(8),
                ),
                child: SelectableText(
                  output,
                  style: const TextStyle(
                    fontFamily: 'monospace',
                    fontSize: 12,
                  ),
                ),
              ),
            ],
            // Loading indicator
            if (status == 'running') ...[
              const SizedBox(height: 8),
              const LinearProgressIndicator(),
            ],
          ],
        ),
      ),
    );
  }
}
