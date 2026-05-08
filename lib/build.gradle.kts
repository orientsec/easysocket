plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }
    
    // 增加编译器选项以压制 expect/actual class 警告
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.ktor.network)
                implementation(libs.ktor.network.tls)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.annotation)
                implementation(libs.androidx.lifecycle.common.jvm)
                implementation(libs.androidx.lifecycle.process)
            }
        }
        val jvmMain by getting
    }
}

android {
    namespace = "com.orientsec.easysocket"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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
