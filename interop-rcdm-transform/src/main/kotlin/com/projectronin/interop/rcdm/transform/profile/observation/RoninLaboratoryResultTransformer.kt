package com.projectronin.interop.rcdm.transform.profile.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import org.springframework.stereotype.Component

@Component
class RoninLaboratoryResultTransformer(registryClient: NormalizationRegistryClient) :
    BaseRoninObservationTransformer(registryClient) {
    override val profile: RoninProfile = RoninProfile.OBSERVATION_LABORATORY_RESULT
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_37_0
    override val profileVersion: Int = 5

    override fun getQualifyingCategories(): List<Coding> =
        listOf(Coding(system = CodeSystem.OBSERVATION_CATEGORY.uri, code = Code("laboratory")))
}
