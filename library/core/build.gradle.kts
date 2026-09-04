import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)
}

kotlin {
    applyDefaultHierarchyTemplate()

    // AGP 9 KMP 库插件：Android target 的配置在 kotlin.android 块内声明，
    // 不再使用 com.android.library + androidTarget + configure<LibraryExtension>
    android {
        namespace = "com.orientsec.easysocket"
        compileSdk = 37
        minSdk = 24
    }

    iosArm64()
    iosSimulatorArm64()

    // 增加编译器选项以压制 expect/actual class 警告
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.network)
            implementation(libs.ktor.network.tls)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.annotation)
            implementation(libs.androidx.lifecycle.common.jvm)
            implementation(libs.androidx.lifecycle.process)
        }
        jvmMain.dependencies {
        }
    }
}

configure<MavenPublishBaseExtension> {
    // 为所有 target publication（kotlinMultiplatform/jvm/iosXxx/androidRelease）
    // 统一设置坐标。不要用 publishing{} 手动注册 publication：那会产生一个
    // 空壳 artifact，而真正的 KMP 产物仍落在默认的错误坐标上。
    coordinates("com.orientsec.easysocket", "core", "1.0.0")
}
