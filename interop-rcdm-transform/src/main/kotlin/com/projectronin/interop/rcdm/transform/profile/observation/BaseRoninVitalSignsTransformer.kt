package com.projectronin.interop.rcdm.transform.profile.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient

abstract class BaseRoninVitalSignsTransformer(registryClient: NormalizationRegistryClient) :
    BaseRoninObservationTransformer(registryClient) {
    override fun getQualifyingCategories(): List<Coding> =
        listOf(Coding(system = CodeSystem.OBSERVATION_CATEGORY.uri, code = Code("vital-signs")))
}
