## 项目概览

IntelliJ IDEA 插件（兼容 2025.2+ / build `252.25557+`），为编辑器提供基于 [DeepSeek FIM](https://api-docs.deepseek.com/zh-cn/api/create-completion) 的 inline 灰色补全。Java 21 + Kotlin 2.2 混编，使用 IntelliJ Platform Gradle 插件 2.10.x 构建。

## 常用命令

```bash
./gradlew build           # 编译 + 打包 + 跑测试
./gradlew buildPlugin     # 仅打包，产物在 build/distributions/yl-code-completion-*.zip
./gradlew runIde          # 启动沙箱 IDE 调试（也可用 .run/Run IDE with Plugin.run.xml）
./gradlew verifyPlugin    # 跑 IntelliJ Plugin Verifier
./gradlew test --tests "FullyQualifiedClassName.methodName"  # 跑单个测试
```

- 版本号来自环境变量 `PLUGIN_VERSION`（GitHub Actions 注入 git tag），本地无该变量时默认 `1.0-SNAPSHOT`。
- `build.gradle.kts` 会探测 `/Applications/IntelliJ IDEA.app` 是否存在：本地有就走 `local()`（启动快），CI 上 fallback 到 `intellijIdeaCommunity("2025.2.4")`（远端拉 1.8GB）。

## 架构

### 模块边界

- `settings/`（Java）— `YlCompletionSettingsState`（`PersistentStateComponent`，包含 API Key 和各项默认值常量）+ `YlCompletionSettingsConfigurable` / `YlCompletionSettingsComponent`（Tools 设置页、tooltip、测试连接、恢复默认）
- `llm/`（Java）— `DeepSeekFimClient` 用 JDK 21 `HttpClient` + Gson 直接打 `/completions`，错误分类为 `LlmException.Kind`（AUTH / TIMEOUT / NETWORK / RATE_LIMITED / SERVER / BAD_REQUEST / UNKNOWN），并解析响应里的 `model` / `usage`
- `completion/`（Java）— `YlContextBuilder` 切上下文（prefix 70% / suffix 30%，避开 surrogate pair）；`YlTriggerGuard` 决定是否触发；`SuffixDeduplicator` 去掉模型输出和光标后内容的重叠；`YlSuggestionCache` 复用上一次成功 ghost text 的剩余部分
- `status/`（Java）— `YlBusyState` 是应用级单例，承担「在飞计数」和「状态广播」（通过 `MessageBus` Topic 推 IDLE/LOADING/ERROR 给 `YlStatusBarWidget`）；`YlStatusBarWidget` 点击后打开快速弹窗，可切换启用状态并查看当月用量
- `usage/`（Java）— `YlUsageStatsState` 按自然月持久化请求数、token 和按模型聚合的估算成本；`YlPricing` 维护 DeepSeek 模型价格估算
- `ghost/`（Kotlin）— `YlInlineCompletionProvider` 实现 IntelliJ Platform 的 `InlineCompletionProvider`（`suspend` 函数 + value class 返回值，所以**必须** Kotlin）；`YlTriggerInlineCompletionAction` 是手动触发 Action（默认 `Alt+\`）

### 一次补全请求的关键链路

```
DocumentChange/DirectCall/LookupChange (event)
  → YlInlineCompletionProvider.isEnabled()       // settings.enabled + event 类型筛
  → ReadAction: YlTriggerGuard.shouldTrigger()   // 必须在 read action 内访问 PSI/Editor
  → debounce（仅 DocumentChange，按 settings.debounceMs 延迟）
  → ReadAction: YlContextBuilder.build()         // 切 prefix/suffix
  → YlSuggestionCache.tryHit(...)                // 非 DirectCall 时先尝试复用上次建议
  → settings.apiKey                              // API Key 当前随 PersistentStateComponent 保存
  → YlBusyState.start()                          // 记录一次在飞请求，必须配对 finish*
  → DeepSeekFimClient.complete(...).await()      // 异步 HTTP
  → busy.finishOk/Error/Cancelled                // 务必配对，否则状态栏卡 LOADING
  → YlUsageStatsState.recordCompletion(...)      // 记录 model / token / 成本估算
  → SuffixDeduplicator.dedup(raw, suffix)        // 去重，可能返回 null
  → YlSuggestionCache.update(...)                // 成功返回后缓存本次建议
  → InlineCompletionGrayTextElement(deduped)
```

### 不可违反的不变量

- **PSI / Editor 访问必须在 `ReadAction.compute {}` 里**，参考 `YlInlineCompletionProvider.getSuggestion`。直接在 `suspend` 协程里访问会 throw。
- **`YlBusyState.start()` 后，所有出口路径都必须调用 `finishOk()` / `finishError()` / `finishCancelled()` 之一** — 漏配对会把状态栏卡在 LOADING；协程取消必须走 `finishCancelled()`，不要标成 ERROR。
- **`YlBusyState` 不再做单飞守门** — 新输入触发时依赖 IntelliJ inline completion 框架取消旧 `getSuggestion` 协程；不要恢复成 CAS 丢弃新请求，除非同步修改缓存、状态栏和取消语义。
- **API Key 当前保存在 `YlCompletionSettingsState.apiKey`**，会随插件配置 XML 持久化；如果要改回系统 Keychain / `PasswordSafe`，必须同步设置页 dirty 逻辑、测试连接逻辑、Provider 读取逻辑和本文档。
- **`YlSuggestionCache` 只缓存上一次成功建议**，命中依赖 filePath、caret、prefixTail 和用户已接受文本一致；读取 document text 仍要在 read action 内完成。
- **`request.endOffset` 是光标位置**（不是 `startOffset`），切上下文要用它。

### 配置改动需要同步的地方

新增设置项时需要改动：
1. `YlCompletionSettingsState`（state 字段 + 默认值）
2. `YlCompletionSettingsComponent`（UI 控件）
3. `YlCompletionSettingsConfigurable`（apply / reset / isModified 三个钩子）
4. `src/main/resources/messages/YlMessageBundle.properties`（label / tooltip / 按钮或状态文案）

`plugin.xml` 中已注册的扩展点：`applicationService` × 4（settings / llm / busy / usage）/ `applicationConfigurable` / `inline.completion.provider` / `statusBarWidgetFactory`。新增服务记得在这里挂上。

## 业务规则

- 业务代码默认 **Java 21**（享受 `record`、switch 表达式、`HttpClient`）；**仅当 IntelliJ Platform API 强制要求 Kotlin**（如 `suspend` + value class）时才用 Kotlin，目前只有 `ghost/` 包符合。
- `i18n` 文本统一通过 `YlMessageBundle` + `src/main/resources/messages/YlMessageBundle.properties`。
- DeepSeek FIM 协议同时利用 `prompt`（光标前）+ `suffix`（光标后），不要退化成纯前缀续写。`stop` 默认设为 `["\n\n"]`。
