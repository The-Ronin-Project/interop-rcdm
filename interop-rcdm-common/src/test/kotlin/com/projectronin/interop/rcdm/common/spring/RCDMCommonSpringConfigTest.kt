package com.projectronin.interop.rcdm.common.spring

import com.projectronin.interop.common.http.auth.InteropAuthenticationService
import com.projectronin.interop.common.http.spring.HttpSpringConfig
import com.projectronin.interop.rcdm.common.validation.ValidationClient
import com.projectronin.interop.validation.client.CommentClient
import com.projectronin.interop.validation.client.IssueClient
import com.projectronin.interop.validation.client.ResourceClient
import com.projectronin.interop.validation.client.auth.ValidationAuthenticationConfig
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [RCDMCommonSpringConfig::class, TestConfig::class, HttpSpringConfig::class])
class RCDMCommonSpringConfigTest {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun `loads ValidationClient`() {
        val service = applicationContext.getBean<ValidationClient>()
        assertNotNull(service)
        assertInstanceOf(ValidationClient::class.java, service)
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
    @Qualifier(ValidationAuthenticationConfig.AUTH_SERVICE_BEAN_NAME)
    fun validationAuthenticationService() = mockk<InteropAuthenticationService>(relaxed = true)
}
