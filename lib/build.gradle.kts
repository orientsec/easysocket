import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget {
        publishLibraryVariants("release")
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

configure<LibraryExtension> {
    namespace = "com.orientsec.easysocket"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.orientsec"
            artifactId = "easysocket"
            version = "1.0.0"
            // Multiplatform publishing is handled differently, usually done automatically for all targets
        }
    }
}
