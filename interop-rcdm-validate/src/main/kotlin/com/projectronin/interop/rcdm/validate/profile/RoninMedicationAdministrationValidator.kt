package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.resource.MedicationAdministration
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationAdministrationValidator
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.validate.util.validateMedicationDatatype
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import com.projectronin.interop.fhir.validate.ProfileValidator as R4ProfileValidator

@Component
class RoninMedicationAdministrationValidator : ProfileValidator<MedicationAdministration>() {
    override val supportedResource: KClass<MedicationAdministration> = MedicationAdministration::class
    override val r4Validator: R4ProfileValidator<MedicationAdministration> = R4MedicationAdministrationValidator
    override val profile: RoninProfile = RoninProfile.MEDICATION_ADMINISTRATION
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_31_0
    override val profileVersion: Int = 3

    private val requiredCategoryError = FHIRError(
        code = "RONIN_MEDADMIN_001",
        description = "More than one category cannot be present if category is not null",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(MedicationAdministration::category)
    )

    private val requiredMedicationReferenceError = FHIRError(
        code = "RONIN_MEDADMIN_002",
        description = "Medication must be a Reference",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(MedicationAdministration::medication)
    )
    private val invalidMedicationAdministrationStatusExtensionError = FHIRError(
        code = "RONIN_MEDADMIN_003",
        severity = ValidationIssueSeverity.ERROR,
        description = "Tenant source medication administration status extension is missing or invalid",
        location = LocationContext(MedicationAdministration::extension)
    )

    override fun validate(resource: MedicationAdministration, validation: Validation, context: LocationContext) {
        validation.apply {
            validateMedicationDatatype(resource.extension, context, this)
            resource.medication?.let { medication ->
                checkTrue(
                    medication.type == DynamicValueType.REFERENCE,
                    requiredMedicationReferenceError,
                    context
                )
            }

            // category can only be of size 1 if it exists/is populated
            ifNotNull(resource.category) {
                val categorySize = resource.category?.coding?.size == 1
                checkTrue(
                    categorySize,
                    requiredCategoryError,
                    context
                )
            }

            // extension - status tenant source extension - 1..1
            checkTrue(
                resource.extension.any {
                    it.url?.value == RoninExtension.TENANT_SOURCE_MEDICATION_ADMINISTRATION_STATUS.value &&
                        it.value?.type == DynamicValueType.CODING
                },
                invalidMedicationAdministrationStatusExtensionError,
                context
            )
        }
    }
}
