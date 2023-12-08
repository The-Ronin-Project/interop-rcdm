package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.event.interop.internal.v1.ResourceType
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationRequestValidator
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.validate.util.validateMedicationDatatype
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import com.projectronin.interop.fhir.validate.ProfileValidator as R4ProfileValidator

@Component
class RoninMedicationRequestValidator : ProfileValidator<MedicationRequest>() {
    override val supportedResource: KClass<MedicationRequest> = MedicationRequest::class
    override val r4Validator: R4ProfileValidator<MedicationRequest> = R4MedicationRequestValidator
    override val profile: RoninProfile = RoninProfile.MEDICATION_REQUEST
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_29_0
    override val profileVersion: Int = 3

    private val requiredRequesterError = RequiredFieldError(MedicationRequest::requester)
    private val requiredMedicationReferenceError =
        FHIRError(
            code = "RONIN_MEDREQ_001",
            description = "Medication must be a Reference",
            severity = ValidationIssueSeverity.ERROR,
            location = LocationContext(MedicationRequest::medication),
        )

    override fun validate(
        resource: MedicationRequest,
        validation: Validation,
        context: LocationContext,
    ) {
        validation.apply {
            checkNotNull(resource.requester, requiredRequesterError, context)

            validateMedicationDatatype(resource.extension, context, this)

            resource.medication?.let { medication ->
                checkTrue(
                    medication.type == DynamicValueType.REFERENCE,
                    requiredMedicationReferenceError,
                    context,
                )
            }

            validateReferenceType(
                resource.subject,
                listOf(ResourceType.Patient),
                LocationContext(MedicationRequest::subject),
                this,
            )
        }
    }
}
