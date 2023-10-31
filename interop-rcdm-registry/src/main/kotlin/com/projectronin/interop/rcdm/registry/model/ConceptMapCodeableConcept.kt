package com.projectronin.interop.rcdm.registry.model

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Extension

data class ConceptMapCodeableConcept(
    val codeableConcept: CodeableConcept,
    val extension: Extension,
    val metadata: List<ConceptMapMetadata>
)
