package com.projectronin.interop.rcdm.registry.model

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Extension

data class ConceptMapCoding(
    val coding: Coding,
    val extension: Extension,
    val metadata: List<ConceptMapMetadata>? = listOf()
)
