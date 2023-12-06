package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.resource.Procedure
import com.projectronin.interop.fhir.r4.validate.resource.R4ProcedureValidator
import com.projectronin.interop.fhir.r4.valueset.EventStatus
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
class RoninProcedureValidator : ProfileValidator<Procedure>() {
    override val supportedResource: KClass<Procedure> = Procedure::class
    override val r4Validator: R4ProfileValidator<Procedure> = R4ProcedureValidator
    override val profile = RoninProfile.PROCEDURE
    override val rcdmVersion = RCDMVersion.V3_28_0
    override val profileVersion = 1

    private val requiredExtensionCodeError = FHIRError(
        code = "RONIN_PROC_001",
        description = "Tenant source procedure code extension is missing or invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(Procedure::extension)
    )
    private val requiredExtensionCategoryError = FHIRError(
        code = "RONIN_PROC_002",
        description = "Tenant source procedure category extension is invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(Procedure::extension)
    )
    private val requiredPerformedError = FHIRError(
        code = "USCORE_PROC_001",
        description = "Performed SHALL be present if the status is 'completed' or 'in-progress'",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(Procedure::performed)
    )
    private val requiredCodeError = FHIRError(
        code = "USCORE_PROC_002",
        description = "Procedure code is missing or invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(Procedure::code)
    )

    override fun validate(resource: Procedure, validation: Validation, context: LocationContext) {
        validation.apply {
            if (resource.status?.value == EventStatus.COMPLETED.code || resource.status?.value == EventStatus.IN_PROGRESS.code) {
                checkNotNull(resource.performed, requiredPerformedError, context)
            }
            checkNotNull(resource.code, requiredCodeError, context)

            // extension must include procedure code
            checkTrue(
                resource.extension.any {
                    it.url == RoninExtension.TENANT_SOURCE_PROCEDURE_CODE.uri &&
                        it.value?.type == DynamicValueType.CODEABLE_CONCEPT
                },
                requiredExtensionCodeError,
                context
            )
            // extension may include procedure category
            checkTrue(
                resource.extension.filter { it.url == RoninExtension.TENANT_SOURCE_PROCEDURE_CATEGORY.uri }.size <= 1,
                requiredExtensionCategoryError,
                context
            )
        }
    }
}
