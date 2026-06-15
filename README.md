<div align="center">

# YL Code Completion

**一款轻量的 IntelliJ IDEA 代码补全插件，提供类 Copilot 的 inline 补全体验，由 [DeepSeek FIM](https://api-docs.deepseek.com/zh-cn/api/create-completion) 驱动。**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![JetBrains Plugin](https://img.shields.io/badge/JetBrains-IntelliJ%20IDEA%202025.2%2B-000?logo=intellijidea)](https://www.jetbrains.com/idea/)
[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![DeepSeek](https://img.shields.io/badge/DeepSeek-FIM%20Beta-1E88E5)](https://api-docs.deepseek.com/zh-cn/api/create-completion)

[功能特性](#-功能特性) · [快速开始](#-快速开始) · [配置说明](#-配置说明) · [架构概览](#-架构概览) · [路线图](#-路线图) · [参与贡献](#-参与贡献)

</div>

---

## ✨ 功能特性

- 👻 **Inline 灰色补全** — 在光标位置实时显示灰色建议
- 🎯 **DeepSeek FIM 协议** — 同时利用光标前的 `prefix` 与光标后的 `suffix`，比纯前缀续写更精准
- 🚦 **状态栏小图标** — 右下角实时反映请求状态（idle / loading / error），点击可直达设置页
- 🔒 **单飞机制** — 已有请求在飞时直接丢弃新请求，避免快速输入打爆 API
- ⌨️ **手动触发快捷键** — 默认 `Option+\`（macOS）/ `Alt+\`（Win/Linux），可在 Keymap 中改
- 🔑 **API Key 安全存储** — 通过 IntelliJ `PasswordSafe` 写入系统 Keychain，绝不落明文配置
- ⚙️ **细粒度配置** — Base URL、模型、温度、上下文预算、debounce、超时、文件后缀黑名单一应俱全

## 📦 安装

> Marketplace 发布**尚未上架**，目前请从源码构建。

```bash
git clone git@github.com:Lee0110/yl-code-completion.git
cd yl-code-completion
./gradlew buildPlugin
```

打包产物会出现在 `build/distributions/yl-code-completion-*.zip`。在 IDE 中通过 `Settings → Plugins → ⚙️ → Install Plugin from Disk...` 安装。

也可以直接启动一个沙箱 IDE 调试：

```bash
./gradlew runIde
```

> **环境要求**：Java 21 · 本机已安装 IntelliJ IDEA 2025.2+，路径默认为 `/Applications/IntelliJ IDEA.app`（可在 `build.gradle.kts` 修改）

## 🚀 快速开始

1. 在 [DeepSeek 开放平台](https://platform.deepseek.com/) 申请 API Key
2. 打开 `Settings / Preferences → Tools → YL Code Completion`
3. 粘贴 API Key，点击 **Test Connection** 验证
4. 打开任意源码文件开始输入，停顿片刻：
   - <kbd>Tab</kbd> 接受建议
   - <kbd>Esc</kbd> 取消建议
   - <kbd>Option</kbd>+<kbd>\\</kbd>（Mac）/ <kbd>Alt</kbd>+<kbd>\\</kbd>（Win/Linux） 手动触发

## ⚙️ 配置说明

设置入口：`Tools → YL Code Completion`

| 字段 | 默认值 | 说明 |
|---|---|---|
| Enable inline completion | `true` | 总开关 |
| Base URL | `https://api.deepseek.com/beta` | DeepSeek FIM 接口前缀 |
| API Key | *（空）* | 存于 `PasswordSafe`，不会写入任何 `*.xml` |
| Model | `deepseek-v4-pro` | DeepSeek 模型 ID |
| Max tokens | `128` | 单次补全 token 上限 |
| Temperature | `0.2` | 越低越确定，越高越发散 |
| Top P | `0.95` | 核采样阈值 |
| Debounce (ms) | `300` | 输入停止多久后触发请求 |
| Request timeout (ms) | `8000` | 单次 HTTP 超时 |
| Max context chars | `4000` | 上下文总预算（光标前 70% / 光标后 30%） |
| Min prefix length to trigger | `1` | 当前行至少多少字符才触发 |
| Disabled file extensions | *（空）* | 逗号分隔，例如 `md,txt,log` |

## 🧩 工作原理

```
   用户输入 ─► debounce ─► 触发守卫 ─► 单飞守门 ─► DeepSeek FIM
                                                        │
   灰色补全  ◄─── suffix 去重 ◄─── 解析 choices[0].text ◄┘
```

- **触发守卫**：跳过禁用后缀、dumb 模式、多光标、Lookup 弹窗、Live Template 展开等场景
- **上下文构建器**：在光标周围切出 prefix（70%）+ suffix（30%），同时避免切到 Unicode surrogate pair
- **suffix 去重**：去掉模型输出与光标后已有内容的重复部分，避免 `}}` 之类的重复尾巴

## 🏗 架构概览

```
src/main/java/com/lyl/ylcodecompletion/
├── settings/   PersistentStateComponent · Configurable · PasswordSafe
├── llm/        DeepSeekFimClient（JDK 21 HttpClient + Gson）
├── completion/ 上下文构建 · 触发守卫 · suffix 去重
└── status/     状态总线 · 状态栏 widget

src/main/kotlin/com/lyl/ylcodecompletion/ghost/
├── YlInlineCompletionProvider.kt        InlineCompletionProvider 实现
└── YlTriggerInlineCompletionAction.kt   手动触发 Action
```

> **为什么 Java 与 Kotlin 混编？** 业务代码主要使用 Java 21，享受 `record`、`switch` 表达式、`HttpClient` 等能力。`ghost/` 包用 Kotlin，是因为 IntelliJ Platform 的 `InlineCompletionProvider` 是 Kotlin `suspend` 接口，返回值还是 value class —— 用 Java 实现需要手写 `Continuation` 与反射 box/unbox，可读性差。

## 🛠 开发指南

```bash
./gradlew build          # 编译 + 打包
./gradlew runIde         # 启动沙箱 IDE 调试插件
./gradlew buildPlugin    # 仅打包，产物在 build/distributions/
```

也可以直接在 IDEA 里使用 `.run/Run IDE with Plugin.run.xml` 运行配置启动。

### 目录结构

```
.
├── src/main/java/com/lyl/ylcodecompletion/    Java 21 源码
├── src/main/kotlin/com/lyl/ylcodecompletion/   Kotlin 源码（仅 ghost/）
├── src/main/resources/META-INF/plugin.xml      插件清单
├── build.gradle.kts                            Gradle 构建脚本
└── LICENSE
```

## 🗺 路线图

- [ ] 流式响应，降低首字延迟
- [ ] 多候选切换
- [ ] 基于 PSI 的语义上下文增强（当前函数、imports、包名）
- [ ] 跨文件检索，引入项目级上下文
- [ ] 本地缓存最近补全
- [ ] 调试用工具窗口（请求日志、耗时、错误统计）
- [ ] 发布到 JetBrains Marketplace

## 🤝 参与贡献

欢迎提 PR！开 PR 前请：

1. 本地跑通 `./gradlew build`
2. 在 `runIde` 沙箱中用真实 DeepSeek Key 实测
3. 新业务代码默认 Java 21；仅在 platform API 强制要求时使用 Kotlin

发现 Bug 请 [提 issue](https://github.com/Lee0110/yl-code-completion/issues)，附上复现步骤与 `idea.log` 截选。

## 📜 开源协议

[MIT](LICENSE) © 2026 Lee0110

## 🙏 致谢

- [JetBrains IntelliJ Platform](https://plugins.jetbrains.com/docs/intellij/) — inline completion API
- [DeepSeek](https://www.deepseek.com/) — FIM 补全模型
- [GitHub Copilot](https://github.com/features/copilot) — 交互体验灵感来源
