package com.projectronin.interop.rcdm.validate.profile.observation

import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import org.springframework.stereotype.Component

@Component
class RoninStagingRelatedValidator(registryClient: NormalizationRegistryClient) :
    BaseRoninObservationProfileValidator(registryClient) {
    override val profile: RoninProfile = RoninProfile.OBSERVATION_STAGING_RELATED
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_37_0
    override val profileVersion: Int = 4

    override fun validateSpecificObservation(
        resource: Observation,
        parentContext: LocationContext,
        validation: Validation,
    ) {
        validation.apply {
            checkTrue(
                resource.category.any { category -> category.coding.isNotEmpty() },
                FHIRError(
                    code = "RONIN_STAGING_OBS_002",
                    severity = ValidationIssueSeverity.ERROR,
                    description = "Coding is required",
                    location = LocationContext(Observation::category),
                ),
                parentContext,
            )
        }
    }
}
