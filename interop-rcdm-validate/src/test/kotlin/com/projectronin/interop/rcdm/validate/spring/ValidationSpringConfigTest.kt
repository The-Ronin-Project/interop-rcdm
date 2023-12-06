package com.projectronin.interop.rcdm.validate.spring

import com.projectronin.interop.rcdm.validate.ValidationService
import com.projectronin.interop.validation.client.CommentClient
import com.projectronin.interop.validation.client.IssueClient
import com.projectronin.interop.validation.client.ResourceClient
import com.projectronin.interop.validation.client.auth.ValidationAuthenticationService
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
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [ValidationSpringConfig::class, TestConfig::class])
class ValidationSpringConfigTest {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun `loads ValidationService`() {
        val service = applicationContext.getBean<ValidationService>()
        assertNotNull(service)
        assertInstanceOf(ValidationService::class.java, service)
    }
}

@Configuration
class TestConfig {
    @Bean
    fun resourceClient() = mockk<ResourceClient>(relaxed = true)

    @Bean
    fun commentClient() = mockk<CommentClient>(relaxed = true)

    @Bean
    fun issueClient() = mockk<IssueClient>(relaxed = true)

    @Bean
    fun validationAuthenticationService() = mockk<ValidationAuthenticationService>(relaxed = true)
}
