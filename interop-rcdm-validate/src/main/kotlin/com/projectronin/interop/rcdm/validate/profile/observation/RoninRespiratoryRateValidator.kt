package com.projectronin.interop.rcdm.validate.profile.observation

import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import org.springframework.stereotype.Component

@Component
class RoninRespiratoryRateValidator(registryClient: NormalizationRegistryClient) :
    BaseRoninVitalSignProfileValidator(registryClient) {
    override val profile: RoninProfile = RoninProfile.OBSERVATION_RESPIRATORY_RATE
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_37_0
    override val profileVersion: Int = 5

    override fun validateVitalSign(
        resource: Observation,
        parentContext: LocationContext,
        validation: Validation,
    ) {
        validateVitalSignValue(resource.value, listOf("/min"), validation)
    }
}
