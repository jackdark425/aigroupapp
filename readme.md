# AIGroupApp 技术文档

## 项目概述

AIGroupApp 是一个功能丰富的 Android 原生应用，专注于提供多模态 AI 对话和知识管理功能。该应用采用现代 Android 开发技术栈，实现了高度可定制的 AI 助手系统，支持多种 AI 模型的接入和知识库管理。

## 技术架构

### 核心框架

- **开发语言**: Kotlin
- **UI 框架**: Jetpack Compose + Material 3
- **依赖注入**: Dagger Hilt
- **异步处理**: Kotlin Coroutines + Flow
- **构建工具**: Gradle (Kotlin DSL)
- **最低 API**: 26 (Android 8.0)
- **目标 API**: 34 (Android 14)

### UI 架构

1. **Material 3 设计系统**

   - 支持动态主题颜色
   - 自适应布局设计
   - 深色/浅色主题支持
2. **Compose 组件架构**

   - 使用 MVVM 架构模式
   - 状态提升和单向数据流
   - 支持动画和手势交互

### 路由系统

参考 app/src/main/java/com/aigroup/aigroupmobile/Routes.kt

#### 1. 核心结构

- 基于 Jetpack Navigation Compose
- 采用密封类定义路由
- 支持参数传递和类型安全

#### 2. 主要路由分类

- 欢迎流程路由（Welcome Flow）
- 主页面路由（Home Navigation）
- 助手相关路由（Assistant Routes）
- 设置页面路由（Settings Routes）

#### 3. 导航特性

- 自定义页面转场动画
- 集成触觉反馈
- 支持分屏模式
- 抽屉式布局导航

## ViewModel 架构指南

### 概述

本项目采用 MVVM 架构模式，使用 ViewModel 作为 UI 层和数据层之间的桥梁。ViewModel 实现遵循 Android Architecture Components 的最佳实践，并结合 Kotlin 协程和 Flow 进行状态管理。

### 依赖注入与基础结构

#### 基础配置

所有 ViewModel 都使用 Hilt 进行依赖注入：

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatDao: ChatDao,
    private val userDao: UserDao,
    private val dataStore: DataStore<AppPreferences>,
    stateHandle: SavedStateHandle
) : ViewModel()
```

#### 状态管理系统

1. 持久化状态

```kotlin
// DataStore 状态
private val preferences = dataStore.data
val chatViewMode = preferences.map { it.chatViewMode }

// SavedStateHandle 状态
private val chatId = stateHandle.get<String>("chatId")!!
```

2. UI 状态

```kotlin
// 状态定义
data class ChatBottomBarState(
    val inputText: AnnotatedString = buildAnnotatedString { },
    val isRecognizing: Boolean = false,
    val recognizingText: String? = null,
    val mediaItem: MediaItem? = null
)

// 状态管理
private val _bottombarState = MutableStateFlow(ChatBottomBarState())
val bottomBarState = _bottombarState.asStateFlow()
```

#### 协程集成

1. 异常处理

```kotlin
private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
    // 错误处理逻辑
}
```

2. 协程作用域

```kotlin
viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
    // 异步操作
    withContext(Dispatchers.Main) {
        // UI 更新
    }
}
```

#### 数据操作示例

1. 数据库操作

```kotlin
fun updateChatProperties(properties: AppPreferences.LongBotProperties) {
    viewModelScope.launch(Dispatchers.IO) {
        chatDao.updatePrimaryBot(session) {
            temperature = properties.temperature
            topP = properties.topP
            frequencyPenalty = properties.frequencyPenalty
            presencePenalty = properties.presencePenalty
        }
    }
}
```

2. 状态更新

```kotlin
fun updateInputText(text: AnnotatedString) {
    _bottombarState.value = bottomBarState.value.copy(inputText = text)
}
```

3. 复杂业务逻辑

```kotlin
suspend fun startMessageLoading(message: MessageChat, action: suspend () -> Unit) {
    if (_loadingId.value != null) {
        Log.w(TAG, "Loading id is not null, skip loading")
        return
    }

    withContext(Dispatchers.Main) {
        _loadingId.value = message.id.toHexString()
        action()
        _loadingId.value = null
    }
}
```

#### 生命周期管理

```kotlin
override fun onCleared() {
    super.onCleared()
    // 资源清理
    chatPluginExecutor.close()
}
```

#### 开发规范

1. 命名规则

- ViewModel 类名必须以 ViewModel 结尾
- 私有状态使用下划线前缀
- 公开状态使用描述性名称

2. 代码组织

- 相关的状态和操作应该放在一起
- 使用 companion object 存储常量
- 使用区域注释组织代码块

3. 注释要求

```kotlin
/**
 * 重试消息发送
 * 
 * @param retryMessage 重试的消息，该消息应该为 bot 所发送
 * @param imageSingleContext 是否在单独上下文中发送图片
 * @param mediaCompatibilityHistory 媒体内容的后备采用策略
 */
