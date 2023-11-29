package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.PositiveInt
import com.projectronin.interop.fhir.r4.datatype.primitive.UnsignedInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Participant
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.valueset.ParticipationStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.util.dataAuthorityExtension
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoninAppointmentTransformerTest {
    private val transformer = RoninAppointmentTransformer()

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `returns supported resource`() {
        assertEquals(Appointment::class, transformer.supportedResource)
    }

    @Test
    fun `always qualifies`() {
        assertTrue(transformer.qualifies(Appointment(status = Code("ok"), participant = emptyList())))
    }

    @Test
    fun `transforms appointment with all attributes`() {
        val appointment = Appointment(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://hl7.org/fhir/R4/appointment.html")),
                source = Uri("source")
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = com.projectronin.interop.fhir.r4.valueset.NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            contained = listOf(Location(id = Id("67890"))),
            extension = listOf(
                Extension(
                    url = Uri("http://hl7.org/extension-1"),
                    value = DynamicValue(DynamicValueType.STRING, "value")
                ),
                Extension(
                    url = Uri("http://hl7.org/extension-2"),
                    value = DynamicValue(DynamicValueType.BOOLEAN, false)
                ),
                Extension(
                    url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
                    value = DynamicValue(
                        type = DynamicValueType.CODING,
                        value = Coding(
                            system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                            code = Code(value = "input")
                        )
                    )
                )
            ),
            modifierExtension = listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            identifier = listOf(Identifier(value = "id".asFHIR())),
            status = Code(
                "cancelled",
                extension = listOf(
                    Extension(
                        url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
                        value = DynamicValue(
                            type = DynamicValueType.CODING,
                            value = Coding(
                                system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                code = Code(value = "input")
                            )
                        )
                    )
                )
            ),
            cancelationReason = CodeableConcept(text = "cancel reason".asFHIR()),
            serviceCategory = listOf(CodeableConcept(text = "service category".asFHIR())),
            serviceType = listOf(CodeableConcept(text = "service type".asFHIR())),
            specialty = listOf(CodeableConcept(text = "specialty".asFHIR())),
            appointmentType = CodeableConcept(text = "appointment type".asFHIR()),
            reasonCode = listOf(CodeableConcept(text = "reason code".asFHIR())),
            reasonReference = listOf(Reference(display = "reason reference".asFHIR())),
            priority = UnsignedInt(1),
            description = "appointment test".asFHIR(),
            supportingInformation = listOf(Reference(display = "supporting info".asFHIR())),
            start = Instant("2017-01-01T00:00:00Z"),
            end = Instant("2017-01-01T01:00:00Z"),
            minutesDuration = PositiveInt(15),
            slot = listOf(Reference(display = "slot".asFHIR())),
            created = DateTime("2021-11-16"),
            comment = "comment".asFHIR(),
            patientInstruction = "patient instruction".asFHIR(),
            basedOn = listOf(Reference(display = "based on".asFHIR())),
            participant = listOf(
                Participant(
                    actor = Reference(
                        reference = "Practitioner/actor".asFHIR(),
                        type = Uri("Practitioner", extension = dataAuthorityExtension)
                    ),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            ),
            requestedPeriod = listOf(Period(start = DateTime("2021-11-16")))
        )

        val transformResponse = transformer.transform(appointment, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("Appointment", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.APPOINTMENT.value)), source = Uri("source")),
            transformed.meta
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = com.projectronin.interop.fhir.r4.valueset.NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()), transformed.text)
        assertEquals(
            listOf(Location(id = Id("67890"))),
            transformed.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://hl7.org/extension-1"),
                    value = DynamicValue(DynamicValueType.STRING, "value")
                ),
                Extension(
                    url = Uri("http://hl7.org/extension-2"),
                    value = DynamicValue(DynamicValueType.BOOLEAN, false)
                ),
                Extension(
                    url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
                    value = DynamicValue(
                        type = DynamicValueType.CODING,
                        value = Coding(
                            system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                            code = Code(value = "input")
                        )
                    )
                )
            ),
            transformed.extension
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            transformed.modifierExtension
        )
        assertEquals(
            listOf(
                Identifier(value = "id".asFHIR()),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertEquals(
            Code(
                "cancelled",
                extension = listOf(
                    Extension(
                        url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
                        value = DynamicValue(
                            type = DynamicValueType.CODING,
                            value = Coding(
                                system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                code = Code(value = "input")
                            )
                        )
                    )
                )
            ),
            transformed.status
        )
        assertEquals(CodeableConcept(text = "cancel reason".asFHIR()), transformed.cancelationReason)
        assertEquals((listOf(CodeableConcept(text = "service category".asFHIR()))), transformed.serviceCategory)
        assertEquals((listOf(CodeableConcept(text = "service type".asFHIR()))), transformed.serviceType)
        assertEquals((listOf(CodeableConcept(text = "specialty".asFHIR()))), transformed.specialty)
        assertEquals(CodeableConcept(text = "appointment type".asFHIR()), transformed.appointmentType)
        assertEquals(listOf(CodeableConcept(text = "reason code".asFHIR())), transformed.reasonCode)
        assertEquals(listOf(Reference(display = "reason reference".asFHIR())), transformed.reasonReference)
        assertEquals(UnsignedInt(1), transformed.priority)
        assertEquals("appointment test".asFHIR(), transformed.description)
        assertEquals(listOf(Reference(display = "supporting info".asFHIR())), transformed.supportingInformation)
        assertEquals(Instant(value = "2017-01-01T00:00:00Z"), transformed.start)
        assertEquals(Instant(value = "2017-01-01T01:00:00Z"), transformed.end)
        assertEquals(PositiveInt(15), transformed.minutesDuration)
        assertEquals(listOf(Reference(display = "slot".asFHIR())), transformed.slot)
        assertEquals(DateTime(value = "2021-11-16"), transformed.created)
        assertEquals("patient instruction".asFHIR(), transformed.patientInstruction)
        assertEquals(listOf(Reference(display = "based on".asFHIR())), transformed.basedOn)
        assertEquals(
            listOf(
                Participant(
                    actor = Reference(
                        reference = "Practitioner/actor".asFHIR(),
                        type = Uri("Practitioner", extension = dataAuthorityExtension)
                    ),
                    status = com.projectronin.interop.fhir.r4.valueset.ParticipationStatus.ACCEPTED.asCode()
                )
            ),
            transformed.participant
        )
        assertEquals(listOf(Period(start = DateTime(value = "2021-11-16"))), transformed.requestedPeriod)
    }

    @Test
    fun `transform appointment with only required attributes`() {
        val appointment = Appointment(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            extension = listOf(
                Extension(
                    url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
                    value = DynamicValue(
                        type = DynamicValueType.CODING,
                        value = Coding(
                            system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                            code = Code(value = "input")
                        )
                    )
                )
            ),
            status = Code(
                "cancelled",
                extension = listOf(
                    Extension(
                        url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
                        value = DynamicValue(
                            type = DynamicValueType.CODING,
                            value = Coding(
                                system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                code = Code(value = "input")
                            )
                        )
                    )
                )
            ),
            participant = listOf(
                Participant(
                    actor = Reference(
                        reference = "Practitioner/actor".asFHIR(),
                        type = Uri("Practitioner", extension = dataAuthorityExtension)
                    ),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            )
        )

        val transformResponse = transformer.transform(appointment, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource

        assertEquals("Appointment", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.APPOINTMENT.value)), source = Uri("source")),
            transformed.meta
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<Resource<*>>(), transformed.contained)
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
                    value = DynamicValue(
                        type = DynamicValueType.CODING,
                        value = Coding(
                            system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                            code = Code(value = "input")
                        )
                    )
                )
            ),
            transformed.extension
        )
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
        assertEquals(
            listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertEquals(
            Code(
                "cancelled",
                extension = listOf(
                    Extension(
                        url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
                        value = DynamicValue(
                            type = DynamicValueType.CODING,
                            value = Coding(
                                system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                code = Code(value = "input")
                            )
                        )
                    )
                )
            ),
            transformed.status
        )
        assertNull(transformed.cancelationReason)
        assertEquals(listOf<CodeableConcept>(), transformed.serviceCategory)
        assertEquals(listOf<CodeableConcept>(), transformed.serviceType)
        assertEquals(listOf<CodeableConcept>(), transformed.specialty)
        assertNull(transformed.appointmentType)
        assertEquals(listOf<CodeableConcept>(), transformed.reasonCode)
        assertEquals(listOf<Reference>(), transformed.reasonReference)
        assertNull(transformed.priority)
        assertNull(transformed.description)
        assertEquals(listOf<Reference>(), transformed.supportingInformation)
        assertNull(transformed.start)
        assertNull(transformed.end)
        assertNull(transformed.minutesDuration)
        assertEquals(listOf<Reference>(), transformed.slot)
        assertNull(transformed.created)
        assertNull(transformed.patientInstruction)
        assertEquals(listOf<Reference>(), transformed.basedOn)
        assertEquals(
            listOf(
                Participant(
                    actor = Reference(
                        reference = "Practitioner/actor".asFHIR(),
                        type = Uri("Practitioner", extension = dataAuthorityExtension)
                    ),
                    status = com.projectronin.interop.fhir.r4.valueset.ParticipationStatus.ACCEPTED.asCode()
                )
            ),
            transformed.participant
        )
        assertEquals(listOf<Period>(), transformed.requestedPeriod)
    }
}
