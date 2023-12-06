package com.projectronin.interop.rcdm.registry.spring

import com.projectronin.interop.datalake.spring.DatalakeSpringConfig
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ComponentScan("com.projectronin.interop.rcdm.registry")
@Import(DatalakeSpringConfig::class)
class RegistrySpringConfig
