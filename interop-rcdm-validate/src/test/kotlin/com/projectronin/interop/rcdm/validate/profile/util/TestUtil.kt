package com.projectronin.interop.rcdm.validate.profile.util

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR

val requiredIdentifiers = listOf(
    Identifier(
        type = CodeableConcepts.RONIN_TENANT,
        system = CodeSystem.RONIN_TENANT.uri,
        value = "test".asFHIR()
    ),
    Identifier(
        type = CodeableConcepts.RONIN_FHIR_ID,
        system = CodeSystem.RONIN_FHIR_ID.uri,
        value = "12345".asFHIR()
    ),
    Identifier(
        type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
        system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
        value = "EHR Data Authority".asFHIR()
    )
)
