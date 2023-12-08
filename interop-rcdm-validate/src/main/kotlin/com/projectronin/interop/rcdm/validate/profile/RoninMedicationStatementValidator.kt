package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.resource.MedicationStatement
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationStatementValidator
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.validate.util.validateMedicationDatatype
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import com.projectronin.interop.fhir.validate.ProfileValidator as R4ProfileValidator

@Component
class RoninMedicationStatementValidator : ProfileValidator<MedicationStatement>() {
    override val supportedResource: KClass<MedicationStatement> = MedicationStatement::class
    override val r4Validator: R4ProfileValidator<MedicationStatement> = R4MedicationStatementValidator
    override val profile: RoninProfile = RoninProfile.MEDICATION_STATEMENT
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_29_0
    override val profileVersion: Int = 3

    private val requiredMedicationReferenceError = FHIRError(
        code = "RONIN_MEDSTAT_001",
        description = "Medication must be a Reference",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(MedicationStatement::medication)
    )

    override fun validate(resource: MedicationStatement, validation: Validation, context: LocationContext) {
        validation.apply {
            validateMedicationDatatype(resource.extension, context, this)

            resource.medication?.let { medication ->
                checkTrue(
                    medication.type == DynamicValueType.REFERENCE,
                    requiredMedicationReferenceError,
                    context
                )
            }
        }
    }
}
