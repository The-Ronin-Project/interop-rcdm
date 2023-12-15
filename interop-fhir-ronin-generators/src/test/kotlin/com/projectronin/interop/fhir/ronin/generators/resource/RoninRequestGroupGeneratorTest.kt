package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.reference
import com.projectronin.interop.fhir.generators.primitives.dateTime
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.util.rcdmReference
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class RoninRequestGroupGeneratorTest : BaseGeneratorSpringTest() {
    @Test
    fun `example use for rcdmRequestGroup`() {
        // create request group resource with attributes you need, provide the tenant
        val rcdmRequestGroup =
            rcdmRequestGroup(TENANT_MNEMONIC) {
                // to test an attribute like status - provide the value
                status of Code("on-hold")
                authoredOn of
                    dateTime {
                        year of 1990
                        day of 8
                        month of 4
                    }
            }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val rcdmRequestGroupJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rcdmRequestGroup)

        // Uncomment to take a peek at the JSON
        // println(rcdmRequestGroupJSON)
        assertNotNull(rcdmRequestGroupJSON)
    }

    @Test
    fun `example use for rcdmPatient rcdmRequestGroup - missing required fields generated`() {
        // create patient and request group for tenant
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val rcdmRequestGroup = rcdmPatient.rcdmRequestGroup {}

        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val rcdmRequestGroupJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rcdmRequestGroup)

        // Uncomment to take a peek at the JSON
        // println(rcdmRequestGroupJSON)
        assertNotNull(rcdmRequestGroupJSON)
        assertNotNull(rcdmRequestGroup.meta)
        assertEquals(
            rcdmRequestGroup.meta!!.profile[0].value,
            RoninProfile.REQUEST_GROUP.value,
        )
        assertEquals(3, rcdmRequestGroup.identifier.size)
        assertNotNull(rcdmRequestGroup.status)
        assertNotNull(rcdmRequestGroup.intent)
        assertNotNull(rcdmRequestGroup.subject)
        assertNotNull(rcdmRequestGroup.id)
        val patientFHIRId =
            rcdmRequestGroup.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant =
            rcdmRequestGroup.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", rcdmRequestGroup.id?.value.toString())
        assertEquals(TENANT_MNEMONIC, tenant)
    }

    @Test
    fun `rcdmRequestGroup validates`() {
        val requestGroup = rcdmRequestGroup(TENANT_MNEMONIC) {}
        val validation = service.validate(requestGroup, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
    }

    @Test
    fun `rcdmRequestGroup validates with identifier added`() {
        val requestGroup =
            rcdmRequestGroup(TENANT_MNEMONIC) {
                identifier of listOf(Identifier(value = "identifier".asFHIR()))
            }
        val validation = service.validate(requestGroup, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals(4, requestGroup.identifier.size)
        val values = requestGroup.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 4)
        assertTrue(values.contains("identifier".asFHIR()))
        assertTrue(values.contains(TENANT_MNEMONIC.asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        // the fourth value is a generated identifier string
    }

    @Test
    fun `rcdmRequestGroup - valid subject input - validate succeeds`() {
        val requestGroup =
            rcdmRequestGroup(TENANT_MNEMONIC) {
                subject of rcdmReference("Patient", "456")
            }
        val validation = service.validate(requestGroup, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/456", requestGroup.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmRequestGroup validates`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val requestGroup = rcdmPatient.rcdmRequestGroup {}
        assertEquals("Patient/${rcdmPatient.id?.value}", requestGroup.subject?.reference?.value)
        val validation = service.validate(requestGroup, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
    }

    @Test
    fun `rcdmPatient rcdmRequestGroup - valid subject input overrides base patient - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val requestGroup =
            rcdmPatient.rcdmRequestGroup {
                subject of rcdmReference("Patient", "456")
            }
        val validation = service.validate(requestGroup, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/456", requestGroup.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmRequestGroup - base patient overrides invalid subject input - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val requestGroup =
            rcdmPatient.rcdmRequestGroup {
                subject of reference("Patient", "456")
            }
        val validation = service.validate(requestGroup, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/${rcdmPatient.id?.value}", requestGroup.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmRequestGroup - fhir id input for both - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) { id of "99" }
        val requestGroup =
            rcdmPatient.rcdmRequestGroup {
                id of "88"
            }
        val validation = service.validate(requestGroup, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals(3, requestGroup.identifier.size)
        val values = requestGroup.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 3)
        assertTrue(values.contains("88".asFHIR()))
        assertTrue(values.contains(TENANT_MNEMONIC.asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        assertEquals("test-88", requestGroup.id?.value)
        assertEquals("test-99", rcdmPatient.id?.value)
        assertEquals("Patient/test-99", requestGroup.subject?.reference?.value)
    }
}
