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
class RoninBodyWeightValidator(registryClient: NormalizationRegistryClient) :
    BaseRoninVitalSignProfileValidator(registryClient) {
    override val profile: RoninProfile = RoninProfile.OBSERVATION_BODY_WEIGHT
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_37_0
    override val profileVersion: Int = 5

    private val noBodySiteError =
        FHIRError(
            code = "RONIN_WTOBS_001",
            severity = ValidationIssueSeverity.ERROR,
            description = "bodySite not allowed for Body Weight observation",
            location = LocationContext(Observation::bodySite),
        )

    override fun validateVitalSign(
        resource: Observation,
        parentContext: LocationContext,
        validation: Validation,
    ) {
        validation.apply {
            validateVitalSignValue(resource.value, listOf("kg", "[lb_av]", "g"), this)

            checkTrue(resource.bodySite == null, noBodySiteError, parentContext)
        }
    }
}
