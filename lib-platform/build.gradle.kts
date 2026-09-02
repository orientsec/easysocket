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
        androidMain.dependencies {
            implementation(libs.androidx.annotation)
        }
        jvmMain.dependencies {
        }
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
