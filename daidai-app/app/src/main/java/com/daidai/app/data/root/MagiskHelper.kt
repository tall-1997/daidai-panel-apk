package com.daidai.app.data.root

import java.io.File

/**
 * Magisk目录访问助手
 * Magisk模块通常位于 /data/adb/modules/ 目录下
 */
object MagiskHelper {
    
    // Magisk模块目录
    private const val MAGISK_MODULES_DIR = "/data/adb/modules"
    private const val MAGISK_DATA_DIR = "/data/adb"
    
    // 呆呆面板模块目录
    private const val DAIDAI_MODULE_DIR = "$MAGISK_MODULES_DIR/daidai-panel"
    private const val DAIDAI_DATA_DIR = "/data/local/daidai"
    
    /**
     * 检查是否安装了呆呆面板Magisk模块
     */
    fun isDaidaiModuleInstalled(): Boolean {
        return if (RootChecker.isRooted()) {
            val result = RootChecker.executeAsRoot("test -d $DAIDAI_MODULE_DIR && echo 'exists'")
            result.isSuccess && result.getOrNull()?.contains("exists") == true
        } else {
            false
        }
    }
    
    /**
     * 获取呆呆面板模块信息
     */
    fun getModuleInfo(): Result<MagiskModuleInfo> {
        if (!RootChecker.isRooted()) {
            return Result.failure(Exception("设备未获取root权限"))
        }
        
        return try {
            val moduleProp = RootChecker.readFileAsRoot("$DAIDAI_MODULE_DIR/module.prop")
            if (moduleProp.isFailure) {
                return Result.failure(Exception("无法读取模块信息"))
            }
            
            val props = moduleProp.getOrNull()?.lines()?.associate { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) parts[0].trim() to parts[1].trim() else "" to ""
            } ?: emptyMap()
            
            Result.success(
                MagiskModuleInfo(
                    id = props["id"] ?: "daidai-panel",
                    name = props["name"] ?: "呆呆面板",
                    version = props["version"] ?: "unknown",
                    versionCode = props["versionCode"]?.toIntOrNull() ?: 0,
                    author = props["author"] ?: "unknown",
                    description = props["description"] ?: ""
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取呆呆面板数据目录
     */
    fun getDaidaiDataDir(): String = DAIDAI_DATA_DIR
    
    /**
     * 获取面板配置文件路径
     */
    fun getPanelConfigPath(): String = "$DAIDAI_DATA_DIR/app/Dumb-Panel/config.json"
    
    /**
     * 获取面板数据库路径
     */
    fun getPanelDbPath(): String = "$DAIDAI_DATA_DIR/app/Dumb-Panel/database.db"
    
    /**
     * 获取面板日志目录
     */
    fun getPanelLogsDir(): String = "$DAIDAI_DATA_DIR/app/Dumb-Panel/logs"
    
    /**
     * 获取面板脚本目录
     */
    fun getPanelScriptsDir(): String = "$DAIDAI_DATA_DIR/app/Dumb-Panel/scripts"
    
    /**
     * 读取面板配置
     */
    fun readPanelConfig(): Result<String> {
        if (!RootChecker.isRooted()) {
            return Result.failure(Exception("设备未获取root权限"))
        }
        return RootChecker.readFileAsRoot(getPanelConfigPath())
    }
    
    /**
     * 获取面板端口配置
     */
    fun getPanelPort(): Result<Int> {
        if (!RootChecker.isRooted()) {
            return Result.failure(Exception("设备未获取root权限"))
        }
        
        val portsConf = RootChecker.readFileAsRoot("/data/adb/daidai-panel/ports.conf")
        return portsConf.mapCatching { content ->
            val portLine = content.lines().find { it.startsWith("PANEL_PORT=") }
            portLine?.split("=")?.get(1)?.trim()?.toIntOrNull() ?: 5700
        }
    }
    
    /**
     * 获取所有已安装的Magisk模块
     */
    fun getInstalledModules(): Result<List<MagiskModuleInfo>> {
        if (!RootChecker.isRooted()) {
            return Result.failure(Exception("设备未获取root权限"))
        }
        
        return try {
            val modulesDir = RootChecker.listDirectoryAsRoot(MAGISK_MODULES_DIR)
            if (modulesDir.isFailure) {
                return Result.failure(Exception("无法列出模块目录"))
            }
            
            val modules = modulesDir.getOrNull()
                ?.filter { !it.startsWith(".") && !it.startsWith("total") }
                ?.mapNotNull { line ->
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size >= 9) {
                        val moduleName = parts.last()
                        val moduleProp = RootChecker.readFileAsRoot("$MAGISK_MODULES_DIR/$moduleName/module.prop")
                        if (moduleProp.isSuccess) {
                            val props = moduleProp.getOrNull()?.lines()?.associate { l ->
                                val p = l.split("=", limit = 2)
                                if (p.size == 2) p[0].trim() to p[1].trim() else "" to ""
                            } ?: emptyMap()
                            
                            MagiskModuleInfo(
                                id = props["id"] ?: moduleName,
                                name = props["name"] ?: moduleName,
                                version = props["version"] ?: "unknown",
                                versionCode = props["versionCode"]?.toIntOrNull() ?: 0,
                                author = props["author"] ?: "unknown",
                                description = props["description"] ?: ""
                            )
                        } else null
                    } else null
                } ?: emptyList()
            
            Result.success(modules)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Magisk模块信息
 */
data class MagiskModuleInfo(
    val id: String,
    val name: String,
    val version: String,
    val versionCode: Int,
    val author: String,
    val description: String
)
