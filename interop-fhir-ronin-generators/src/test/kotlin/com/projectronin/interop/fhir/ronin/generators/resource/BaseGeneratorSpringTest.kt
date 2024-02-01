package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.rcdm.common.validation.ValidationClient
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.validate.ValidationService
import com.projectronin.interop.rcdm.validate.spring.ValidationSpringConfig
import io.ktor.client.HttpClient
import io.mockk.mockk
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

const val TENANT_MNEMONIC = "test"
val normalizationRegistryClient = mockk<NormalizationRegistryClient>()

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestConfig::class, ValidationSpringConfig::class])
abstract class BaseGeneratorSpringTest {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    val service by lazy { applicationContext.getBean<ValidationService>() }
}

@Configuration
class TestConfig {
    @Bean
    @Primary
    fun validationClient() = mockk<ValidationClient>(relaxed = true)

    @Bean
    fun httpClient() = mockk<HttpClient>(relaxed = true)

    @Bean
    @Primary
    fun normalizationClient() = normalizationRegistryClient
}
