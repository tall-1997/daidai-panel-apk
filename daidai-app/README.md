# 呆呆面板 Android App

基于呆呆面板 API 开发的 Android 客户端应用，支持面板原生功能，同时提供扩展模块，帮助用户快捷管理定时任务。

## 功能特性

### 基础功能
- **定时任务管理**：支持任务的增删改查、批量操作、执行、停止、启用、禁用
- **环境变量管理**：支持环境变量的增删改查、批量操作
- **脚本管理**：支持脚本文件的查看、编辑、上传
- **执行日志**：支持查看任务执行日志、实时日志流
- **系统监控**：支持 CPU、内存、磁盘监控，系统状态查看
- **用户管理**：支持用户登录、退出、个人信息管理

### 扩展模块
- **Web 助手**：网页 Cookie 提取和导入变量功能
- **快捷操作**：常用任务的快捷执行和批量操作
- **数据统计**：任务执行统计和数据分析
- **通知管理**：消息推送渠道配置和管理
- **自定义扩展**：支持用户自定义扩展模块

## 技术栈

- **开发语言**：Kotlin
- **UI 框架**：Jetpack Compose
- **架构模式**：MVVM
- **依赖注入**：Hilt
- **网络请求**：Retrofit + OkHttp
- **数据存储**：DataStore
- **图片加载**：Coil

## 项目结构

```
app/src/main/java/com/daidai/app/
├── data/                    # 数据层
│   ├── local/              # 本地数据存储
│   ├── remote/             # 网络请求
│   │   ├── model/          # API 数据模型
│   │   ├── ApiService.kt   # API 接口定义
│   │   ├── AuthInterceptor.kt  # 认证拦截器
│   │   └── TokenManager.kt     # Token 管理
│   └── repository/         # 数据仓库
├── domain/                  # 领域层
│   ├── model/              # 领域模型
│   ├── repository/         # 仓库接口
│   └── usecase/            # 用例
├── ui/                      # 表现层
│   ├── screen/             # 页面
│   │   ├── login/          # 登录页面
│   │   └── home/           # 主页面
│   ├── component/          # 通用组件
│   ├── theme/              # 主题
│   └── navigation/         # 导航
└── di/                      # 依赖注入
```

## 开发环境

- Android Studio Hedgehog | 2023.1.1
- Kotlin 1.9.20
- Jetpack Compose 1.5.5
- Gradle 8.2

## 快速开始

1. 克隆项目到本地
2. 使用 Android Studio 打开项目
3. 同步 Gradle 依赖
4. 连接 Android 设备或启动模拟器
5. 运行项目

## 配置说明

### 面板地址配置
在 `NetworkModule.kt` 中修改默认面板地址：
```kotlin
.baseUrl("http://localhost:5700/") // 修改为实际面板地址
```

### API 接口
App 使用呆呆面板的 v1 版本 API，主要接口包括：
- `/api/v1/auth/login` - 用户登录
- `/api/v1/tasks` - 任务管理
- `/api/v1/envs` - 环境变量管理
- `/api/v1/scripts` - 脚本管理
- `/api/v1/logs` - 日志管理
- `/api/v1/system/info` - 系统信息

## 开发计划

### 第一阶段：基础功能
- [x] 项目架构搭建
- [x] 登录功能
- [ ] 任务列表展示
- [ ] 任务详情查看
- [ ] 任务创建/编辑
- [ ] 任务执行/停止

### 第二阶段：完整功能
- [ ] 环境变量管理
- [ ] 脚本管理
- [ ] 日志查看
- [ ] 系统监控

### 第三阶段：扩展功能
- [ ] Web 助手
- [ ] 快捷操作
- [ ] 数据统计
- [ ] 通知管理
- [ ] 自定义扩展

## 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 致谢

- [呆呆面板](https://github.com/linzixuanzz/daidai-panel) - 后端 API 支持
- [青龙面板 APP](https://gitee.com/qlpanel/QingLong-App) - 功能参考
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - UI 框架
- [Material Design 3](https://m3.material.io/) - 设计规范
