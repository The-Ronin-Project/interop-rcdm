package com.projectronin.interop.fhir.ronin.generators.resource.observation

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.resource.BaseGeneratorSpringTest
import com.projectronin.interop.fhir.ronin.generators.resource.TENANT_MNEMONIC
import com.projectronin.interop.fhir.ronin.generators.resource.normalizationRegistryClient
import com.projectronin.interop.fhir.ronin.generators.resource.rcdmPatient
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import io.mockk.every
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class RoninBodyHeightGeneratorTest : BaseGeneratorSpringTest() {
    @BeforeEach
    fun normalizationClient() {
        every {
            normalizationRegistryClient.getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_BODY_HEIGHT.value)
        } returns possibleBodyHeightCodes
    }

    @Test
    fun `example use for roninObservationBodyHeight`() {
        // Create BodyHeight Obs with attributes you need, provide the tenant
        val roninObsBodyHeight =
            rcdmObservationBodyHeight(TENANT_MNEMONIC) {
                // if you want to test for a specific status
                status of Code("random-status")
                // test for a new or different code
                code of
                    codeableConcept {
                        coding of
                            listOf(
                                coding {
                                    system of "http://loinc.org"
                                    code of Code("8304-8")
                                    display of "Body height special circumstances"
                                },
                            )
                        text of "Body height special circumstances" // text is kept if provided otherwise only a code.coding is generated
                    }
            }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsBodyHeightJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsBodyHeight)

        // Uncomment to take a peek at the JSON
        // println(roninObsBodyHeightJSON)
        assertNotNull(roninObsBodyHeightJSON)
    }

    @Test
    fun `example use for rcdmPatient rcdmObservationBodyHeight - missing required fields generated`() {
        // create patient and observation for tenant
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val roninObsBodyHeight = rcdmPatient.rcdmObservationBodyHeight {}
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsBodyHeightJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsBodyHeight)

        // Uncomment to take a peek at the JSON
        // println(roninObsBodyHeightJSON)
        assertNotNull(roninObsBodyHeightJSON)
        assertNotNull(roninObsBodyHeight.meta)
        assertEquals(
            roninObsBodyHeight.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_BODY_HEIGHT.value,
        )
        assertNotNull(roninObsBodyHeight.status)
        assertEquals(1, roninObsBodyHeight.category.size)
        assertNotNull(roninObsBodyHeight.code)
        assertNotNull(roninObsBodyHeight.subject)
        assertNotNull(roninObsBodyHeight.subject?.type?.extension)
        assertEquals("vital-signs", roninObsBodyHeight.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsBodyHeight.category[0].coding[0].system)
        assertNotNull(roninObsBodyHeight.status)
        assertNotNull(roninObsBodyHeight.code?.coding?.get(0)?.code?.value)
        assertNotNull(roninObsBodyHeight.id)
        val patientFHIRId =
            roninObsBodyHeight.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant =
            roninObsBodyHeight.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", roninObsBodyHeight.id?.value.toString())
    }

    @Test
    fun `generates valid roninObservationBodyHeight Observation`() {
        val roninObsBodyHeight = rcdmObservationBodyHeight(TENANT_MNEMONIC) {}
        assertNotNull(roninObsBodyHeight.id)
        assertNotNull(roninObsBodyHeight.meta)
        assertEquals(
            roninObsBodyHeight.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_BODY_HEIGHT.value,
        )
        assertNull(roninObsBodyHeight.implicitRules)
        assertNull(roninObsBodyHeight.language)
        assertNull(roninObsBodyHeight.text)
        assertEquals(0, roninObsBodyHeight.contained.size)
        assertEquals(1, roninObsBodyHeight.extension.size)
        assertEquals(0, roninObsBodyHeight.modifierExtension.size)
        assertTrue(roninObsBodyHeight.identifier.size >= 3)
        assertTrue(roninObsBodyHeight.identifier.any { it.value == "test".asFHIR() })
        assertTrue(roninObsBodyHeight.identifier.any { it.value == "EHR Data Authority".asFHIR() })
        assertTrue(roninObsBodyHeight.identifier.any { it.system == CodeSystem.RONIN_FHIR_ID.uri })
        assertNotNull(roninObsBodyHeight.status)
        assertEquals(1, roninObsBodyHeight.category.size)
        assertNotNull(roninObsBodyHeight.code)
        assertNotNull(roninObsBodyHeight.subject)
        assertNotNull(roninObsBodyHeight.subject?.type?.extension)
        assertEquals("vital-signs", roninObsBodyHeight.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsBodyHeight.category[0].coding[0].system)
        assertNotNull(roninObsBodyHeight.status)
        assertNotNull(roninObsBodyHeight.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `validates roninObservationBodyHeight generator`() {
        val roninObsBodyHeight = rcdmObservationBodyHeight(TENANT_MNEMONIC) {}
        val validation = service.validate(roninObsBodyHeight, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
    }

    @Test
    fun `validation for rcdmObservationBodyHeight with provided identifier and code`() {
        val roninObsBodyHeight =
            rcdmObservationBodyHeight(TENANT_MNEMONIC) {
                identifier of
                    listOf(
                        Identifier(
                            system = Uri("testsystem"),
                            value = "tomato".asFHIR(),
                        ),
                    )
                code of
                    codeableConcept {
                        coding of
                            listOf(
                                coding {
                                    system of "http://loinc.org"
                                    version of "2.74"
                                    code of Code("8302-2")
                                    display of "Body height"
                                },
                            )
                    }
            }
        val validation = service.validate(roninObsBodyHeight, TENANT_MNEMONIC).succeeded
        assertTrue(validation)
        assertNotNull(roninObsBodyHeight.meta)
        assertNotNull(roninObsBodyHeight.identifier)
        assertEquals(4, roninObsBodyHeight.identifier.size)
        assertNotNull(roninObsBodyHeight.status)
        assertNotNull(roninObsBodyHeight.code)
        assertNotNull(roninObsBodyHeight.subject)
    }
}
