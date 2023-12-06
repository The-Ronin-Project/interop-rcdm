package com.projectronin.interop.rcdm.common.spring

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(
    *[
        "com.projectronin.interop.rcdm.common",
        "com.projectronin.interop.validation.client" // INT-2128 modularize validation
    ]
)
class RCDMCommonSpringConfig
