package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.resource.Participant
import com.projectronin.interop.fhir.r4.validate.resource.R4AppointmentValidator
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import com.projectronin.interop.fhir.r4.valueset.ParticipationStatus
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.validate.profile.util.requiredIdentifiers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninAppointmentValidatorTest {
    private val validator = RoninAppointmentValidator()

    @Test
    fun `returns supported resource`() {
        assertEquals(Appointment::class, validator.supportedResource)
    }

    @Test
    fun `returns R4 validator`() {
        assertEquals(R4AppointmentValidator, validator.r4Validator)
    }

    @Test
    fun `returns profile`() {
        assertEquals(RoninProfile.APPOINTMENT, validator.profile)
    }

    @Test
    fun `validate succeeds`() {
        val appointment = Appointment(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.APPOINTMENT.canonical)),
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_APPOINTMENT_STATUS.uri,
                    value = DynamicValue(
                        type = DynamicValueType.CODING,
                        value = Coding(code = Code("garbo"))
                    )
                )

            ),
            identifier = requiredIdentifiers,
            status = Code(AppointmentStatus.BOOKED.code),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/123".asFHIR()),
                    status = Code(ParticipationStatus.ACCEPTED.code)
                )
            )
        )

        val validation = validator.validate(appointment, LocationContext(Appointment::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate fails with bad extension uri`() {
        val appointment = Appointment(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.APPOINTMENT.canonical)),
            extension = listOf(
                Extension(
                    url = Uri("google.biz"),
                    value = DynamicValue(
                        type = DynamicValueType.CODING,
                        value = Coding(code = Code("garbo"))
                    )
                )

            ),
            identifier = requiredIdentifiers,
            status = Code(AppointmentStatus.BOOKED.code),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/123".asFHIR()),
                    status = Code(ParticipationStatus.ACCEPTED.code)
                )
            )
        )

        val validation = validator.validate(appointment, LocationContext(Appointment::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_APPT_002: Tenant source appointment status extension is missing or invalid @ Appointment.status",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails with bad extension value`() {
        val appointment = Appointment(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.APPOINTMENT.canonical)),
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_APPOINTMENT_STATUS.uri,
                    value = DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value = Reference("Garbo".asFHIR())
                    )
                )

            ),
            identifier = requiredIdentifiers,
            status = Code(AppointmentStatus.BOOKED.code),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/123".asFHIR()),
                    status = Code(ParticipationStatus.ACCEPTED.code)
                )
            )
        )

        val validation = validator.validate(appointment, LocationContext(Appointment::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_APPT_002: Tenant source appointment status extension is missing or invalid @ Appointment.status",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails with missing extension value`() {
        val appointment = Appointment(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.APPOINTMENT.canonical)),
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_APPOINTMENT_STATUS.uri,
                    value = null
                )

            ),
            identifier = requiredIdentifiers,
            status = Code(AppointmentStatus.BOOKED.code),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/123".asFHIR()),
                    status = Code(ParticipationStatus.ACCEPTED.code)
                )
            )
        )

        val validation = validator.validate(appointment, LocationContext(Appointment::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_APPT_002: Tenant source appointment status extension is missing or invalid @ Appointment.status",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails with missing extension uri`() {
        val appointment = Appointment(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.APPOINTMENT.canonical)),
            extension = listOf(
                Extension(
                    url = null,
                    value = null
                )

            ),
            identifier = requiredIdentifiers,
            status = Code(AppointmentStatus.BOOKED.code),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/123".asFHIR()),
                    status = Code(ParticipationStatus.ACCEPTED.code)
                )
            )
        )

        val validation = validator.validate(appointment, LocationContext(Appointment::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_APPT_002: Tenant source appointment status extension is missing or invalid @ Appointment.status",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails with missing extension`() {
        val appointment = Appointment(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.APPOINTMENT.canonical)),
            extension = emptyList(),
            identifier = requiredIdentifiers,
            status = Code(AppointmentStatus.BOOKED.code),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/123".asFHIR()),
                    status = Code(ParticipationStatus.ACCEPTED.code)
                )
            )
        )

        val validation = validator.validate(appointment, LocationContext(Appointment::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_APPT_001: Appointment extension list may not be empty @ Appointment.status",
            validation.issues().first().toString()
        )
    }
}
