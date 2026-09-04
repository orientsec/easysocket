plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

dependencies {
    implementation(libs.netty.all)
}

application {
    mainClass.set("com.orientsec.Server")
}
