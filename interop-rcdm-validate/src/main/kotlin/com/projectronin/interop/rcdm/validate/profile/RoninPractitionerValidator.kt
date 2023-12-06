package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.fhir.r4.validate.resource.R4PractitionerValidator
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import com.projectronin.interop.fhir.validate.ProfileValidator as R4ProfileValidator

@Component
class RoninPractitionerValidator : ProfileValidator<Practitioner>() {
    override val supportedResource: KClass<Practitioner> = Practitioner::class
    override val r4Validator: R4ProfileValidator<Practitioner> = R4PractitionerValidator
    override val profile: RoninProfile = RoninProfile.PRACTITIONER
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 2

    private val requiredNameError = RequiredFieldError(Practitioner::name)
    private val requiredNameFamilyError = RequiredFieldError(HumanName::family)
    override fun validate(resource: Practitioner, validation: Validation, context: LocationContext) {
        validation.apply {
            checkTrue(resource.name.isNotEmpty(), requiredNameError, context)

            resource.name.forEachIndexed { index, name ->
                val currentContext = context.append(LocationContext("Practitioner", "name[$index]"))
                checkNotNull(name.family, requiredNameFamilyError, currentContext)
            }
        }
    }
}
