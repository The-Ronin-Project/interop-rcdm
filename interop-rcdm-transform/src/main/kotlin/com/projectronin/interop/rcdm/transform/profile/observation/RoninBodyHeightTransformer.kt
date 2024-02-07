package com.projectronin.interop.rcdm.transform.profile.observation

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import org.springframework.stereotype.Component

@Component
class RoninBodyHeightTransformer(registryClient: NormalizationRegistryClient) :
    BaseRoninVitalSignsTransformer(registryClient) {
    override val profile: RoninProfile = RoninProfile.OBSERVATION_BODY_HEIGHT
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_37_0
    override val profileVersion: Int = 5

    override fun getTransformedBodySite(bodySite: CodeableConcept?): CodeableConcept? = null
}
