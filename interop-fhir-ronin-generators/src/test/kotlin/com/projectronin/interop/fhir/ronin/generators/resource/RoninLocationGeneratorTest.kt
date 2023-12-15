package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class RoninLocationGeneratorTest : BaseGeneratorSpringTest() {
    @Test
    fun `example use for roninLocation`() {
        // create location for tenant "test"
        val roninLocation =
            rcdmLocation(TENANT_MNEMONIC) {
                // to test an attribute like status - provide the value
                status of Code("testing-this-status")
                telecom of
                    listOf(
                        ContactPoint(
                            value = "123-456-7890".asFHIR(),
                            system = Code(ContactPointSystem.PHONE.code),
                            use = Code(ContactPointUse.WORK.code),
                        ),
                    )
            }

        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninLocationJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninLocation)

        // Uncomment to take a peek at the JSON
        // println(roninLocationJSON)
        assertNotNull(roninLocationJSON)
    }

    @Test
    fun `example use for roninLocation - missing required fields generated`() {
        val roninLocation = rcdmLocation(TENANT_MNEMONIC) {}

        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninLocationJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninLocation)

        // Uncomment to take a peek at the JSON
        // println(roninLocationJSON)
        assertNotNull(roninLocationJSON)
        assertNotNull(roninLocation.meta)
        assertEquals(
            roninLocation.meta!!.profile[0].value,
            RoninProfile.LOCATION.value,
        )
        assertEquals(3, roninLocation.identifier.size)
        assertNotNull(roninLocation.status)
        assertNotNull(roninLocation.name)
        assertNotNull(roninLocation.telecom)
        assertNotNull(roninLocation.id)
        val patientFHIRId = roninLocation.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant = roninLocation.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", roninLocation.id?.value.toString())
        assertEquals(TENANT_MNEMONIC, tenant)
    }

    @Test
    fun `validates with identifier added`() {
        val location =
            rcdmLocation(TENANT_MNEMONIC) {
                identifier of listOf(Identifier(id = "ID-Id".asFHIR()))
                status of Code("active")
                name of "this is a name"
            }
        val validation = service.validate(location, TENANT_MNEMONIC).succeeded
        assertTrue(validation)
        assertEquals(4, location.identifier.size)
        val ids = location.identifier.map { it.id }.toSet()
        assertTrue(ids.contains("ID-Id".asFHIR()))
    }

    @Test
    fun `validates with fhir id input`() {
        val location =
            rcdmLocation(TENANT_MNEMONIC) {
                id of "88"
            }
        val validation = service.validate(location, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals(3, location.identifier.size)
        val values = location.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 3)
        assertTrue(values.contains("88".asFHIR()))
        assertTrue(values.contains(TENANT_MNEMONIC.asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        assertEquals("test-88", location.id?.value)
    }

    @Test
    fun `generates valid rcdm location`() {
        val location = rcdmLocation(TENANT_MNEMONIC) {}
        val validation = service.validate(location, TENANT_MNEMONIC).succeeded
        assertTrue(validation)
        assertNotNull(location.meta)
        assertNotNull(location.identifier)
        assertEquals(3, location.identifier.size)
        assertNotNull(location.name)
        assertNotNull(location.telecom)
    }

    @Test
    fun `generates rcdm location and validates with contact-point and status`() {
        val contactPoint =
            ContactPoint(
                value = "123-456-7890".asFHIR(),
                system = Code("something"),
            )
        val location =
            rcdmLocation(TENANT_MNEMONIC) {
                status of Code("active")
                telecom of listOf(contactPoint)
                name of "this is a name"
            }
        val validation = service.validate(location, TENANT_MNEMONIC).succeeded
        assertTrue(validation)
        assertNotNull(location.meta)
        assertNotNull(location.identifier)
        assertEquals(3, location.identifier.size)
        assertNotNull(location.telecom)
    }

    @Test
    fun `validates if empty name provided, data-absent-reason is returned`() {
        val location =
            rcdmLocation(TENANT_MNEMONIC) {
                name of ""
            }
        val validation = service.validate(location, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertNotNull(location.name)
        assertTrue(location.name?.value!!.contains("http://hl7.org/fhir/StructureDefinition/data-absent-reason"))
    }
}
