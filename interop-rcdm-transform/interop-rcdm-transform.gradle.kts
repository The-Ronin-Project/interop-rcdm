plugins {
    alias(libs.plugins.interop.spring)
    alias(libs.plugins.interop.junit)
}

dependencies {
    implementation(project(":interop-rcdm-common"))
    implementation(project(":interop-rcdm-registry"))

    implementation(libs.interop.common)
    implementation(libs.interop.ehr.tenant)
    implementation(libs.interop.fhir)
    implementation(libs.interop.validation.client)

    testImplementation(libs.mockk)
}
