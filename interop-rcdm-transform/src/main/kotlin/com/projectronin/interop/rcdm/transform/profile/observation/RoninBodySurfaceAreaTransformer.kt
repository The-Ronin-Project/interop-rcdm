package com.projectronin.interop.rcdm.transform.profile.observation

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import org.springframework.stereotype.Component

@Component
class RoninBodySurfaceAreaTransformer(registryClient: NormalizationRegistryClient) :
    BaseRoninObservationTransformer(registryClient) {
    override val profile: RoninProfile = RoninProfile.OBSERVATION_BODY_SURFACE_AREA
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_39_0
    override val profileVersion: Int = 5

    // rely solely on code
    override fun getQualifyingCategories(): List<Coding> = emptyList()
}
