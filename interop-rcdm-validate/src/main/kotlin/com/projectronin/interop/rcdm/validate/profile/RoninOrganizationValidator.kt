package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.resource.Organization
import com.projectronin.interop.fhir.r4.validate.resource.R4OrganizationValidator
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import com.projectronin.interop.fhir.validate.ProfileValidator as R4ProfileValidator

@Component
class RoninOrganizationValidator : ProfileValidator<Organization>() {
    override val supportedResource: KClass<Organization> = Organization::class
    override val r4Validator: R4ProfileValidator<Organization> = R4OrganizationValidator
    override val profile = RoninProfile.ORGANIZATION
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 2

    private val requiredActiveFieldError = RequiredFieldError(Organization::active)
    private val requiredNameFieldError = RequiredFieldError(Organization::name)

    override fun validate(
        resource: Organization,
        validation: Validation,
        context: LocationContext,
    ) {
        validation.apply {
            checkNotNull(resource.active, requiredActiveFieldError, context)

            checkNotNull(resource.name, requiredNameFieldError, context)
        }
    }
}
