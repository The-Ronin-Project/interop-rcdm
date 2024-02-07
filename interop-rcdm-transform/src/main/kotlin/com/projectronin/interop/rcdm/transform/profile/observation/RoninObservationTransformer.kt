package com.projectronin.interop.rcdm.transform.profile.observation

import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import org.springframework.stereotype.Component

@Component
class RoninObservationTransformer(registryClient: NormalizationRegistryClient) :
    BaseRoninObservationTransformer(registryClient) {
    override val profile: RoninProfile = RoninProfile.OBSERVATION
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_37_0
    override val profileVersion: Int = 6

    override val isDefault: Boolean = true

    override fun qualifies(resource: Observation): Boolean = true
}
