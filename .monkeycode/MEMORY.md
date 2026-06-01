# 用户指令记忆

本文件记录了用户的指令、偏好和教导，用于在未来的交互中提供参考。

## 格式

### 用户指令条目
用户指令条目应遵循以下格式：

[用户指令摘要]
- Date: [YYYY-MM-DD]
- Context: [提及的场景或时间]
- Instructions:
  - [用户教导或指示的内容，逐行描述]

### 项目知识条目
Agent 在任务执行过程中发现的条目应遵循以下格式：

[项目知识摘要]
- Date: [YYYY-MM-DD]
- Context: Agent 在执行 [具体任务描述] 时发现
- Category: [运维部署|构建方法|测试方法|排错调试|工作流协作|环境配置]
- Instructions:
  - [具体的知识点，逐行描述]

## 去重策略
- 添加新条目前，检查是否存在相似或相同的指令
- 若发现重复，跳过新条目或与已有条目合并
- 合并时，更新上下文或日期信息
- 这有助于避免冗余条目，保持记忆文件整洁

## 条目

### Flutter 项目构建配置
- Date: 2026-06-01
- Context: Agent 在修复呆呆面板 Flutter App 构建问题时发现
- Category: 构建方法
- Instructions:
  - 项目使用 Flutter 跨平台框架，同时构建 Android APK 和 iOS IPA
  - Android 构建需要 AGP 9、Gradle 9.1.0、Kotlin 2.3.20、compileSdk 36
  - iOS 构建需要在 project.pbxproj 中设置 CODE_SIGNING_ALLOWED = NO 和 CODE_SIGNING_REQUIRED = NO
  - file_picker 必须使用 v10.x 版本，v11 与 AGP 9 存在兼容性问题
  - permission_handler 使用 v12.0.0 或更高版本
  - build.gradle.kts 必须应用 org.jetbrains.kotlin.android 插件

### GitHub Actions 发布流程
- Date: 2026-06-01
- Context: Agent 在配置 GitHub Actions 自动发布时发现
- Category: 工作流协作
- Instructions:
  - Release 任务只在推送 tag 时触发（if: startsWith(github.ref, 'refs/tags/v')）
  - tag 格式必须是 v*-flutter，例如 v0.0.22-flutter
  - 构建产物包括 android-apk 和 ios-ipa 两个 artifact
  - 发布到 GitHub Releases 后可下载 APK 和 IPA 文件

### 仓库信息
- Date: 2026-06-01
- Context: 用户提供的仓库迁移信息
- Category: 运维部署
- Instructions:
  - 原始仓库: https://github.com/tall-1997/daidai-panel
  - 迁移后仓库: https://github.com/tall-1997/daidai-panel-apk
  - Flutter 代码在 flutter-app 分支上
  - main 分支保留原始 Kotlin 代码
