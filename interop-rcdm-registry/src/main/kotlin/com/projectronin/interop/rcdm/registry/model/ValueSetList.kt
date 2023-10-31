package com.projectronin.interop.rcdm.registry.model

import com.projectronin.interop.fhir.r4.datatype.Coding

data class ValueSetList(
    val codes: List<Coding>,
    val metadata: ValueSetMetadata? = null
)
