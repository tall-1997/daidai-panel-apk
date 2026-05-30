package com.daidai.app.data.root

import java.io.File

object RootChecker {
    
    private var isRootChecked = false
    private var isRootAvailable = false
    
    /**
     * 检查设备是否有root权限
     */
    fun isRooted(): Boolean {
        if (isRootChecked) return isRootAvailable
        
        isRootChecked = true
        isRootAvailable = checkRootAccess()
        return isRootAvailable
    }
    
    private fun checkRootAccess(): Boolean {
        return try {
            // 检查su命令是否存在
            val suPaths = listOf(
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su",
                "/system/app/Superuser.apk",
                "/system/app/SuperSU.apk",
                "/system/app/SuperSU/SuperSU.apk"
            )
            
            val suExists = suPaths.any { File(it).exists() }
            if (suExists) {
                // 尝试执行su命令验证
                try {
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                    val result = process.inputStream.bufferedReader().readText()
                    process.waitFor()
                    result.contains("uid=0")
                } catch (e: Exception) {
                    suExists
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 以root权限执行命令
     */
    fun executeAsRoot(command: String): Result<String> {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                Result.success(output.trim())
            } else {
                Result.failure(Exception("Root command failed: $error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 读取root权限下的文件内容
     */
    fun readFileAsRoot(path: String): Result<String> {
        return executeAsRoot("cat $path")
    }
    
    /**
     * 列出root权限下的目录内容
     */
    fun listDirectoryAsRoot(path: String): Result<List<String>> {
        return executeAsRoot("ls -la $path").map { output ->
            output.lines().filter { it.isNotBlank() }
        }
    }
}
