import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)
}

kotlin {
    // AGP 9 KMP 库插件：Android target 的配置在 kotlin.android 块内声明，
    // 不再使用 com.android.library + androidTarget + configure<LibraryExtension>
    android {
        namespace = "com.orientsec.easysocket.platform"
        compileSdk = 37
        minSdk = 24
    }

    iosArm64()
    iosSimulatorArm64()

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":library:core"))
            implementation(libs.kotlinx.coroutines.core)
        }

        // 创建一个中间源集，用于存放 Android 和 JVM 共享的 Java Socket 实现。
        // 注意：手动 dependsOn 边会停用默认层级模板（已通过
        // kotlin.mpp.applyDefaultHierarchyTemplate=false 显式关闭），
        // 因此下方必须完整声明所有源集的层级关系，包括 iOS。
        val jvmCommonMain = create("jvmCommonMain") {
            dependsOn(commonMain.get())
        }

        androidMain.get().dependsOn(commonMain.get())
        androidMain.get().dependsOn(jvmCommonMain)
        jvmMain.get().dependsOn(commonMain.get())
        jvmMain.get().dependsOn(jvmCommonMain)

        val iosMain = create("iosMain") {
            dependsOn(commonMain.get())
        }
        iosArm64Main.get().dependsOn(iosMain)
        iosSimulatorArm64Main.get().dependsOn(iosMain)
    }
}

configure<MavenPublishBaseExtension> {
    // 为所有 target publication（kotlinMultiplatform/jvm/iosXxx/androidRelease）
    // 统一设置坐标。不要用 publishing{} 手动注册 publication：那会产生一个
    // 空壳 artifact，而真正的 KMP 产物仍落在默认的错误坐标上。
    coordinates("com.orientsec.easysocket", "platform", "1.0.0")
}
