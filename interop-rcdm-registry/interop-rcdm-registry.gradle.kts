plugins {
    alias(libs.plugins.interop.spring)
    alias(libs.plugins.interop.junit)
}

dependencies {
    implementation(project(":interop-rcdm-common"))

    implementation(libs.caffeine)
    implementation(libs.dd.trace.api)
    implementation(libs.interop.common)
    implementation(libs.interop.commonJackson)
    implementation(libs.interop.datalake)
    implementation(libs.interop.fhir)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.mockk)
    testImplementation("org.springframework:spring-test")
}
