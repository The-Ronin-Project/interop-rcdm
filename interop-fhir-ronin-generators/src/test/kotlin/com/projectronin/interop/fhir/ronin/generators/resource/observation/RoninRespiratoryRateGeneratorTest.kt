package com.projectronin.interop.fhir.ronin.generators.resource.observation

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
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
class RoninRespiratoryRateGeneratorTest : BaseGeneratorSpringTest() {
    @BeforeEach
    fun normalizationClient() {
        every {
            normalizationRegistryClient.getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_RESPIRATORY_RATE.value)
        } returns possibleRespiratoryRateCodes
    }

    @Test
    fun `example use for roninObservationRespiratoryRate`() {
        // Create RespiratoryRate Obs with attributes you need, provide the tenant
        val roninObsRespiratoryRate =
            rcdmObservationRespiratoryRate(TENANT_MNEMONIC) {
                // if you want to test for a specific status
                status of Code("unknown")
                // test for a new or different code
                code of
                    codeableConcept {
                        coding of
                            listOf(
                                coding {
                                    system of "http://loinc.org"
                                    version of "0.01"
                                    code of Code("00000-2")
                                    display of "Respiratory rate"
                                },
                            )
                        text of "Respiratory rate" // text is kept if provided otherwise only a code.coding is generated
                    }
            }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsRespiratoryRateJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsRespiratoryRate)

        // Uncomment to take a peek at the JSON
        // println(roninObsRespiratoryRateJSON)
        assertNotNull(roninObsRespiratoryRateJSON)
    }

    @Test
    fun `example use for rcdmPatient rcdmObservationRespiratoryRate - missing required fields generated`() {
        // create patient and observation for tenant
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val roninObsRespiratoryRate = rcdmPatient.rcdmObservationRespiratoryRate {}
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsRespiratoryRateJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsRespiratoryRate)

        // Uncomment to take a peek at the JSON
        // println(roninObsRespiratoryRateJSON)
        assertNotNull(roninObsRespiratoryRateJSON)
        assertNotNull(roninObsRespiratoryRate.meta)
        assertEquals(
            roninObsRespiratoryRate.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_RESPIRATORY_RATE.value,
        )
        assertNotNull(roninObsRespiratoryRate.status)
        assertEquals(1, roninObsRespiratoryRate.category.size)
        assertNotNull(roninObsRespiratoryRate.code)
        assertNotNull(roninObsRespiratoryRate.subject)
        assertNotNull(roninObsRespiratoryRate.subject?.type?.extension)
        assertEquals("vital-signs", roninObsRespiratoryRate.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsRespiratoryRate.category[0].coding[0].system)
        assertNotNull(roninObsRespiratoryRate.status)
        assertNotNull(roninObsRespiratoryRate.code?.coding?.get(0)?.code?.value)
        assertNotNull(roninObsRespiratoryRate.id)
        val patientFHIRId =
            roninObsRespiratoryRate.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant =
            roninObsRespiratoryRate.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", roninObsRespiratoryRate.id?.value.toString())
    }

    @Test
    fun `generates valid roninObservationRespiratoryRate Observation`() {
        val roninObsRespRate = rcdmObservationRespiratoryRate(TENANT_MNEMONIC) {}
        assertNotNull(roninObsRespRate.id)
        assertNotNull(roninObsRespRate.meta)
        assertEquals(
            roninObsRespRate.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_RESPIRATORY_RATE.value,
        )
        assertNull(roninObsRespRate.implicitRules)
        assertNull(roninObsRespRate.language)
        assertNull(roninObsRespRate.text)
        assertEquals(0, roninObsRespRate.contained.size)
        assertEquals(1, roninObsRespRate.extension.size)
        assertEquals(0, roninObsRespRate.modifierExtension.size)
        assertTrue(roninObsRespRate.identifier.size >= 3)
        assertTrue(roninObsRespRate.identifier.any { it.value == "test".asFHIR() })
        assertTrue(roninObsRespRate.identifier.any { it.value == "EHR Data Authority".asFHIR() })
        assertTrue(roninObsRespRate.identifier.any { it.system == CodeSystem.RONIN_FHIR_ID.uri })
        assertNotNull(roninObsRespRate.status)
        assertEquals(1, roninObsRespRate.category.size)
        assertNotNull(roninObsRespRate.code)
        assertNotNull(roninObsRespRate.subject)
        assertNotNull(roninObsRespRate.subject?.type?.extension)
        assertEquals("vital-signs", roninObsRespRate.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsRespRate.category[0].coding[0].system)
        assertNotNull(roninObsRespRate.status)
        assertNotNull(roninObsRespRate.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `validates for roninObservationRespiratoryRate`() {
        val roninObsRespRate = rcdmObservationRespiratoryRate(TENANT_MNEMONIC) {}
        val validation = service.validate(roninObsRespRate, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
    }
}
