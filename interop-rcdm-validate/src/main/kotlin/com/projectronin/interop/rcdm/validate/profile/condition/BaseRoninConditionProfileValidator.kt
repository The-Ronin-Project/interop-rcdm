package com.projectronin.interop.rcdm.validate.profile.condition

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.r4.validate.resource.R4ConditionValidator
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.util.qualifiesForValueSet
import com.projectronin.interop.rcdm.validate.profile.ProfileValidator
import kotlin.reflect.KClass
import com.projectronin.interop.fhir.validate.ProfileValidator as R4ProfileValidator

abstract class BaseRoninConditionProfileValidator : ProfileValidator<Condition>() {
    override val supportedResource: KClass<Condition> = Condition::class
    override val r4Validator: R4ProfileValidator<Condition> = R4ConditionValidator

    private val requiredCodeError = RequiredFieldError(Condition::code)

    private val requiredConditionCodeExtension =
        FHIRError(
            code = "RONIN_CND_001",
            description = "Tenant source condition code extension is missing or invalid",
            severity = ValidationIssueSeverity.ERROR,
            location = LocationContext(Condition::extension),
        )

    abstract fun getQualifyingCategories(): List<Coding>

    override fun validate(
        resource: Condition,
        validation: Validation,
        context: LocationContext,
    ) {
        validation.apply {
            val qualifyingCategories = getQualifyingCategories()
            checkTrue(
                resource.category.qualifiesForValueSet(qualifyingCategories),
                FHIRError(
                    code = "RONIN_CND_001",
                    severity = ValidationIssueSeverity.ERROR,
                    description = "Must match this system|code: ${
                        qualifyingCategories.joinToString(", ") { "${it.system?.value}|${it.code?.value}" }
                    }",
                    location = LocationContext(Condition::category),
                ),
                context,
            )

            checkNotNull(resource.code, requiredCodeError, context)
            validateRoninNormalizedCodeableConcept(resource.code, Condition::code, null, context, validation)

            checkTrue(
                resource.extension.any {
                    it.url == RoninExtension.TENANT_SOURCE_CONDITION_CODE.uri && it.value?.type == DynamicValueType.CODEABLE_CONCEPT
                },
                requiredConditionCodeExtension,
                context,
            )
        }
    }
}
