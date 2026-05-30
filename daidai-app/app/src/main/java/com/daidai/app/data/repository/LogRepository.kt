package com.daidai.app.data.repository

import com.daidai.app.data.remote.ApiService
import com.daidai.app.data.remote.model.*
import javax.inject.Inject

class LogRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getLogs(
        taskId: Int? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<LogListResponse> {
        return try {
            val response = apiService.getLogs(taskId, page, pageSize)
            if (response.isSuccessful && response.body()?.data != null) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("获取日志列表失败"))
            } else {
                Result.failure(Exception("获取日志列表失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLog(id: Int): Result<LogDetailResponse> {
        return try {
            val response = apiService.getLog(id)
            if (response.isSuccessful && response.body() != null) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("获取日志详情失败"))
            } else {
                Result.failure(Exception("获取日志详情失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteLog(id: Int): Result<Unit> {
        return try {
            val response = apiService.deleteLog(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.message ?: "删除日志失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
