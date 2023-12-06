package com.projectronin.interop.rcdm.registry.spring

import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [RegistrySpringConfig::class, TestConfig::class])
class RegistrySpringConfigTest {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun `loads NormalizationRegistryClient`() {
        val service = applicationContext.getBean<NormalizationRegistryClient>()
        assertNotNull(service)
        assertInstanceOf(NormalizationRegistryClient::class.java, service)
    }
}

@Configuration
class TestConfig {
    @Bean
    fun threadPoolTaskExecutor() = mockk<ThreadPoolTaskExecutor>(relaxed = true)
}
