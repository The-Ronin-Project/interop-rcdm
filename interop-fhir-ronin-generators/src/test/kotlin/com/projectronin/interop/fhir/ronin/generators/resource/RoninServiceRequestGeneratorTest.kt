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
class RoninServiceRequestGeneratorTest : BaseGeneratorSpringTest() {
    @Test
    fun `example use for roninServiceRequest`() {
        // create serviceRequest resource with attributes you need, provide the tenant
        val roninServiceRequest =
            rcdmServiceRequest(TENANT_MNEMONIC) {
                // to test an attribute like status - provide the value
                status of Code("testing-this-status")
            }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val roninServiceRequestJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(roninServiceRequest)

        // Uncomment to take a peek at the JSON
        // println(roninServiceRequestJSON)
        assertNotNull(roninServiceRequestJSON)
    }

    @Test
    fun `validates with identifier added`() {
        val serviceRequest =
            rcdmServiceRequest(TENANT_MNEMONIC) {
                identifier of listOf(Identifier(id = "ID-Id".asFHIR()))
            }
        val validation = service.validate(serviceRequest, TENANT_MNEMONIC).succeeded
        assertTrue(validation)
        assertEquals(4, serviceRequest.identifier.size)
        val ids = serviceRequest.identifier.map { it.id }.toSet()
        assertTrue(ids.contains("ID-Id".asFHIR()))
    }

    @Test
    fun `rcdmPatient rcdmServiceRequest validates`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val serviceRequest = rcdmPatient.rcdmServiceRequest {}
        assertEquals("Patient/${rcdmPatient.id?.value}", serviceRequest.subject?.reference?.value)
        val validation = service.validate(serviceRequest, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
    }
}
