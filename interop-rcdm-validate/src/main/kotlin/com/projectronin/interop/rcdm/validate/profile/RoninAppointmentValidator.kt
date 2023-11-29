package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.validate.resource.R4AppointmentValidator
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
class RoninAppointmentValidator : ProfileValidator<Appointment>() {
    override val supportedResource: KClass<Appointment> = Appointment::class
    override val r4Validator: R4ProfileValidator<Appointment> = R4AppointmentValidator
    override val profile = RoninProfile.APPOINTMENT
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 2

    private val requiredAppointmentExtensionError = FHIRError(
        code = "RONIN_APPT_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "Appointment extension list may not be empty",
        location = LocationContext(Appointment::status)
    )
    private val invalidAppointmentStatusExtensionError = FHIRError(
        code = "RONIN_APPT_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "Tenant source appointment status extension is missing or invalid",
        location = LocationContext(Appointment::status)
    )

    override fun validate(resource: Appointment, validation: Validation, context: LocationContext) {
        validation.apply {
            // extension - not empty - 1..*
            val extension = resource.extension
            checkTrue(extension.isNotEmpty(), requiredAppointmentExtensionError, context)

            // extension - status tenant source extension - 1..1
            if (extension.isNotEmpty()) {
                checkTrue(
                    extension.any {
                        it.url?.value == RoninExtension.TENANT_SOURCE_APPOINTMENT_STATUS.value &&
                            it.value?.type == DynamicValueType.CODING
                    },
                    invalidAppointmentStatusExtensionError,
                    context
                )
            }
        }
    }
}
