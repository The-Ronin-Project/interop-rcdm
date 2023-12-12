plugins {
    alias(libs.plugins.interop.spring)
    alias(libs.plugins.interop.junit)
}

dependencies {
    implementation(project(":interop-rcdm-common"))
    implementation(project(":interop-rcdm-registry"))

    implementation(libs.interop.common)
    implementation(libs.interop.fhir)
    implementation(libs.interop.validation.client)
    implementation(libs.event.interop.resource.internal)

    testImplementation(libs.mockk)
    testImplementation(libs.classgraph)
    testImplementation("org.springframework:spring-test")
}
