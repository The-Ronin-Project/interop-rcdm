package com.projectronin.interop.rcdm.transform.spring

import com.projectronin.interop.rcdm.common.spring.RCDMCommonSpringConfig
import com.projectronin.interop.rcdm.registry.spring.RegistrySpringConfig
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ComponentScan("com.projectronin.interop.rcdm.transform")
@Import(RCDMCommonSpringConfig::class, RegistrySpringConfig::class)
class TransformSpringConfig
