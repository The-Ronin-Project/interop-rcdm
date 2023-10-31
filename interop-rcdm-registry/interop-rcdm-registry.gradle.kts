plugins {
    alias(libs.plugins.interop.spring)
    alias(libs.plugins.interop.junit)
}

dependencies {
    implementation(project(":interop-rcdm-common"))

    implementation(libs.caffeine)
    implementation(libs.interop.common)
    implementation(libs.interop.commonJackson)
    implementation(libs.interop.publishers.datalake)
    implementation(libs.interop.fhir)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.mockk)
}
