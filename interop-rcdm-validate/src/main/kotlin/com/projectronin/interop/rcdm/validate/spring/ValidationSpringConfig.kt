package com.projectronin.interop.rcdm.validate.spring

import com.projectronin.interop.rcdm.common.spring.RCDMCommonSpringConfig
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ComponentScan("com.projectronin.interop.rcdm.validate")
@Import(RCDMCommonSpringConfig::class)
class ValidationSpringConfig
