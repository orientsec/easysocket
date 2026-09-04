pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/release")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "easysocket"

// 库层（物理上位于 library/ 目录，逻辑上为 :library:* 二级模块）
include(":library:core")
include(":library:platform")

// demo 层（物理上位于 demo/ 目录，逻辑上为 :demo:* 二级模块）
include(":demo:client")
include(":demo:androidDemo")
include(":demo:server")
