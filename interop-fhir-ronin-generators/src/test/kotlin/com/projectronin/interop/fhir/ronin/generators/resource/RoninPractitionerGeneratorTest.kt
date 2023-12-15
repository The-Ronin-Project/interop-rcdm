package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Qualification
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class RoninPractitionerGeneratorTest : BaseGeneratorSpringTest() {
    @Test
    fun `example use for rcdmPractitioner`() {
        // create practitioner for tenant "test"
        val roninPractitioner1 =
            rcdmPractitioner(TENANT_MNEMONIC) {
                // to test an attribute like telecom - provide the value
                telecom of
                    listOf(
                        ContactPoint(
                            value = "123-456-7890".asFHIR(),
                            system = Code(ContactPointSystem.PHONE.code),
                            use = Code(ContactPointUse.WORK.code),
                        ),
                    )
                // an NPI is optional, but if you want an NPI, generate it like this
                identifier of listOf(rcdmPractitionerNPI())
            }

        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninPractitionerJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninPractitioner1)

        // Uncomment to take a peek at the JSON
        // println(roninPractitionerJSON)
        assertNotNull(roninPractitioner1)
    }

    @Test
    fun `rcdmPractitioner - missing required fields generated`() {
        val roninPractitioner1 = rcdmPractitioner(TENANT_MNEMONIC) {}
        assertNotNull(roninPractitioner1.id)
        assertNotNull(roninPractitioner1.meta)
        assertEquals(
            RoninProfile.PRACTITIONER.value,
            roninPractitioner1.meta!!.profile[0].value,
        )
        assertEquals(3, roninPractitioner1.identifier.size)
        val practitionerFHIRId =
            roninPractitioner1.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenantId =
            roninPractitioner1.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenantId-$practitionerFHIRId", roninPractitioner1.id?.value.toString())
        assertNull(roninPractitioner1.active)
        assertNotNull(roninPractitioner1.name)
        assertEquals(emptyList<ContactPoint>(), roninPractitioner1.telecom)
        assertEquals(emptyList<Address>(), roninPractitioner1.address)
        assertEquals(emptyList<Qualification>(), roninPractitioner1.qualification)
        assertEquals(emptyList<CodeableConcept>(), roninPractitioner1.communication)
    }

    @Test
    fun `rcdmPractitioner validate succeeds`() {
        val roninPractitioner1 = rcdmPractitioner(TENANT_MNEMONIC) {}
        val validation = service.validate(roninPractitioner1, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
    }

    @Test
    fun `rcdmPractitioner with fhir id input`() {
        val roninPractitioner1 =
            rcdmPractitioner(TENANT_MNEMONIC) {
                id of Id("99")
            }
        val validation = service.validate(roninPractitioner1, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals(3, roninPractitioner1.identifier.size)
        val values = roninPractitioner1.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 3)
        assertTrue(values.contains("99".asFHIR()))
        assertTrue(values.contains(TENANT_MNEMONIC.asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        assertEquals("test-99", roninPractitioner1.id?.value)
    }

    @Test
    fun `rcdmPractitioner with an input NPI generated`() {
        val roninPractitioner1 =
            rcdmPractitioner(TENANT_MNEMONIC) {
                identifier of listOf(rcdmPractitionerNPI())
            }
        val validation = service.validate(roninPractitioner1, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        val npi = roninPractitioner1.identifier.find { it.system == CodeSystem.NPI.uri }
        assertTrue(npi?.value?.value?.isNotEmpty() == true)
    }

    @Test
    fun `rcdmPractitioner with an input NPI value supplied`() {
        val roninPractitioner1 =
            rcdmPractitioner(TENANT_MNEMONIC) {
                identifier of listOf(rcdmPractitionerNPI("An NPI"))
            }
        val validation = service.validate(roninPractitioner1, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        val npi = roninPractitioner1.identifier.find { it.system == CodeSystem.NPI.uri }
        assertEquals("An NPI".asFHIR(), npi!!.value)
    }

    @Test
    fun `rcdmPractitioner with an empty input NPI generated`() {
        val roninPractitioner1 =
            rcdmPractitioner(TENANT_MNEMONIC) {
                identifier of listOf(rcdmPractitionerNPI(""))
            }
        val validation = service.validate(roninPractitioner1, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        val npi = roninPractitioner1.identifier.find { it.system == CodeSystem.NPI.uri }
        assertTrue(npi?.value?.value?.isNotEmpty() == true)
    }

    @Test
    fun `rcdmPractitioner with valid telecom input`() {
        val contactPoint =
            ContactPoint(
                value = "123-456-7890".asFHIR(),
                system = Code(ContactPointSystem.PHONE.code),
                use = Code(ContactPointUse.HOME.code),
            )
        val roninPractitioner1 =
            rcdmPractitioner(TENANT_MNEMONIC) {
                telecom of listOf(contactPoint)
            }
        val validation = service.validate(roninPractitioner1, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals(1, roninPractitioner1.telecom.size)
        assertEquals("123-456-7890", roninPractitioner1.telecom[0].value?.value)
    }

    @Test
    fun `rcdmPractitioner with an invalid contact point drops only that contact point`() {
        val contactPoint =
            ContactPoint(
                value = "123-456-7890".asFHIR(),
            )
        val contactPoint2 =
            ContactPoint(
                value = "123-456-7890".asFHIR(),
                system = Code(ContactPointSystem.PHONE.code),
                use = Code(ContactPointUse.HOME.code),
            )
        val roninPractitioner1 =
            rcdmPractitioner(TENANT_MNEMONIC) {
                telecom of listOf(contactPoint, contactPoint2)
            }
        val validation = service.validate(roninPractitioner1, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals(1, roninPractitioner1.telecom.size)
        assertEquals("123-456-7890", roninPractitioner1.telecom[0].value?.value)
    }
}
