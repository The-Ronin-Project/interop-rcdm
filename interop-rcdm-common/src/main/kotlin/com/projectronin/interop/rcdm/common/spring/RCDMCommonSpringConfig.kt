package com.projectronin.interop.rcdm.common.spring

import com.projectronin.interop.validation.client.spring.ValidationClientSpringConfig
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ComponentScan(
    *[
        "com.projectronin.interop.rcdm.common",
    ],
)
@Import(ValidationClientSpringConfig::class)
class RCDMCommonSpringConfig
