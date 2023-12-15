plugins {
    alias(libs.plugins.interop.spring)
    alias(libs.plugins.interop.junit)
}

dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":interop-rcdm-common"))
    implementation(project(":interop-rcdm-registry"))

    implementation(libs.interop.common)
    implementation(libs.interop.commonJackson)
    implementation(libs.interop.fhir)
    implementation(libs.interop.fhir.generators)
    implementation(libs.ronin.test.data.generator)

    testImplementation(libs.mockk)
    testImplementation(project(":interop-rcdm-validate"))
    testImplementation("org.springframework:spring-test")
    testImplementation(libs.interop.commonHttp) // only needed to mock stubs
}
