package com.projectronin.interop.fhir.ronin.generators.resource.condition

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.datatypes.reference
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.resource.BaseGeneratorSpringTest
import com.projectronin.interop.fhir.ronin.generators.resource.TENANT_MNEMONIC
import com.projectronin.interop.fhir.ronin.generators.resource.rcdmPatient
import com.projectronin.interop.fhir.ronin.generators.util.conditionCodeExtension
import com.projectronin.interop.fhir.ronin.generators.util.rcdmReference
import com.projectronin.interop.rcdm.common.util.dataAuthorityExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class RoninConditionProblemsAndHealthConcernsGeneratorTest : BaseGeneratorSpringTest() {
    @Test
    fun `generates basic ronin condition problems and health concerns`() {
        // create condition problems and health concerns resource with attributes you need, provide the tenant
        val roninCondition = rcdmConditionProblemsAndHealthConcerns(TENANT_MNEMONIC) { }
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
    fun `generates ronin condition problems and health concerns with input parameters`() {
        // create patient and condition problems and health concerns for tenant
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val roninCondition =
            rcdmPatient.rcdmConditionProblemsAndHealthConcerns {
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
                subject of
                    Reference(
                        reference = "Patient/123".asFHIR(),
                        type = Uri("Condition", extension = dataAuthorityExtension),
                    )
            }
        val validate = service.validate(roninCondition, TENANT_MNEMONIC).succeeded
        assertTrue(validate)
    }

    @Test
    fun `rcdmConditionProblemsAndHealthConcerns - valid subject input - validate succeeds`() {
        val roninCondition =
            rcdmConditionProblemsAndHealthConcerns(TENANT_MNEMONIC) {
                subject of rcdmReference("Patient", "456")
            }
        val validation = service.validate(roninCondition, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/456", roninCondition.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmConditionProblemsAndHealthConcerns validates`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val roninCondition = rcdmPatient.rcdmConditionProblemsAndHealthConcerns {}
        assertEquals("Patient/${rcdmPatient.id?.value}", roninCondition.subject?.reference?.value)
        val validation = service.validate(roninCondition, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
    }

    @Test
    fun `rcdmPatient rcdmConditionProblemsAndHealthConcerns - valid subject input overrides base patient - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val roninCondition =
            rcdmPatient.rcdmConditionProblemsAndHealthConcerns {
                subject of rcdmReference("Patient", "456")
            }
        val validation = service.validate(roninCondition, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/456", roninCondition.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmConditionProblemsAndHealthConcerns - base patient overrides invalid subject input - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val roninCondition =
            rcdmPatient.rcdmConditionProblemsAndHealthConcerns {
                subject of reference("Patient", "456")
            }
        val validation = service.validate(roninCondition, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/${rcdmPatient.id?.value}", roninCondition.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmConditionProblemsAndHealthConcerns - fhir id input for both - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) { id of "99" }
        val roninCondition =
            rcdmPatient.rcdmConditionProblemsAndHealthConcerns {
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
