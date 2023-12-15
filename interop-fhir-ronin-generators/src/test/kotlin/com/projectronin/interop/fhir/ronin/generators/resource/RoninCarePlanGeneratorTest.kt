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
class RoninCarePlanGeneratorTest : BaseGeneratorSpringTest() {
    @Test
    fun `example use for rcdmCarePlan`() {
        // create care plan resource with attributes you need, provide the tenant
        val rcdmCarePlan =
            rcdmCarePlan(TENANT_MNEMONIC) {
                // to test an attribute like status - provide the value
                status of Code("on-hold")
                created of
                    dateTime {
                        year of 1990
                        day of 8
                        month of 4
                    }
            }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val rcdmCarePlanJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rcdmCarePlan)

        // Uncomment to take a peek at the JSON
        // println(rcdmCarePlanJSON)
        assertNotNull(rcdmCarePlanJSON)
    }

    @Test
    fun `example use for rcdmPatient rcdmCarePlan - missing required fields generated`() {
        // create patient and care plan for tenant
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val rcdmCarePlan = rcdmPatient.rcdmCarePlan {}

        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val rcdmCarePlanJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rcdmCarePlan)

        // Uncomment to take a peek at the JSON
        // println(rcdmCarePlanJSON)
        assertNotNull(rcdmCarePlanJSON)
        assertNotNull(rcdmPatient)
        assertTrue(rcdmPatient.id?.value?.startsWith("test-") == true)
        assertNotNull(rcdmCarePlan.meta)
        assertEquals(
            rcdmCarePlan.meta!!.profile[0].value,
            RoninProfile.CARE_PLAN.value,
        )
        assertEquals(3, rcdmCarePlan.identifier.size)
        assertNotNull(rcdmCarePlan.status)
        assertNotNull(rcdmCarePlan.intent)
        assertNotNull(rcdmCarePlan.subject)
        assertNotNull(rcdmCarePlan.id)
        val patientFHIRId =
            rcdmCarePlan.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant =
            rcdmCarePlan.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", rcdmCarePlan.id?.value.toString())
        assertEquals(TENANT_MNEMONIC, tenant)
    }

    @Test
    fun `rcdmCarePlan validates`() {
        val carePlan = rcdmCarePlan(TENANT_MNEMONIC) {}
        val validation = service.validate(carePlan, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
    }

    @Test
    fun `rcdmCarePlan validates with identifier added`() {
        val carePlan =
            rcdmCarePlan(TENANT_MNEMONIC) {
                identifier of listOf(Identifier(value = "identifier".asFHIR()))
            }
        val validation = service.validate(carePlan, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals(4, carePlan.identifier.size)
        val values = carePlan.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 4)
        assertTrue(values.contains("identifier".asFHIR()))
        assertTrue(values.contains(TENANT_MNEMONIC.asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        // the fourth value is a generated identifier string
    }

    @Test
    fun `rcdmCarePlan - valid subject input - validate succeeds`() {
        val carePlan =
            rcdmCarePlan(TENANT_MNEMONIC) {
                subject of rcdmReference("Patient", "456")
            }
        val validation = service.validate(carePlan, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/456", carePlan.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmCarePlan validates`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val carePlan = rcdmPatient.rcdmCarePlan {}
        assertEquals("Patient/${rcdmPatient.id?.value}", carePlan.subject?.reference?.value)
        val validation = service.validate(carePlan, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
    }

    @Test
    fun `rcdmPatient rcdmCarePlan - valid subject input overrides base patient - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val carePlan =
            rcdmPatient.rcdmCarePlan {
                subject of rcdmReference("Patient", "456")
            }
        val validation = service.validate(carePlan, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/456", carePlan.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmCarePlan - base patient overrides invalid subject input - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val carePlan =
            rcdmPatient.rcdmCarePlan {
                subject of reference("Patient", "456")
            }
        val validation = service.validate(carePlan, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/${rcdmPatient.id?.value}", carePlan.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmCarePlan - fhir id input for both - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) { id of "99" }
        val carePlan =
            rcdmPatient.rcdmCarePlan {
                id of "88"
            }
        val validation = service.validate(carePlan, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals(3, carePlan.identifier.size)
        val values = carePlan.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 3)
        assertTrue(values.contains("88".asFHIR()))
        assertTrue(values.contains(TENANT_MNEMONIC.asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        assertEquals("test-88", carePlan.id?.value)
        assertEquals("test-99", rcdmPatient.id?.value)
        assertEquals("Patient/test-99", carePlan.subject?.reference?.value)
    }
}
