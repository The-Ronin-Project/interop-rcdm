package com.projectronin.interop.rcdm.transform.profile.observation

import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.util.qualifiesForValueSet
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import org.springframework.stereotype.Component

@Component
class RoninStagingRelatedTransformer(registryClient: NormalizationRegistryClient) :
    BaseRoninObservationTransformer(registryClient) {
    override val profile: RoninProfile = RoninProfile.OBSERVATION_STAGING_RELATED
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_26_1
    override val profileVersion: Int = 3

    override fun qualifies(resource: Observation): Boolean {
        // code.coding must exist in the valueSet
        // category and category.coding must be present but are not fixed values
        return resource.code.qualifiesForValueSet(getQualifyingCodes()) &&
            (resource.category.isNotEmpty() && resource.category.any { category -> category.coding.isNotEmpty() })
    }
}
