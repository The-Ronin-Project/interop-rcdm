package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class RoninMedicationAdministrationGeneratorTest : BaseGeneratorSpringTest() {
    @Test
    fun `example use for roninMedicationAdministration`() {
        // create medicationAdministration resource with attributes you need, provide the tenant
        val roninMedicationAdministration =
            rcdmMedicationAdministration(TENANT_MNEMONIC) {
                // to test an attribute like status - provide the value
                status of Code("testing-this-status")
            }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val roninMedicationAdministrationJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(roninMedicationAdministration)

        // Uncomment to take a peek at the JSON
        // println(roninMedicationAdministrationJSON)
        assertNotNull(roninMedicationAdministrationJSON)
    }

    @Test
    fun `validates rcdm medication administration`() {
        val medication = rcdmMedicationAdministration(TENANT_MNEMONIC) {}
        val validation = service.validate(medication, TENANT_MNEMONIC).succeeded
        assertTrue(validation)
    }

    @Test
    fun `validates with identifier added`() {
        val medicationAdministration =
            rcdmMedicationAdministration(TENANT_MNEMONIC) {
                identifier of listOf(Identifier(id = "ID-Id".asFHIR()))
            }
        val validation = service.validate(medicationAdministration, TENANT_MNEMONIC).succeeded
        assertTrue(validation)
        assertEquals(4, medicationAdministration.identifier.size)
        val ids = medicationAdministration.identifier.map { it.id }.toSet()
        assertTrue(ids.contains("ID-Id".asFHIR()))
    }

    @Test
    fun `rcdmPatient rcdmMedicationAdministration validates`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val medicationAdministration = rcdmPatient.rcdmMedicationAdministration {}
        assertEquals("Patient/${rcdmPatient.id?.value}", medicationAdministration.subject?.reference?.value)
        val validation = service.validate(medicationAdministration, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
    }
}