fun retryMessage(
    retryMessage: MessageChat,
    imageSingleContext: Boolean = false,
    mediaCompatibilityHistory: MediaCompatibilityHistory = MediaCompatibilityHistory.HELP_PROMPT,
)
```

#### 最佳实践

1. 状态管理最佳实践

- 使用不可变状态设计
- 实现单一数据源原则
- 封装复杂状态逻辑

2. 异常处理最佳实践

- 实现统一的异常处理
- 提供错误恢复机制
- 使用用户友好的错误提示

3. 性能优化最佳实践

- 避免在主线程进行耗时操作
- 合理使用协程调度器
- 注意防止内存泄漏

4. 数据持久化与数据库

- 在必要的时候创建新的 Dao 类

#### 注意事项

1. 数据一致性

- 确保状态更新的原子性
- 正确处理并发操作
- 维护数据间的关系

2. 内存管理

- 及时释放资源
- 避免内存泄漏
- 合理使用缓存

3. 测试考虑

- 提供可测试的接口
- 避免复杂的依赖关系
- 支持单元测试的状态设计

## 数据架构

### 1. Realm 数据库

主要用于核心业务数据存储，当前 Schema 版本为 29。

#### 核心数据模型

1. 聊天相关：

   - `ChatSession`: 聊天会话
   - `MessageChat`: 聊天消息
   - `MessageChatData`: 消息数据
   - 各类消息类型（文本、图片、视频、文档）
2. 用户相关：

   - `UserProfile`: 用户配置文件
   - `MessageSenderUser`: 用户发送者
   - `MessageSenderBot`: 机器人发送者
3. 知识库相关：

   - `KnowledgeBase`: 知识库
   - `DocumentItem`: 文档项目

#### 技术特点

- 使用 Kotlin Realm SDK
- 支持自动模式迁移
- LiveData 集成支持

### 2. Protocol Buffers DataStore

#### 配置项类别

1. API Token 配置

   - 多种 AI 服务商的 token
   - 服务端点配置
2. 模型配置

   - 默认模型及参数
   - 生成参数配置
   - 图像和视频生成配置
3. UI 偏好设置

   - 视图模式选择
   - 主题设置
   - 界面定制选项

#### 技术特点

- 使用 Proto3 语法
- 类型安全的数据访问
- 支持数据迁移

### 3. ObjectBox 向量数据库

#### 核心功能

- 实现高效的向量相似度搜索
- 支持 HNSW 算法
- 3072 维向量存储

#### 主要模型

1. `KnowledgeChunk`

   - 文本内容存储
   - 向量表示
   - 元数据管理
2. `KnowledgeDocument`

   - 文档容器
   - 关系管理

## AI 能力集成

### 架构概述

AIGroupApp 实现了一个灵活且可扩展的多 AI 服务提供商集成架构，支持包括 OpenAI、Anthropic Claude、Google PaLM 等在内的多个主流 AI 服务商。该架构采用统一的接口设计，同时保持了对各服务商特性的充分支持。

### 核心组件

#### 1. ChatEndpoint 接口

ChatEndpoint 作为统一的服务接口，为所有 AI 提供商定义了标准化的通信协议：

- 继承自 OpenAI 的基础接口
- 支持通用的 HTTP 客户端配置
- 统一的日志和重试机制
- 标准化的请求选项处理

#### 2. ChatServiceProvider 枚举类

统一管理所有支持的 AI 服务提供商：

```kotlin
enum class ChatServiceProvider(
    val id: String,
    val apiBase: String,
    @StringRes val descriptionRes: Int
)
```

主要特性：

- 集中式服务商配置管理
- 统一的品牌标识和本地化支持
- 灵活的服务端点配置
- 自定义模型支持

#### 3. 服务商适配层

##### 3.1 标准 OpenAI 兼容实现

对于遵循 OpenAI API 规范的服务商（如 Moonshot、Baichuan 等），采用简单的适配方式：

```kotlin
class OpenAIChatEndpoint(private val openAI: OpenAI) : ChatEndpoint,
    com.aallam.openai.client.Chat by openAI,
    com.aallam.openai.client.Models by openAI
