package com.projectronin.interop.rcdm.validate.profile.observation

import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.registry.model.ValueSetList
import org.springframework.stereotype.Component

@Component
class RoninObservationValidator(registryClient: NormalizationRegistryClient) :
    BaseRoninObservationProfileValidator(registryClient) {
    override val profile: RoninProfile = RoninProfile.OBSERVATION
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_37_0
    override val profileVersion: Int = 6

    override fun getSupportedValueSet(): ValueSetList = ValueSetList(emptyList(), null)

    override fun validateSpecificObservation(
        resource: Observation,
        parentContext: LocationContext,
        validation: Validation,
    ) {
    }
}
