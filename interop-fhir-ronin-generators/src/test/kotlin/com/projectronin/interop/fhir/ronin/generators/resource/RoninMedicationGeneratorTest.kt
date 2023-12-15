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
class RoninMedicationGeneratorTest : BaseGeneratorSpringTest() {
    @Test
    fun `example use for roninMedication`() {
        // create appointment resource with attributes you need, provide the tenant
        val roninMedication =
            rcdmMedication(TENANT_MNEMONIC) {
                // to test an attribute like status - provide the value
                status of Code("testing-this-status")
            }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val roninMedicationJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninMedication)

        // Uncomment to take a peek at the JSON
        // println(roninMedicationJSON)
        assertNotNull(roninMedicationJSON)
    }

    @Test
    fun `validates rcdm medication`() {
        val medication = rcdmMedication(TENANT_MNEMONIC) {}
        val validation = service.validate(medication, TENANT_MNEMONIC).succeeded
        assertTrue(validation)
    }

    @Test
    fun `validates with identifier added`() {
        val medication =
            rcdmMedication(TENANT_MNEMONIC) {
                identifier of listOf(Identifier(id = "ID-Id".asFHIR()))
            }
        val validation = service.validate(medication, TENANT_MNEMONIC).succeeded
        assertTrue(validation)
        assertEquals(4, medication.identifier.size)
        val ids = medication.identifier.map { it.id }.toSet()
        assertTrue(ids.contains("ID-Id".asFHIR()))
    }
}
