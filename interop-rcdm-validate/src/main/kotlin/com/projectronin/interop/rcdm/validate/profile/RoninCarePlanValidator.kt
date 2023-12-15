package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.resource.CarePlan
import com.projectronin.interop.fhir.r4.validate.resource.R4CarePlanValidator
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninExtension
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

    private val categoryListError =
        FHIRError(
            code = "RONIN_CAREPLAN_001",
            description = "CarePlan category list size must match the tenantSourceCarePlanCategory extension list size",
            severity = ValidationIssueSeverity.ERROR,
            location = LocationContext("", ""),
        )

    override fun validate(
        resource: CarePlan,
        validation: Validation,
        context: LocationContext,
    ) {
        validation.apply {
            // check if categories exist
            if (resource.category.isNotEmpty()) {
                // check that careplan category list size and the tenantSourceCarePlanCategory extension list size are equal
                val categoryExtensionList =
                    resource.extension.filter { it.url == RoninExtension.TENANT_SOURCE_CARE_PLAN_CATEGORY.uri }
                checkTrue(
                    categoryExtensionList.size == resource.category.size,
                    categoryListError,
                    context,
                )
            }
        }
    }
}
