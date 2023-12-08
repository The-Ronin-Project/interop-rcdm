package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.resource.CarePlan
import com.projectronin.interop.fhir.r4.validate.resource.R4CarePlanValidator
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import com.projectronin.interop.fhir.validate.ProfileValidator as R4ProfileValidator

@Component
class RoninCarePlanValidator : ProfileValidator<CarePlan>() {
    override val supportedResource: KClass<CarePlan> = CarePlan::class
    override val r4Validator: R4ProfileValidator<CarePlan> = R4CarePlanValidator
    override val profile: RoninProfile = RoninProfile.CARE_PLAN
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_27_0
    override val profileVersion: Int = 7

    override fun validate(
        resource: CarePlan,
        validation: Validation,
        context: LocationContext,
    ) {
        // Nothing explicit to validate
    }
}
