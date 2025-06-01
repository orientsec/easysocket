plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.orientsec.easysocket.demo"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.orientsec.easysocket.demo"
        minSdk = 21
        lint.targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        targetCompatibility = JavaVersion.VERSION_1_8
        sourceCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraint.layout)
    implementation(libs.kotlin.stdlib)
    implementation(project(":lib"))
}