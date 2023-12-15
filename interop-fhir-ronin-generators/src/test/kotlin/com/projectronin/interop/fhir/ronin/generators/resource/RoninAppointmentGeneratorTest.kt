package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.ParticipantGenerator
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Participant
import com.projectronin.interop.fhir.ronin.generators.util.rcdmReference
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.test.data.generator.collection.ListDataGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class RoninAppointmentGeneratorTest : BaseGeneratorSpringTest() {
    var participant = ListDataGenerator(0, ParticipantGenerator())
    private val providedParticipant =
        listOf(
            Participant(
                status = possibleParticipantStatus.random(),
                type =
                    listOf(
                        CodeableConcept(
                            coding = listOf(Coding(system = Uri("some-system"), code = Code("some-code"))),
                        ),
                    ),
                actor = rcdmReference("Patient", "test-1234"),
            ),
        )

    @Test
    fun `example use for roninAppointment`() {
        // create appointment resource with attributes you need, provide the tenant
        val roninAppointment =
            rcdmAppointment(TENANT_MNEMONIC) {
                // to test an attribute like status - provide the value
                status of Code("testing-this-status")
                serviceCategory of
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = Uri("http://example.org/service-category"),
                                        code = Code("gp"),
                                    ),
                                ),
                        ),
                    )
            }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val roninAppointmentJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninAppointment)

        // Uncomment to take a peek at the JSON
        // println(roninAppointmentJSON)
        assertNotNull(roninAppointmentJSON)
    }

    @Test
    fun `example use for rcdmPatient roninAppointment - missing required fields generated`() {
        // create patient and appointment for tenant
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val roninAppointment = rcdmPatient.rcdmAppointment {}

        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val roninAppointmentJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninAppointment)

        // Uncomment to take a peek at the JSON
        // println(roninAppointmentJSON)
        assertNotNull(roninAppointmentJSON)
        assertNotNull(roninAppointment.meta)
        assertEquals(
            roninAppointment.meta!!.profile[0].value,
            RoninProfile.APPOINTMENT.value,
        )
        assertEquals(3, roninAppointment.identifier.size)
        assertNotNull(roninAppointment.status)
        assertNotNull(roninAppointment.extension)
        assertNotNull(roninAppointment.participant)
        assertNotNull(roninAppointment.id)
        val patientFHIRId = roninAppointment.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant = roninAppointment.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", roninAppointment.id?.value.toString())
        assertEquals(TENANT_MNEMONIC, tenant)
    }

    @Test
    fun `validates rcdm appoinment`() {
        val appointment = rcdmAppointment(TENANT_MNEMONIC) {}
        val validation = service.validate(appointment, TENANT_MNEMONIC).succeeded
        assertTrue(validation)
    }

    @Test
    fun `validates with identifier added`() {
        val appointment =
            rcdmAppointment(TENANT_MNEMONIC) {
                identifier of listOf(Identifier(id = "ID-Id".asFHIR()))
            }
        val validation = service.validate(appointment, TENANT_MNEMONIC).succeeded
        assertTrue(validation)
        assertEquals(4, appointment.identifier.size)
        val ids = appointment.identifier.map { it.id }.toSet()
        assertTrue(ids.contains("ID-Id".asFHIR()))
    }

    @Test
    fun `generates rcdm appointment and validates with appointment status extension`() {
        val appointment =
            rcdmAppointment(TENANT_MNEMONIC) {
                status of Code("booked")
            }
        val validation = service.validate(appointment, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertNotNull(appointment.meta)
        assertNotNull(appointment.identifier)
        assertEquals(3, appointment.identifier.size)
        assertNotNull(appointment.status)
        assertNull(appointment.cancelationReason)
        assertNotNull(appointment.extension)
        // assert that the status value is also the code value in the extension
        assertTrue(appointment.extension[0].value?.value.toString().contains(appointment.status?.value.toString()))
    }

    @Test
    fun `generates rcdm appointment with cancelationReason if status requires`() {
        val appointment =
            rcdmAppointment(TENANT_MNEMONIC) {
                status of Code("noshow")
            }
        val validation = service.validate(appointment, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals(appointment.status?.value, "noshow")
        assertNotNull(appointment.cancelationReason)
    }

    @Test
    fun `generates rcdm appointment with status requiring cancelationReason and keeps provided cancelationReason`() {
        val appointment =
            rcdmAppointment(TENANT_MNEMONIC) {
                status of Code("cancelled")
                cancelationReason of
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = Uri("http://terminology.hl7.org/CodeSystem/appointment-cancellation-reason"),
                                    code = Code("some-code-here"),
                                    display = "some-display-here".asFHIR(),
                                ),
                            ),
                    )
            }
        val validation = service.validate(appointment, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals(appointment.status?.value, "cancelled")
        assertNotNull(appointment.cancelationReason)
        assertEquals(appointment.cancelationReason!!.coding[0].code, Code("some-code-here"))
        assertEquals(appointment.cancelationReason!!.coding[0].display?.value, "some-display-here")
    }

    @Test
    fun `rcdmAppointment - valid participant actor input - validate succeeds`() {
        val appointment =
            rcdmAppointment(TENANT_MNEMONIC) {
                participant of
                    rcdmParticipant(emptyList(), TENANT_MNEMONIC, "Patient", "456")
            }
        val validation = service.validate(appointment, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals(
            "Patient/test-456",
            appointment.participant.first().actor?.reference?.value,
        )
    }

    @Test
    fun `rcdmPatient rcdmAppointment validates`() {
        val patient = rcdmPatient(TENANT_MNEMONIC) {}
        val appointment = patient.rcdmAppointment {}
        val validation = service.validate(appointment, TENANT_MNEMONIC).succeeded
        assertTrue(validation)
    }

    @Test
    fun `rcdmPatient rcdmAppointment - valid participant actor input - adds base patient - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val appointment =
            rcdmPatient.rcdmAppointment {
                participant of
                    rcdmParticipant(emptyList(), TENANT_MNEMONIC, "Practitioner", "456")
            }
        val validation = service.validate(appointment, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertNotNull(appointment.participant.firstOrNull { it.actor?.reference?.value == "Practitioner/test-456" })
        assertNotNull(appointment.participant.firstOrNull { it.actor?.reference?.value?.startsWith("Patient/test-") == true })
    }

    @Test
    fun `rcdmPatient rcdmCarePlan - fhir id input for both - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) { id of "99" }
        val appointment =
            rcdmPatient.rcdmAppointment {
                id of "88"
            }
        val validation = service.validate(appointment, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals(3, appointment.identifier.size)
        val values = appointment.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 3)
        assertTrue(values.contains("88".asFHIR()))
        assertTrue(values.contains(TENANT_MNEMONIC.asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        assertEquals("test-88", appointment.id?.value)
        assertEquals("test-99", rcdmPatient.id?.value)
        assertNotNull(appointment.participant.firstOrNull { it.actor?.reference?.value == "Patient/test-99" })
    }

    @Test
    fun `generate rcdm participant when none is provided using possible participant`() {
        val roninParticipant = rcdmParticipant(participant.generate(), TENANT_MNEMONIC)
        assertNotNull(roninParticipant)
        assertTrue(roninParticipant.first().status in possibleParticipantStatus)
        assertTrue(roninParticipant.first().actor?.decomposedType() in participantActorReferenceOptions)
        assertTrue(roninParticipant.first().actor?.decomposedId()?.startsWith("test-") == true)
    }

    @Test
    fun `generate rcdm participant when null is provided`() {
        val roninParticipant = rcdmParticipant(emptyList(), TENANT_MNEMONIC)
        assertNotNull(roninParticipant)
        assertTrue(roninParticipant.first().status in possibleParticipantStatus)
        assertTrue(roninParticipant.first().actor?.decomposedType() in participantActorReferenceOptions)
        assertTrue(roninParticipant.first().actor?.decomposedId()?.startsWith("test-") == true)
    }

    @Test
    fun `generate rcdm participant when null is provided and a reference type and empty id are input`() {
        val goodResult = rcdmParticipant(emptyList(), TENANT_MNEMONIC, "Practitioner", "")
        assertEquals(1, goodResult.first().type.size)
        assertNotNull(goodResult)
        assertEquals("Practitioner", goodResult.first().actor?.decomposedType())
        assertTrue(goodResult.first().actor?.decomposedId()?.startsWith("test-") == true)
    }

    @Test
    fun `generate rcdm participant when null is provided and a reference type and id are input`() {
        val goodResult = rcdmParticipant(emptyList(), TENANT_MNEMONIC, "Practitioner", "1234")
        assertEquals(1, goodResult.first().type.size)
        assertNotNull(goodResult)
        assertEquals("Practitioner", goodResult.first().actor?.decomposedType())
        assertEquals("test-1234", goodResult.first().actor?.decomposedId())
    }

    @Test
    fun `generate rcdm participant when actor is missing valid type and no reference type is input`() {
        // make a good participant
        val roninParticipantList = rcdmParticipant(emptyList(), TENANT_MNEMONIC)

        // make a bad participant
        val roninParticipant = roninParticipantList.first()
        val badParticipant = roninParticipant.copy(actor = roninParticipant.actor?.copy(reference = "".asFHIR()))

        // generate a good participant on input of the bad participant
        val goodResult = rcdmParticipant(listOf(badParticipant), TENANT_MNEMONIC)
        assertEquals(1, goodResult.first().type.size)
        assertNotNull(goodResult)
        assertTrue(goodResult.first().actor?.decomposedType() in participantActorReferenceOptions)
        assertTrue(goodResult.first().actor?.decomposedId()?.startsWith("test-") == true)
    }

    @Test
    fun `generate rcdm participant when actor is missing valid type and a reference type and id are input`() {
        // make a good participant
        val roninParticipantList = rcdmParticipant(emptyList(), TENANT_MNEMONIC)

        // make a bad participant
        val roninParticipant = roninParticipantList.first()
        val badParticipant = roninParticipant.copy(actor = roninParticipant.actor?.copy(reference = "".asFHIR()))

        // generate a good participant on input of the bad participant
        val goodResult = rcdmParticipant(listOf(badParticipant), TENANT_MNEMONIC, "Practitioner", "1234")
        assertEquals(1, goodResult.first().type.size)
        assertNotNull(goodResult)
        assertEquals("Practitioner", goodResult.first().actor?.decomposedType())
        assertEquals("test-1234", goodResult.first().actor?.decomposedId())
    }

    @Test
    fun `generate rcdm participant when actor is missing valid type and a reference type and empty id are input`() {
        // make a good participant
        val roninParticipantList = rcdmParticipant(emptyList(), TENANT_MNEMONIC)

        // make a bad participant
        val roninParticipant = roninParticipantList.first()
        val badParticipant = roninParticipant.copy(actor = roninParticipant.actor?.copy(reference = "".asFHIR()))

        // generate a good participant on input of the bad participant
        val goodResult = rcdmParticipant(listOf(badParticipant), TENANT_MNEMONIC, "Practitioner", "")
        assertEquals(1, goodResult.first().type.size)
        assertNotNull(goodResult)
        assertEquals("Practitioner", goodResult.first().actor?.decomposedType())
        assertTrue(goodResult.first().actor?.decomposedId()?.startsWith("test-") == true)
    }

    @Test
    fun `generate provided participant`() {
        val roninParticipant = rcdmParticipant(providedParticipant, TENANT_MNEMONIC)
        assertEquals(roninParticipant, providedParticipant)
    }

    @Test
    fun `generate rcdmParticipant when none is provided using possible participant - provide input type and id`() {
        val roninParticipant = rcdmParticipant(participant.generate(), TENANT_MNEMONIC, "Patient", "1234")
        assertNotNull(roninParticipant)
        assertTrue(roninParticipant.first().status in possibleParticipantStatus)
        assertEquals("Patient/test-1234", roninParticipant.first().actor?.reference?.value)
    }

    @Test
    fun `generate rcdmParticipant when none is provided using possible participant - provide input type with no id`() {
        val roninParticipant = rcdmParticipant(participant.generate(), TENANT_MNEMONIC, "Patient")
        assertNotNull(roninParticipant)
        assertTrue(roninParticipant.first().status in possibleParticipantStatus)
        assertTrue(roninParticipant.first().actor?.type?.value in participantActorReferenceOptions)
        assertTrue(roninParticipant.first().actor?.decomposedId()?.startsWith("test-") == true)
    }

    @Test
    fun `generate rcdmParticipant when none is provided using possible participant - provide input type with empty id`() {
        val roninParticipant = rcdmParticipant(participant.generate(), TENANT_MNEMONIC, "Patient", "")
        assertNotNull(roninParticipant)
        assertTrue(roninParticipant.first().status in possibleParticipantStatus)
        assertTrue(roninParticipant.first().actor?.type?.value in participantActorReferenceOptions)
        assertTrue(roninParticipant.first().actor?.decomposedId()?.startsWith("test-") == true)
    }

    @Test
    fun `generate rcdmParticipant when none is provided using possible participant - provide input id with no type`() {
        val roninParticipant = rcdmParticipant(participant.generate(), TENANT_MNEMONIC, null, "1234")
        assertNotNull(roninParticipant)
        assertTrue(roninParticipant.first().status in possibleParticipantStatus)
        assertTrue(roninParticipant.first().actor?.type?.value in participantActorReferenceOptions)
        assertTrue(roninParticipant.first().actor?.decomposedId()?.startsWith("test-") == true)
    }

    @Test
    fun `generate rcdmParticipant when none is provided using possible participant - provide input id with empty type`() {
        val roninParticipant = rcdmParticipant(participant.generate(), TENANT_MNEMONIC, "", "1234")
        assertNotNull(roninParticipant)
        assertTrue(roninParticipant.first().status in possibleParticipantStatus)
        assertTrue(roninParticipant.first().actor?.type?.value in participantActorReferenceOptions)
        assertTrue(roninParticipant.first().actor?.decomposedId()?.startsWith("test-") == true)
    }

    @Test
    fun `generate rcdmParticipant when none is provided using possible participant and there is no type and no id`() {
        val roninParticipant = rcdmParticipant(participant.generate(), TENANT_MNEMONIC)
        assertNotNull(roninParticipant)
        assertTrue(roninParticipant.first().status in possibleParticipantStatus)
        assertTrue(roninParticipant.first().actor?.type?.value in participantActorReferenceOptions)
        assertTrue(roninParticipant.first().actor?.decomposedId()?.startsWith("test-") == true)
    }

    @Test
    fun `generate rcdmParticipant when none is provided using possible participant and there is null type and null id`() {
        val roninParticipant = rcdmParticipant(participant.generate(), TENANT_MNEMONIC, null, null)
        assertNotNull(roninParticipant)
        assertTrue(roninParticipant.first().status in possibleParticipantStatus)
        assertTrue(roninParticipant.first().actor?.type?.value in participantActorReferenceOptions)
        assertTrue(roninParticipant.first().actor?.decomposedId()?.startsWith("test-") == true)
    }

    @Test
    fun `generate rcdmParticipant when none is provided using possible participant and there is null type and empty id`() {
        val roninParticipant = rcdmParticipant(participant.generate(), TENANT_MNEMONIC, null, "")
        assertNotNull(roninParticipant)
        assertTrue(roninParticipant.first().status in possibleParticipantStatus)
        assertTrue(roninParticipant.first().actor?.type?.value in participantActorReferenceOptions)
        assertTrue(roninParticipant.first().actor?.decomposedId()?.startsWith("test-") == true)
    }
}