```

##### 3.2 自定义协议适配

针对使用专有 API 的服务商（如 Anthropic Claude、Google、百度等），实现完整的协议转换：

- 请求格式转换
- 响应数据映射
- 流式传输适配
- 错误处理标准化

示例（Anthropic Claude）：

```kotlin
class AnthropicChat(private val config: OpenAIConfig) : ChatEndpoint {
    // 自定义协议转换
    private fun ChatCompletionRequest.toAnthropicBody(stream: Boolean): JsonObject {
        // 消息格式转换
        // 系统提示处理
        // 参数映射
    }
  
    // 统一的响应处理
    override suspend fun chatCompletion(
        request: ChatCompletionRequest, 
        requestOptions: RequestOptions?
    ): ChatCompletion
}
```

### 技术特性

#### 1. 多模态支持

- 文本对话
- 图像理解和生成
- 语音交互集成
- 文档分析能力

#### 2. 流式传输

- 支持实时响应流
- 分块传输优化
- 统一的事件处理机制

#### 3. 错误处理

- 标准化的错误码体系
- 重试机制
- 降级策略

#### 4. 配置管理

- 动态服务端点配置
- Token 管理
- 模型参数定制

### 扩展性设计

#### 1. 新增服务商流程

1. 在 ChatServiceProvider 中添加服务商定义
2. 实现 ChatEndpoint 接口
3. 配置服务商特定参数
4. 注册服务实现

#### 2. 自定义能力扩展

- 支持自定义模型配置
- 可扩展的响应处理管道
- 插件化的功能扩展

通过这种架构设计，AIGroupApp 实现了对多个 AI 服务提供商的无缝集成，为用户提供了统一且强大的 AI 能力访问接口，同时保持了良好的扩展性和可维护性。

## 多媒体处理

### 核心组件

- **图片处理**: Coil
- **视频播放**: Media3
- **文档解析**: PDFBox + Apache Tika
- **相机功能**: CameraX API

### 特性支持

- 图片和视频压缩
- 格式转换
- 实时预览
- 文件管理

## 开发规范

### 1. 代码组织

- 模块化架构
- 清晰的包结构
- 统一的命名规范

### 2. 文件命名

- Compose 组件：PascalCase
- 工具类：下划线命名

### 3. 数据库开发

- 版本管理
- 迁移策略
- 性能优化

### 4. UI 开发

- 响应式设计
- 可访问性支持
- 性能优化

## 性能优化

### 1. 数据库优化

- 索引策略
- 批量操作
- 异步处理

### 2. UI 优化

- 组件复用
- 延迟加载
- 列表性能

### 3. 内存管理

- 图片内存管理
- 缓存策略
- 资源释放

## 安全性

### 1. 数据安全

- API 密钥加密
- 本地数据加密
- 安全存储

### 2. 权限管理

- 运行时权限
- 最小权限原则
- 降级处理
