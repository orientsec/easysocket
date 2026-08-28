import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }
    
    jvm()
    // iOS targets can be added here later

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":lib"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.annotation)
            }
        }
        val jvmMain by getting
    }
}

configure<LibraryExtension> {
    namespace = "com.orientsec.easysocket.socket"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
