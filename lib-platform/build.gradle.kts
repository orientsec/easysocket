import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }
    
    iosArm64()
    iosSimulatorArm64()
    
    jvm()
    // iOS targets can be added here later

    sourceSets {
        commonMain.dependencies {
            implementation(project(":lib"))
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

configure<LibraryExtension> {
    namespace = "com.orientsec.easysocket.platform"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
