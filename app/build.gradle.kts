import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.application)
}

// 纯 Android 壳模块：只包含 manifest、图标和主题资源。
// Compose UI 与业务代码在 :app-shared（KMP），MainActivity 也位于该库的 androidMain。
// 本模块不含 Kotlin 源码，无需 Kotlin 插件。
configure<ApplicationExtension> {
    namespace = "com.orientsec.easysocket.demo"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.orientsec.easysocket.demo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":app-shared"))
    // 壳模块的 AppTheme（parent 为 Theme.AppCompat）需要 appcompat 参与资源链接
    implementation(libs.androidx.appcompat)
}
