package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationValidator
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import com.projectronin.interop.fhir.validate.ProfileValidator as R4ProfileValidator
@Component
class RoninMedicationValidator : ProfileValidator<Medication>() {
    override val supportedResource: KClass<Medication> = Medication::class
    override val r4Validator: R4ProfileValidator<Medication> = R4MedicationValidator
    override val profile: RoninProfile = RoninProfile.MEDICATION
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_19_0
    override val profileVersion: Int = 2

    private val requiredCodeError = RequiredFieldError(Medication::code)
    private val requiredExtensionCodeError = FHIRError(
        code = "RONIN_MED_001",
        description = "Tenant source medication code extension is missing or invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(Medication::extension)
    )

    override fun validate(resource: Medication, validation: Validation, context: LocationContext) {
        validation.apply {
            checkNotNull(resource.code, requiredCodeError, context)
            validateRoninNormalizedCodeableConcept(resource.code, Medication::code, null, context, this)
            checkTrue(
                resource.extension.any {
                    it.url == RoninExtension.TENANT_SOURCE_MEDICATION_CODE.uri &&
                        it.value?.type == DynamicValueType.CODEABLE_CONCEPT
                },
                requiredExtensionCodeError,
                context
            )
        }
    }
}
