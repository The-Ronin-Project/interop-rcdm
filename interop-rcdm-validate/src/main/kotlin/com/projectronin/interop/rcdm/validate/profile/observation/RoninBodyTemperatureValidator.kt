package com.projectronin.interop.rcdm.validate.profile.observation

import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import org.springframework.stereotype.Component

@Component
class RoninBodyTemperatureValidator(registryClient: NormalizationRegistryClient) :
    BaseRoninVitalSignProfileValidator(registryClient) {
    override val profile: RoninProfile = RoninProfile.OBSERVATION_BODY_TEMPERATURE
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_26_1
    override val profileVersion: Int = 3

    override fun validateVitalSign(resource: Observation, parentContext: LocationContext, validation: Validation) {
        validateVitalSignValue(resource.value, listOf("Cel", "[degF]"), validation)
    }
}
