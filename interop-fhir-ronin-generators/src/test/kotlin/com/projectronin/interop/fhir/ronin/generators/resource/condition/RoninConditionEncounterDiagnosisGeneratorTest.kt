package com.projectronin.interop.fhir.ronin.generators.resource.condition

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.datatypes.reference
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.resource.BaseGeneratorSpringTest
import com.projectronin.interop.fhir.ronin.generators.resource.TENANT_MNEMONIC
import com.projectronin.interop.fhir.ronin.generators.resource.rcdmPatient
import com.projectronin.interop.fhir.ronin.generators.util.conditionCodeExtension
import com.projectronin.interop.fhir.ronin.generators.util.rcdmReference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class RoninConditionEncounterDiagnosisGeneratorTest : BaseGeneratorSpringTest() {
    @Test
    fun `generates basic ronin condition encounter diagnosis`() {
        // create condition encounter diagnosis resource with attributes you need, provide the tenant
        val roninCondition = rcdmConditionEncounterDiagnosis(TENANT_MNEMONIC) { }
        val validate = service.validate(roninCondition, TENANT_MNEMONIC).succeeded
        assertEquals(roninCondition.code?.coding?.size, 1)
        assertNotNull(roninCondition.subject)
        assertNotNull(roninCondition.id)
        val patientFHIRId =
            roninCondition.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant =
            roninCondition.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", roninCondition.id?.value.toString())
        assertEquals(TENANT_MNEMONIC, tenant)
        assertTrue(validate)

        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninConditionJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninCondition)
        // Uncomment to take a peek at the JSON
        // println(roninConditionJSON)
        assertNotNull(roninConditionJSON)
    }

    @Test
    fun `example use for patient and condition encounter diagnosis with input parameters - missing required fields generated`() {
        // create patient and condition encounter diagnosis for tenant
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val roninCondition =
            rcdmPatient.rcdmConditionEncounterDiagnosis {
                // add any attributes you need
                id of Id("12345")
                extension of listOf(conditionCodeExtension)
                identifier of
                    listOf(
                        Identifier(
                            system = Uri("testsystem"),
                            value = "tomato".asFHIR(),
                        ),
                    )
                category of
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.CONDITION_CATEGORY.uri,
                                        code = Code("potatos"),
                                    ),
                                ),
                        ),
                    )
            }
        val validate = service.validate(roninCondition, TENANT_MNEMONIC).succeeded
        assertEquals(roninCondition.code?.coding?.size, 1)
        assertNotNull(roninCondition.subject)
        assertTrue(validate)

        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninCondition)
        // Uncomment to take a peek at the JSON
        // println(json)
        assertNotNull(json)
    }

    @Test
    fun `rcdmConditionEncounterDiagnosis - valid subject input - validate succeeds`() {
        val roninCondition =
            rcdmConditionEncounterDiagnosis(TENANT_MNEMONIC) {
                subject of rcdmReference("Patient", "456")
            }
        val validation = service.validate(roninCondition, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/456", roninCondition.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmConditionEncounterDiagnosis validates`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val roninCondition = rcdmPatient.rcdmConditionEncounterDiagnosis {}
        assertEquals("Patient/${rcdmPatient.id?.value}", roninCondition.subject?.reference?.value)
        val validation = service.validate(roninCondition, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
    }

    @Test
    fun `rcdmPatient rcdmConditionEncounterDiagnosis - valid subject input overrides base patient - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val roninCondition =
            rcdmPatient.rcdmConditionEncounterDiagnosis {
                subject of rcdmReference("Patient", "456")
            }
        val validation = service.validate(roninCondition, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/456", roninCondition.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmConditionEncounterDiagnosis - base patient overrides invalid subject input - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val roninCondition =
            rcdmPatient.rcdmConditionEncounterDiagnosis {
                subject of reference("Patient", "456")
            }
        val validation = service.validate(roninCondition, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/${rcdmPatient.id?.value}", roninCondition.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmConditionEncounterDiagnosis - fhir id input for both - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) { id of "99" }
        val roninCondition =
            rcdmPatient.rcdmConditionEncounterDiagnosis {
                id of "88"
            }
        val validation = service.validate(roninCondition, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals(3, roninCondition.identifier.size)
        val values = roninCondition.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 3)
        assertTrue(values.contains("88".asFHIR()))
        assertTrue(values.contains(TENANT_MNEMONIC.asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        assertEquals("test-88", roninCondition.id?.value)
        assertEquals("test-99", rcdmPatient.id?.value)
        assertEquals("Patient/test-99", roninCondition.subject?.reference?.value)
    }
}
