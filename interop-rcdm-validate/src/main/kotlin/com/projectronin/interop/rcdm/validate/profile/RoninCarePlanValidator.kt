package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.resource.CarePlan
import com.projectronin.interop.fhir.r4.validate.resource.R4CarePlanValidator
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
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
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_35_0
    override val profileVersion: Int = 8

    private val categoryListError =
        FHIRError(
            code = "RONIN_CAREPLAN_001",
            description = "CarePlan category entries must each contain the tenantSourceCarePlanCategory extension",
            severity = ValidationIssueSeverity.ERROR,
            location = LocationContext(CarePlan::category),
        )

    override fun validate(
        resource: CarePlan,
        validation: Validation,
        context: LocationContext,
    ) {
        validation.apply {
            // check if categories exist
            if (resource.category.isNotEmpty()) {
                // check that each category entry has tenantSourceCarePlanCategory extension
                resource.category.forEach {
                    checkTrue(
                        it.extension.isNotEmpty(),
                        categoryListError,
                        context,
                    )
                }
            }
        }
    }
}
