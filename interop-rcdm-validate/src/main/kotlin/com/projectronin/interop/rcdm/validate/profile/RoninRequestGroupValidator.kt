package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.resource.RequestGroup
import com.projectronin.interop.fhir.r4.validate.resource.R4RequestGroupValidator
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import com.projectronin.interop.fhir.validate.ProfileValidator as R4ProfileValidator

@Component
class RoninRequestGroupValidator : ProfileValidator<RequestGroup>() {
    override val supportedResource: KClass<RequestGroup> = RequestGroup::class
    override val r4Validator: R4ProfileValidator<RequestGroup> = R4RequestGroupValidator
    override val profile: RoninProfile = RoninProfile.REQUEST_GROUP
    override val rcdmVersion = RCDMVersion.V3_22_1
    override val profileVersion = 1
    val requiredSubject = RequiredFieldError(RequestGroup::subject)

    override fun validate(
        resource: RequestGroup,
        validation: Validation,
        context: LocationContext,
    ) {
        validation.apply {
            checkNotNull(resource.subject, requiredSubject, context)
        }
    }
}
