package com.daidai.app.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerConfig @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("server_config", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_SERVER_HISTORY = "server_history"
        private const val DEFAULT_SERVER_URL = "http://127.0.0.1:5700"
    }
    
    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()
    
    var username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()
    
    var password: String
        get() = prefs.getString(KEY_PASSWORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PASSWORD, value).apply()
    
    var rememberMe: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER_ME, false)
        set(value) = prefs.edit().putBoolean(KEY_REMEMBER_ME, value).apply()
    
    // 服务器历史记录
    var serverHistory: Set<String>
        get() = prefs.getStringSet(KEY_SERVER_HISTORY, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_SERVER_HISTORY, value).apply()
    
    fun saveLoginInfo(username: String, password: String, rememberMe: Boolean) {
        if (rememberMe) {
            this.username = username
            this.password = password
            this.rememberMe = true
        } else {
            this.username = ""
            this.password = ""
            this.rememberMe = false
        }
    }
    
    fun getSavedLoginInfo(): Triple<String, String, Boolean> {
        return Triple(username, password, rememberMe)
    }
    
    fun clearSavedLoginInfo() {
        username = ""
        password = ""
        rememberMe = false
    }
    
    // 添加服务器到历史记录
    fun addServerToHistory(url: String) {
        val history = serverHistory.toMutableSet()
        history.add(url.trimEnd('/'))
        serverHistory = history
    }
    
    // 从历史记录中删除服务器
    fun removeServerFromHistory(url: String) {
        val history = serverHistory.toMutableSet()
        history.remove(url.trimEnd('/'))
        serverHistory = history
    }
    
    // 获取服务器历史记录列表
    fun getServerHistoryList(): List<String> {
        return serverHistory.toList().sorted()
    }
    
    // 清空历史记录
    fun clearServerHistory() {
        serverHistory = emptySet()
    }
    
    // 获取预设的常用服务器地址
    fun getPresetServers(): List<String> {
        return listOf(
            "http://127.0.0.1:5700",
            "http://127.0.0.1:5701",
            "http://localhost:5700",
            "http://localhost:5701"
        )
    }
}
