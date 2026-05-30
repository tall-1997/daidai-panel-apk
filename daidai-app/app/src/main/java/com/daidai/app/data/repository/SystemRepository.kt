package com.daidai.app.data.repository

import com.daidai.app.data.remote.ApiService
import com.daidai.app.data.remote.model.*
import com.daidai.app.data.root.MagiskHelper
import com.daidai.app.data.root.MagiskModuleInfo
import com.daidai.app.data.root.RootChecker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemRepository @Inject constructor(
    private val apiService: ApiService
) {
    /**
     * 检查是否有root权限
     */
    fun hasRootAccess(): Boolean = RootChecker.isRooted()
    
    /**
     * 检查是否安装了呆呆面板Magisk模块
     */
    fun isDaidaiModuleInstalled(): Boolean = MagiskHelper.isDaidaiModuleInstalled()
    
    /**
     * 获取Magisk模块信息
     */
    fun getMagiskModuleInfo(): Result<MagiskModuleInfo> = MagiskHelper.getModuleInfo()
    
    /**
     * 获取系统信息 - 优先使用root获取本地信息，失败时使用API
     */
    suspend fun getSystemInfo(): Result<SystemInfo> {
        // 如果有root权限，尝试获取本地系统信息
        if (hasRootAccess()) {
            val localInfo = getSystemInfoFromRoot()
            if (localInfo.isSuccess) {
                return localInfo
            }
        }
        
        // 回退到API
        return try {
            val response = apiService.getSystemInfo()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception("获取系统信息失败"))
                }
            } else {
                Result.failure(Exception("获取系统信息失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 通过root获取本地系统信息
     */
    private fun getSystemInfoFromRoot(): Result<SystemInfo> {
        return try {
            val hostname = RootChecker.executeAsRoot("getprop net.hostname").getOrNull() ?: "unknown"
            val androidVersion = RootChecker.executeAsRoot("getprop ro.build.version.release").getOrNull() ?: "unknown"
            val sdkVersion = RootChecker.executeAsRoot("getprop ro.build.version.sdk").getOrNull() ?: "unknown"
            val device = RootChecker.executeAsRoot("getprop ro.product.model").getOrNull() ?: "unknown"
            val manufacturer = RootChecker.executeAsRoot("getprop ro.product.manufacturer").getOrNull() ?: "unknown"
            val kernelVersion = RootChecker.executeAsRoot("uname -r").getOrNull() ?: "unknown"
            
            // 获取内存信息
            val memInfo = getMemoryInfoFromRoot()
            val diskInfo = getDiskInfoFromRoot()
            
            Result.success(
                SystemInfo(
                    hostname = hostname,
                    machineCode = null,
                    cpuUsage = getCpuUsageFromRoot(),
                    memoryTotal = memInfo.first,
                    memoryUsed = memInfo.second,
                    memoryFree = memInfo.third,
                    memoryUsage = if (memInfo.first > 0) (memInfo.second.toDouble() / memInfo.first * 100) else 0.0,
                    diskTotal = diskInfo.first,
                    diskUsed = diskInfo.second,
                    diskFree = diskInfo.third,
                    diskUsage = if (diskInfo.first > 0) (diskInfo.second.toDouble() / diskInfo.first * 100) else 0.0,
                    uptime = RootChecker.executeAsRoot("uptime -p").getOrNull(),
                    goroutines = null,
                    goVersion = null,
                    os = "Android $androidVersion",
                    arch = System.getProperty("os.arch") ?: "unknown",
                    numCpu = Runtime.getRuntime().availableProcessors(),
                    dataDir = MagiskHelper.getDaidaiDataDir(),
                    netRxBytes = null,
                    netTxBytes = null,
                    netRxSpeed = null,
                    netTxSpeed = null
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun getCpuUsageFromRoot(): Double {
        val result = RootChecker.executeAsRoot("top -bn1 | grep 'CPU' | head -1")
        return result.getOrNull()?.let { output ->
            val idleMatch = Regex("idle\\s+(\\d+\\.?\\d*)%").find(output)
            val idle = idleMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            100.0 - idle
        } ?: 0.0
    }
    
    private fun getMemoryInfoFromRoot(): Triple<Long, Long, Long> {
        val result = RootChecker.executeAsRoot("cat /proc/meminfo")
        return result.getOrNull()?.let { output ->
            val totalMatch = Regex("MemTotal:\\s+(\\d+)").find(output)
            val freeMatch = Regex("MemFree:\\s+(\\d+)").find(output)
            val availableMatch = Regex("MemAvailable:\\s+(\\d+)").find(output)
            
            val total = totalMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            val free = freeMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            val available = availableMatch?.groupValues?.get(1)?.toLongOrNull() ?: free
            val used = total - available
            
            Triple(total * 1024, used * 1024, free * 1024) // 转换为字节
        } ?: Triple(0L, 0L, 0L)
    }
    
    private fun getDiskInfoFromRoot(): Triple<Long, Long, Long> {
        val result = RootChecker.executeAsRoot("df /data | tail -1")
        return result.getOrNull()?.let { output ->
            val parts = output.split("\\s+".toRegex())
            if (parts.size >= 4) {
                val total = parts[1].toLongOrNull() ?: 0L
                val used = parts[2].toLongOrNull() ?: 0L
                val free = parts[3].toLongOrNull() ?: 0L
                Triple(total * 1024, used * 1024, free * 1024) // 转换为字节
            } else Triple(0L, 0L, 0L)
        } ?: Triple(0L, 0L, 0L)
    }

    suspend fun getDashboard(): Result<DashboardData> {
        return try {
            val response = apiService.getDashboard()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception("获取仪表盘数据失败"))
                }
            } else {
                Result.failure(Exception("获取仪表盘数据失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStats(): Result<StatsData> {
        return try {
            val response = apiService.getStats()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception("获取统计数据失败"))
                }
            } else {
                Result.failure(Exception("获取统计数据失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVersion(): Result<VersionData> {
        return try {
            val response = apiService.getVersion()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception("获取版本信息失败"))
                }
            } else {
                Result.failure(Exception("获取版本信息失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkUpdate(): Result<CheckUpdateData> {
        return try {
            val response = apiService.checkUpdate()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception("检查更新失败"))
                }
            } else {
                Result.failure(Exception("检查更新失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHealthCheck(): Result<HealthCheckResponse> {
        return try {
            val response = apiService.getHealthCheck()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("健康检查失败"))
                }
            } else {
                Result.failure(Exception("健康检查失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun doHealthCheck(): Result<HealthCheckResponse> {
        return try {
            val response = apiService.runHealthCheck()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("执行健康检查失败"))
                }
            } else {
                Result.failure(Exception("执行健康检查失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMachineCode(): Result<String> {
        return try {
            val response = apiService.getMachineCode()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.data?.machineCode != null) {
                    Result.success(body.data.machineCode)
                } else {
                    Result.failure(Exception("获取机器码失败"))
                }
            } else {
                Result.failure(Exception("获取机器码失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPanelLog(): Result<PanelLogResponse> {
        // 如果有root权限，尝试直接读取日志文件
        if (hasRootAccess()) {
            val localLog = getPanelLogFromRoot()
            if (localLog.isSuccess) {
                return localLog
            }
        }
        
        return try {
            val response = apiService.getPanelLog()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("获取面板日志失败"))
                }
            } else {
                Result.failure(Exception("获取面板日志失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 通过root读取面板日志
     */
    private fun getPanelLogFromRoot(): Result<PanelLogResponse> {
        return try {
            val logDir = MagiskHelper.getPanelLogsDir()
            val logFiles = RootChecker.listDirectoryAsRoot(logDir)
            if (logFiles.isFailure) {
                return Result.failure(Exception("无法访问日志目录"))
            }
            
            // 读取最新的日志文件
            val latestLog = logFiles.getOrNull()
                ?.filter { it.endsWith(".log") }
                ?.maxByOrNull { it }
            
            if (latestLog != null) {
                val logContent = RootChecker.readFileAsRoot("$logDir/$latestLog")
                if (logContent.isSuccess) {
                    val lines = logContent.getOrNull()?.lines() ?: emptyList()
                    val logData = PanelLogData(logs = lines, total = lines.size, level = "info")
                    return Result.success(PanelLogResponse(data = logData))
                }
            }
            
            Result.failure(Exception("无法读取日志文件"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createBackup(): Result<BackupData> {
        return try {
            val response = apiService.createBackup()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception("创建备份失败"))
                }
            } else {
                Result.failure(Exception("创建备份失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBackups(): Result<List<BackupFile>> {
        return try {
            val response = apiService.getBackupList()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception("获取备份列表失败"))
                }
            } else {
                Result.failure(Exception("获取备份列表失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBackup(filename: String): Result<Unit> {
        return try {
            val response = apiService.deleteBackup(DeleteBackupRequest(filename))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("删除备份失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(filename: String): Result<Unit> {
        return try {
            val response = apiService.restoreBackup(RestoreBackupRequest(filename))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("恢复备份失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePanel(): Result<Unit> {
        return try {
            val response = apiService.updatePanel()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("更新面板失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restartPanel(): Result<Unit> {
        return try {
            val response = apiService.restartPanel()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("重启面板失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConfigScript(): Result<ConfigScriptData> {
        return try {
            val response = apiService.getConfigScript()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception("获取配置脚本失败"))
                }
            } else {
                Result.failure(Exception("获取配置脚本失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateConfigScript(script: String): Result<Unit> {
        return try {
            val response = apiService.saveConfigScript(SaveConfigScriptRequest(script))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("更新配置脚本失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
