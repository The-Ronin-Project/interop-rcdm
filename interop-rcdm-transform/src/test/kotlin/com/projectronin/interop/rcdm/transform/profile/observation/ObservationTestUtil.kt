package com.projectronin.interop.rcdm.transform.profile.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code

val vitalSignsCategoryConceptList = listOf(
    CodeableConcept(
        coding = listOf(
            Coding(
                system = CodeSystem.OBSERVATION_CATEGORY.uri,
                code = Code("vital-signs")
            )
        )
    )
)
