plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.netty.all)
}

application {
    mainClass.set("com.orientsec.Server")
}
