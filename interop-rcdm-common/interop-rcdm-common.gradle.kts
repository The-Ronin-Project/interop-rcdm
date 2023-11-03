plugins {
    alias(libs.plugins.interop.junit)
    alias(libs.plugins.interop.spring)
}

dependencies {
    implementation(libs.interop.commonJackson)
    implementation(libs.interop.fhir)
    implementation(libs.interop.validation.client)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.mockk)
}
