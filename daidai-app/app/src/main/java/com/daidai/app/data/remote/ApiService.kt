package com.daidai.app.data.remote

import com.daidai.app.data.remote.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // ==================== Auth ====================
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<BaseResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<LoginResponse>

    @GET("api/v1/auth/user")
    suspend fun getCurrentUser(): Response<UserResponse>

    @PUT("api/v1/auth/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<BaseResponse>

    @PUT("api/v1/auth/username")
    suspend fun changeUsername(@Body request: ChangeUsernameRequest): Response<BaseResponse>

    @GET("api/v1/auth/captcha-config")
    suspend fun getCaptchaConfig(): Response<CaptchaConfigResponse>

    @Multipart
    @POST("api/v1/auth/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): Response<AvatarResponse>

    @DELETE("api/v1/auth/avatar")
    suspend fun deleteAvatar(): Response<BaseResponse>

    // ==================== Tasks ====================
    @GET("api/v1/tasks")
    suspend fun getTasks(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("search") search: String? = null,
        @Query("status") status: String? = null
    ): Response<TaskListResponse>

    @GET("api/v1/tasks/{id}")
    suspend fun getTask(@Path("id") id: Int): Response<TaskResponse>

    @POST("api/v1/tasks")
    suspend fun createTask(@Body request: CreateTaskRequest): Response<TaskResponse>

    @PUT("api/v1/tasks/{id}")
    suspend fun updateTask(@Path("id") id: Int, @Body request: UpdateTaskRequest): Response<TaskResponse>

    @DELETE("api/v1/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/tasks/{id}/run")
    suspend fun runTask(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/tasks/{id}/stop")
    suspend fun stopTask(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/tasks/{id}/enable")
    suspend fun enableTask(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/tasks/{id}/disable")
    suspend fun disableTask(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/tasks/{id}/pin")
    suspend fun pinTask(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/tasks/{id}/unpin")
    suspend fun unpinTask(@Path("id") id: Int): Response<BaseResponse>

    @POST("api/v1/tasks/{id}/copy")
    suspend fun copyTask(@Path("id") id: Int): Response<TaskResponse>

    @GET("api/v1/tasks/{id}/latest-log")
    suspend fun getTaskLatestLog(@Path("id") id: Int): Response<TaskLatestLogResponse>

    @GET("api/v1/tasks/{id}/stats")
    suspend fun getTaskStats(@Path("id") id: Int): Response<TaskStatsResponse>

    @GET("api/v1/tasks/{id}/log-files")
    suspend fun getTaskLogFiles(@Path("id") id: Int): Response<TaskLogFilesResponse>

    @GET("api/v1/tasks/{id}/log-files/{filename}")
    suspend fun getTaskLogFileContent(@Path("id") id: Int, @Path("filename") filename: String): Response<TaskLogFileContentResponse>

    @GET("api/v1/tasks/{id}/log-files/{filename}/download")
    suspend fun downloadTaskLogFile(@Path("id") id: Int, @Path("filename") filename: String): Response<ResponseBody>

    @DELETE("api/v1/tasks/{id}/log-files/{filename}")
    suspend fun deleteTaskLogFile(@Path("id") id: Int, @Path("filename") filename: String): Response<BaseResponse>

    @PUT("api/v1/tasks/batch")
    suspend fun batchUpdateTasks(@Body request: BatchTaskRequest): Response<BaseResponse>

    @PUT("api/v1/tasks/batch/enable")
    suspend fun batchEnableTasks(@Body request: BatchTaskIdsRequest): Response<BaseResponse>

    @PUT("api/v1/tasks/batch/disable")
    suspend fun batchDisableTasks(@Body request: BatchTaskIdsRequest): Response<BaseResponse>

    @DELETE("api/v1/tasks/batch/delete")
    suspend fun batchDeleteTasks(@Body request: BatchTaskIdsRequest): Response<BaseResponse>

    @POST("api/v1/tasks/batch/run")
    suspend fun batchRunTasks(@Body request: BatchTaskIdsRequest): Response<BaseResponse>

    @DELETE("api/v1/tasks/clean-logs")
    suspend fun cleanTaskLogs(): Response<BaseResponse>

    @POST("api/v1/tasks/import")
    suspend fun importTasks(@Body request: ImportTasksRequest): Response<ImportTasksResponse>

    @GET("api/v1/tasks/export")
    suspend fun exportTasks(): Response<ExportTasksResponse>

    @POST("api/v1/tasks/cron/parse")
    suspend fun parseCron(@Body request: CronParseRequest): Response<CronParseResponse>

    @GET("api/v1/tasks/cron/templates")
    suspend fun getCronTemplates(): Response<CronTemplatesResponse>

    @GET("api/v1/tasks/notification-channels")
    suspend fun getNotificationChannels(): Response<NotificationChannelsResponse>

    // Task Views
    @GET("api/v1/tasks/views")
    suspend fun getTaskViews(): Response<TaskViewsResponse>

    @POST("api/v1/tasks/views")
    suspend fun createTaskView(@Body request: CreateTaskViewRequest): Response<TaskViewResponse>

    @PUT("api/v1/tasks/views/{viewId}")
    suspend fun updateTaskView(@Path("viewId") viewId: String, @Body request: UpdateTaskViewRequest): Response<TaskViewResponse>

    @DELETE("api/v1/tasks/views/{viewId}")
    suspend fun deleteTaskView(@Path("viewId") viewId: String): Response<BaseResponse>

    @PUT("api/v1/tasks/views/reorder")
    suspend fun reorderTaskViews(@Body request: ReorderTaskViewsRequest): Response<BaseResponse>

    // ==================== Environment Variables ====================
    @GET("api/v1/envs")
    suspend fun getEnvs(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("search") search: String? = null
    ): Response<EnvListResponse>

    @GET("api/v1/envs/{id}")
    suspend fun getEnv(@Path("id") id: Int): Response<EnvResponse>

    @POST("api/v1/envs")
    suspend fun createEnv(@Body request: CreateEnvRequest): Response<EnvResponse>

    @PUT("api/v1/envs/{id}")
    suspend fun updateEnv(@Path("id") id: Int, @Body request: UpdateEnvRequest): Response<EnvResponse>

    @DELETE("api/v1/envs/{id}")
    suspend fun deleteEnv(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/envs/{id}/enable")
    suspend fun enableEnv(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/envs/{id}/disable")
    suspend fun disableEnv(@Path("id") id: Int): Response<BaseResponse>

    @DELETE("api/v1/envs/batch")
    suspend fun batchDeleteEnvs(@Body request: BatchEnvIdsRequest): Response<BaseResponse>

    @PUT("api/v1/envs/batch/rename")
    suspend fun batchRenameEnvs(@Body request: BatchRenameEnvRequest): Response<BaseResponse>

    @PUT("api/v1/envs/batch/enable")
    suspend fun batchEnableEnvs(@Body request: BatchEnvIdsRequest): Response<BaseResponse>

    @PUT("api/v1/envs/batch/disable")
    suspend fun batchDisableEnvs(@Body request: BatchEnvIdsRequest): Response<BaseResponse>

    @PUT("api/v1/envs/batch/group")
    suspend fun batchSetGroupEnvs(@Body request: BatchSetGroupRequest): Response<BaseResponse>

    @GET("api/v1/envs/export")
    suspend fun exportEnvs(): Response<ExportEnvsResponse>

    @GET("api/v1/envs/export-all")
    suspend fun exportAllEnvs(): Response<ExportEnvsResponse>

    @PUT("api/v1/envs/sort")
    suspend fun sortEnvs(@Body request: SortEnvsRequest): Response<BaseResponse>

    @PUT("api/v1/envs/{id}/move-top")
    suspend fun moveEnvToTop(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/envs/{id}/cancel-top")
    suspend fun cancelMoveEnvToTop(@Path("id") id: Int): Response<BaseResponse>

    @GET("api/v1/envs/groups")
    suspend fun getEnvGroups(): Response<EnvGroupsResponse>

    @POST("api/v1/envs/import")
    suspend fun importEnvs(@Body request: ImportEnvsRequest): Response<BaseResponse>

    // ==================== Scripts ====================
    @GET("api/v1/scripts")
    suspend fun getScripts(@Query("path") path: String? = null): Response<ScriptListResponse>

    @GET("api/v1/scripts/tree")
    suspend fun getScriptTree(): Response<ScriptTreeResponse>

    @GET("api/v1/scripts/content")
    suspend fun getScriptContent(@Query("path") path: String): Response<ScriptContentResponse>

    @PUT("api/v1/scripts/content")
    suspend fun saveScriptContent(@Body request: SaveScriptRequest): Response<BaseResponse>

    @Multipart
    @POST("api/v1/scripts/upload")
    suspend fun uploadScript(@Part file: MultipartBody.Part, @Part("path") path: RequestBody? = null): Response<BaseResponse>

    @GET("api/v1/scripts/download")
    suspend fun downloadScript(@Query("path") path: String): Response<ResponseBody>

    @DELETE("api/v1/scripts")
    suspend fun deleteScript(@Query("path") path: String): Response<BaseResponse>

    @POST("api/v1/scripts/directory")
    suspend fun createDirectory(@Body request: CreateDirectoryRequest): Response<BaseResponse>

    @PUT("api/v1/scripts/rename")
    suspend fun renameScript(@Body request: RenameScriptRequest): Response<BaseResponse>

    @PUT("api/v1/scripts/move")
    suspend fun moveScript(@Body request: MoveScriptRequest): Response<BaseResponse>

    @POST("api/v1/scripts/copy")
    suspend fun copyScript(@Body request: CopyScriptRequest): Response<BaseResponse>

    @DELETE("api/v1/scripts/batch")
    suspend fun batchDeleteScripts(@Body request: BatchDeleteScriptsRequest): Response<BaseResponse>

    @GET("api/v1/scripts/versions")
    suspend fun getScriptVersions(@Query("path") path: String): Response<ScriptVersionsResponse>

    @GET("api/v1/scripts/versions/{id}")
    suspend fun getScriptVersion(@Path("id") id: String): Response<ScriptVersionResponse>

    @PUT("api/v1/scripts/versions/{id}/rollback")
    suspend fun rollbackScriptVersion(@Path("id") id: String): Response<BaseResponse>

    @DELETE("api/v1/scripts/versions")
    suspend fun clearScriptVersions(@Query("path") path: String): Response<BaseResponse>

    @POST("api/v1/scripts/run")
    suspend fun runScript(@Body request: RunScriptRequest): Response<RunScriptResponse>

    @POST("api/v1/scripts/run-code")
    suspend fun runCode(@Body request: RunCodeRequest): Response<RunScriptResponse>

    @GET("api/v1/scripts/run/{runId}/logs")
    suspend fun getScriptRunLogs(@Path("runId") runId: String): Response<ScriptRunLogsResponse>

    @PUT("api/v1/scripts/run/{runId}/stop")
    suspend fun stopScriptRun(@Path("runId") runId: String): Response<BaseResponse>

    @DELETE("api/v1/scripts/run/{runId}")
    suspend fun clearScriptRun(@Path("runId") runId: String): Response<BaseResponse>

    @POST("api/v1/scripts/format")
    suspend fun formatScript(@Body request: FormatScriptRequest): Response<FormatScriptResponse>

    // ==================== Logs ====================
    @GET("api/v1/logs")
    suspend fun getLogs(
        @Query("task_id") taskId: Int? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): Response<LogListResponse>

    @GET("api/v1/logs/{id}")
    suspend fun getLog(@Path("id") id: Int): Response<LogDetailResponse>

    @GET("api/v1/logs/{id}/stream")
    suspend fun getLogStream(@Path("id") id: Int): Response<ResponseBody>

    @DELETE("api/v1/logs/{id}")
    suspend fun deleteLog(@Path("id") id: Int): Response<BaseResponse>

    @DELETE("api/v1/logs/batch")
    suspend fun batchDeleteLogs(@Body request: BatchDeleteLogsRequest): Response<BaseResponse>

    @DELETE("api/v1/logs/clean")
    suspend fun cleanLogs(@Query("days") days: Int? = null): Response<BaseResponse>

    // ==================== Dependencies ====================
    @GET("api/v1/deps")
    suspend fun getDeps(@Query("type") type: String = "nodejs"): Response<DependencyListResponse>

    @POST("api/v1/deps")
    suspend fun createDep(@Body request: CreateDepRequest): Response<DependencyResponse>

    @DELETE("api/v1/deps/{id}")
    suspend fun deleteDep(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/deps/{id}/reinstall")
    suspend fun reinstallDep(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/deps/{id}/cancel")
    suspend fun cancelDep(@Path("id") id: Int): Response<BaseResponse>

    @GET("api/v1/deps/{id}/status")
    suspend fun getDepStatus(@Path("id") id: Int): Response<DepStatusResponse>

    @GET("api/v1/deps/{id}/log-stream")
    suspend fun getDepLogStream(@Path("id") id: Int): Response<ResponseBody>

    @POST("api/v1/deps/batch-reinstall")
    suspend fun batchReinstallDeps(@Body request: BatchDepIdsRequest): Response<BaseResponse>

    @POST("api/v1/deps/batch-delete")
    suspend fun batchDeleteDeps(@Body request: BatchDepIdsRequest): Response<BaseResponse>

    @GET("api/v1/deps/export")
    suspend fun exportDeps(): Response<ExportDepsResponse>

    @GET("api/v1/deps/pip")
    suspend fun getPipList(): Response<PipListResponse>

    @GET("api/v1/deps/npm")
    suspend fun getNpmList(): Response<NpmListResponse>

    @GET("api/v1/deps/mirrors")
    suspend fun getDepMirrors(): Response<DepMirrorsResponse>

    @PUT("api/v1/deps/mirrors")
    suspend fun setDepMirrors(@Body request: SetDepMirrorsRequest): Response<BaseResponse>

    // ==================== System ====================
    @GET("api/v1/system/info")
    suspend fun getSystemInfo(): Response<SystemInfoResponse>

    @GET("api/v1/system/machine-code")
    suspend fun getMachineCode(): Response<MachineCodeResponse>

    @GET("api/v1/system/dashboard")
    suspend fun getDashboard(): Response<DashboardResponse>

    @GET("api/v1/system/stats")
    suspend fun getStats(): Response<StatsResponse>

    @GET("api/v1/system/version")
    suspend fun getVersion(): Response<VersionResponse>

    @GET("api/v1/system/check-update")
    suspend fun checkUpdate(): Response<CheckUpdateResponse>

    @GET("api/v1/system/health-check")
    suspend fun getHealthCheck(): Response<HealthCheckResponse>

    @POST("api/v1/system/health-check")
    suspend fun runHealthCheck(): Response<HealthCheckResponse>

    @GET("api/v1/system/panel-log")
    suspend fun getPanelLog(@Query("lines") lines: Int = 100): Response<PanelLogResponse>

    @POST("api/v1/system/backup")
    suspend fun createBackup(): Response<BackupResponse>

    @GET("api/v1/system/backups")
    suspend fun getBackupList(): Response<BackupListResponse>

    @GET("api/v1/system/backup/download/{filename}")
    suspend fun downloadBackup(@Path("filename") filename: String): Response<ResponseBody>

    @Multipart
    @POST("api/v1/system/backup/upload")
    suspend fun uploadBackup(@Part file: MultipartBody.Part): Response<BaseResponse>

    @POST("api/v1/system/restore")
    suspend fun restoreBackup(@Body request: RestoreBackupRequest): Response<BaseResponse>

    @GET("api/v1/system/restore/progress")
    suspend fun getRestoreProgress(): Response<RestoreProgressResponse>

    @DELETE("api/v1/system/backup")
    suspend fun deleteBackup(@Body request: DeleteBackupRequest): Response<BaseResponse>

    @POST("api/v1/system/update")
    suspend fun updatePanel(): Response<BaseResponse>

    @POST("api/v1/system/restart")
    suspend fun restartPanel(): Response<BaseResponse>

    @GET("api/v1/system/config-script")
    suspend fun getConfigScript(): Response<ConfigScriptResponse>

    @PUT("api/v1/system/config-script")
    suspend fun saveConfigScript(@Body request: SaveConfigScriptRequest): Response<BaseResponse>

    // ==================== Notifications ====================
    @GET("api/v1/notifications")
    suspend fun getNotifications(): Response<NotificationListResponse>

    @POST("api/v1/notifications")
    suspend fun createNotification(@Body request: CreateNotificationRequest): Response<NotificationResponse>

    @PUT("api/v1/notifications/{id}")
    suspend fun updateNotification(@Path("id") id: Int, @Body request: UpdateNotificationRequest): Response<NotificationResponse>

    @DELETE("api/v1/notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/notifications/{id}/enable")
    suspend fun enableNotification(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/notifications/{id}/disable")
    suspend fun disableNotification(@Path("id") id: Int): Response<BaseResponse>

    @POST("api/v1/notifications/{id}/test")
    suspend fun testNotification(@Path("id") id: Int): Response<BaseResponse>

    @GET("api/v1/notifications/types")
    suspend fun getNotificationTypes(): Response<NotificationTypesResponse>

    @POST("api/v1/notifications/send")
    suspend fun sendNotification(@Body request: SendNotificationRequest): Response<BaseResponse>

    // ==================== Subscriptions ====================
    @GET("api/v1/subscriptions")
    suspend fun getSubscriptions(): Response<SubscriptionListResponse>

    @POST("api/v1/subscriptions")
    suspend fun createSubscription(@Body request: CreateSubscriptionRequest): Response<SubscriptionResponse>

    @PUT("api/v1/subscriptions/{id}")
    suspend fun updateSubscription(@Path("id") id: Int, @Body request: UpdateSubscriptionRequest): Response<SubscriptionResponse>

    @DELETE("api/v1/subscriptions/{id}")
    suspend fun deleteSubscription(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/subscriptions/{id}/enable")
    suspend fun enableSubscription(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/subscriptions/{id}/disable")
    suspend fun disableSubscription(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/subscriptions/{id}/pull")
    suspend fun pullSubscription(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/subscriptions/{id}/pull/stop")
    suspend fun stopPullSubscription(@Path("id") id: Int): Response<BaseResponse>

    @GET("api/v1/subscriptions/{id}/pull-stream")
    suspend fun getSubscriptionPullStream(@Path("id") id: Int): Response<ResponseBody>

    @GET("api/v1/subscriptions/{id}/logs")
    suspend fun getSubscriptionLogs(@Path("id") id: Int): Response<SubscriptionLogsResponse>

    @DELETE("api/v1/subscriptions/batch")
    suspend fun batchDeleteSubscriptions(@Body request: BatchDeleteSubscriptionsRequest): Response<BaseResponse>

    // ==================== SSH Keys ====================
    @GET("api/v1/ssh-keys")
    suspend fun getSSHKeys(): Response<SSHKeyListResponse>

    @POST("api/v1/ssh-keys")
    suspend fun createSSHKey(@Body request: CreateSSHKeyRequest): Response<SSHKeyResponse>

    @PUT("api/v1/ssh-keys/{id}")
    suspend fun updateSSHKey(@Path("id") id: Int, @Body request: UpdateSSHKeyRequest): Response<SSHKeyResponse>

    @DELETE("api/v1/ssh-keys/{id}")
    suspend fun deleteSSHKey(@Path("id") id: Int): Response<BaseResponse>

    @GET("api/v1/ssh-keys/{id}")
    suspend fun getSSHKey(@Path("id") id: Int): Response<SSHKeyResponse>

    // ==================== Users ====================
    @GET("api/v1/users")
    suspend fun getUsers(): Response<UserListResponse>

    @POST("api/v1/users")
    suspend fun createUser(@Body request: CreateUserRequest): Response<UserResponse>

    @PUT("api/v1/users/{id}")
    suspend fun updateUser(@Path("id") id: Int, @Body request: UpdateUserRequest): Response<UserResponse>

    @DELETE("api/v1/users/{id}")
    suspend fun deleteUser(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/users/{id}/reset-password")
    suspend fun resetUserPassword(@Path("id") id: Int, @Body request: ResetPasswordRequest): Response<BaseResponse>

    // ==================== Security ====================
    @GET("api/v1/security/login-logs")
    suspend fun getLoginLogs(@Query("page") page: Int = 1, @Query("page_size") pageSize: Int = 20): Response<LoginLogListResponse>

    @DELETE("api/v1/security/login-logs")
    suspend fun clearLoginLogs(): Response<BaseResponse>

    @GET("api/v1/security/sessions")
    suspend fun getSessions(): Response<SessionListResponse>

    @DELETE("api/v1/security/sessions/others")
    suspend fun revokeOtherSessions(): Response<BaseResponse>

    @DELETE("api/v1/security/sessions/{id}")
    suspend fun revokeSession(@Path("id") id: String): Response<BaseResponse>

    @DELETE("api/v1/security/sessions/user/{userId}")
    suspend fun revokeAllUserSessions(@Path("userId") userId: Int): Response<BaseResponse>

    @GET("api/v1/security/ip-whitelist")
    suspend fun getIPWhitelist(): Response<IPWhitelistResponse>

    @POST("api/v1/security/ip-whitelist")
    suspend fun addIPWhitelist(@Body request: AddIPWhitelistRequest): Response<BaseResponse>

    @DELETE("api/v1/security/ip-whitelist/{id}")
    suspend fun removeIPWhitelist(@Path("id") id: Int): Response<BaseResponse>

    @GET("api/v1/security/audit-logs")
    suspend fun getAuditLogs(@Query("page") page: Int = 1, @Query("page_size") pageSize: Int = 20): Response<AuditLogListResponse>

    @GET("api/v1/security/login-stats")
    suspend fun getLoginStats(): Response<LoginStatsResponse>

    @POST("api/v1/security/2fa/setup")
    suspend fun setup2FA(): Response<Setup2FAResponse>

    @POST("api/v1/security/2fa/verify")
    suspend fun verify2FA(@Body request: Verify2FARequest): Response<BaseResponse>

    @DELETE("api/v1/security/2fa")
    suspend fun disable2FA(): Response<BaseResponse>

    @GET("api/v1/security/2fa/status")
    suspend fun get2FAStatus(): Response<TwoFAStatusResponse>

    // ==================== Configs ====================
    @GET("api/v1/configs")
    suspend fun getConfigs(): Response<ConfigListResponse>

    @GET("api/v1/configs/{key}")
    suspend fun getConfig(@Path("key") key: String): Response<ConfigResponse>

    @POST("api/v1/configs")
    suspend fun setConfig(@Body request: SetConfigRequest): Response<BaseResponse>

    @PUT("api/v1/configs/batch")
    suspend fun batchSetConfigs(@Body request: BatchSetConfigsRequest): Response<BaseResponse>

    @DELETE("api/v1/configs/{key}")
    suspend fun deleteConfig(@Path("key") key: String): Response<BaseResponse>

    // ==================== Platform Tokens ====================
    @GET("api/v1/platform-tokens/platforms")
    suspend fun getPlatforms(): Response<PlatformListResponse>

    @POST("api/v1/platform-tokens/platforms")
    suspend fun createPlatform(@Body request: CreatePlatformRequest): Response<PlatformResponse>

    @DELETE("api/v1/platform-tokens/platforms/{id}")
    suspend fun deletePlatform(@Path("id") id: Int): Response<BaseResponse>

    @GET("api/v1/platform-tokens")
    suspend fun getPlatformTokens(): Response<PlatformTokenListResponse>

    @POST("api/v1/platform-tokens")
    suspend fun createPlatformToken(@Body request: CreatePlatformTokenRequest): Response<PlatformTokenResponse>

    @PUT("api/v1/platform-tokens/{id}")
    suspend fun updatePlatformToken(@Path("id") id: Int, @Body request: UpdatePlatformTokenRequest): Response<PlatformTokenResponse>

    @DELETE("api/v1/platform-tokens/{id}")
    suspend fun deletePlatformToken(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/platform-tokens/{id}/enable")
    suspend fun enablePlatformToken(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/platform-tokens/{id}/disable")
    suspend fun disablePlatformToken(@Path("id") id: Int): Response<BaseResponse>

    // ==================== Open API ====================
    @GET("api/v1/open-api/apps")
    suspend fun getOpenAPIApps(): Response<OpenAPIAppListResponse>

    @POST("api/v1/open-api/apps")
    suspend fun createOpenAPIApp(@Body request: CreateOpenAPIAppRequest): Response<OpenAPIAppResponse>

    @PUT("api/v1/open-api/apps/{id}")
    suspend fun updateOpenAPIApp(@Path("id") id: Int, @Body request: UpdateOpenAPIAppRequest): Response<OpenAPIAppResponse>

    @DELETE("api/v1/open-api/apps/{id}")
    suspend fun deleteOpenAPIApp(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/open-api/apps/{id}/enable")
    suspend fun enableOpenAPIApp(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/open-api/apps/{id}/disable")
    suspend fun disableOpenAPIApp(@Path("id") id: Int): Response<BaseResponse>

    @PUT("api/v1/open-api/apps/{id}/reset-secret")
    suspend fun resetOpenAPIAppSecret(@Path("id") id: Int): Response<ResetSecretResponse>

    @POST("api/v1/open-api/apps/{id}/view-secret")
    suspend fun viewOpenAPIAppSecret(@Path("id") id: Int): Response<ViewSecretResponse>

    @GET("api/v1/open-api/apps/{id}/logs")
    suspend fun getOpenAPIAppLogs(@Path("id") id: Int, @Query("page") page: Int = 1, @Query("page_size") pageSize: Int = 20): Response<OpenAPILogListResponse>

    // ==================== Sponsors ====================
    @GET("api/v1/sponsors")
    suspend fun getSponsors(): Response<SponsorResponse>

    // ==================== Android Runtime ====================
    @GET("api/v1/android-runtime/status")
    suspend fun getAndroidRuntimeStatus(): Response<AndroidRuntimeStatusResponse>

    @POST("api/v1/android-runtime/install")
    suspend fun installAndroidRuntime(): Response<BaseResponse>

    @POST("api/v1/android-runtime/uninstall")
    suspend fun uninstallAndroidRuntime(): Response<BaseResponse>

    // ==================== Version & Health ====================
    @GET("api/v1/version")
    suspend fun getApiVersion(): Response<ApiVersionResponse>

    @GET("api/v1/health")
    suspend fun getApiHealth(): Response<ApiHealthResponse>

    // ==================== Panel Settings ====================
    @GET("api/v1/system/panel-settings")
    suspend fun getPanelSettings(): Response<PanelSettingsResponse>

    @GET("api/v1/system/public-version")
    suspend fun getPublicVersion(): Response<PublicVersionResponse>
}
