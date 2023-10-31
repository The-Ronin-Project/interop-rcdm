plugins {
    alias(libs.plugins.interop.publish) apply false
    alias(libs.plugins.interop.spring) apply false
    alias(libs.plugins.interop.version)
    alias(libs.plugins.interop.junit) apply false
    alias(libs.plugins.interop.version.catalog)
    alias(libs.plugins.interop.sonarqube)
}

subprojects {
    apply(plugin = "com.projectronin.interop.gradle.publish")
}
