package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.resource.PractitionerRole
import com.projectronin.interop.fhir.r4.validate.resource.R4PractitionerRoleValidator
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import com.projectronin.interop.fhir.validate.ProfileValidator as R4ProfileValidator

@Component
class RoninPractitionerRoleValidator : ProfileValidator<PractitionerRole>() {
    override val supportedResource: KClass<PractitionerRole> = PractitionerRole::class
    override val r4Validator: R4ProfileValidator<PractitionerRole> = R4PractitionerRoleValidator
    override val profile = RoninProfile.PRACTITIONER_ROLE
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 2

    override fun validate(resource: PractitionerRole, validation: Validation, context: LocationContext) {
        // Nothing explicit to validate
    }
}
