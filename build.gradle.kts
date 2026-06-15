plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "com.lyl"
// 优先使用环境变量 PLUGIN_VERSION（GitHub Actions 由 git tag 注入）。
// 本地默认 1.0-SNAPSHOT。
version = providers.environmentVariable("PLUGIN_VERSION").orElse("1.0-SNAPSHOT").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// 本地有 IDEA.app 时走 local（启动快、无需下载 1.8GB），
// 否则 fallback 到从 JetBrains 拉远端版本（CI 上必走这条路）。
val localIdeaPath = "/Applications/IntelliJ IDEA.app"
val useLocalIdea = file(localIdeaPath).exists()

dependencies {
    intellijPlatform {
        if (useLocalIdea) {
            local(localIdeaPath)
        } else {
            intellijIdeaCommunity("2025.2.4")
        }
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252.25557"
        }

        changeNotes = """
            Initial version
        """.trimIndent()
    }
}

kotlin {
    jvmToolchain(21)
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
}
