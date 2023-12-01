package com.projectronin.interop.rcdm.transform.profile.observation

import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import org.springframework.stereotype.Component

@Component
class RoninBloodPressureTransformer(registryClient: NormalizationRegistryClient) :
    BaseRoninVitalSignsTransformer(registryClient) {
    override val profile: RoninProfile = RoninProfile.OBSERVATION_BLOOD_PRESSURE
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_26_1
    override val profileVersion: Int = 4
}
