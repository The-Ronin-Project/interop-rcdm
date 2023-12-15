package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.reference
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Dosage
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.util.rcdmReference
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class RoninMedicationStatementGeneratorTest : BaseGeneratorSpringTest() {
    @Test
    fun `example use for rcdmMedicationStatement`() {
        // create medication statement resource with attributes you need, provide the tenant
        val rcdmMedicationStatement =
            rcdmMedicationStatement(TENANT_MNEMONIC) {
                // to test an attribute like status - provide the value
                status of Code("on-hold")
            }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val rcdmMedicationStatementJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rcdmMedicationStatement)

        // Uncomment to take a peek at the JSON
        // println(rcdmMedicationStatementJSON)
        assertNotNull(rcdmMedicationStatementJSON)
    }

    @Test
    fun `example use for rcdmPatient rcdmMedicationStatement - missing required fields generated`() {
        // create patient and medication statement for tenant
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val rcdmMedicationStatement = rcdmPatient.rcdmMedicationStatement {}

        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val rcdmMedicationStatementJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rcdmMedicationStatement)

        // Uncomment to take a peek at the JSON
        // println(rcdmMedicationStatementJSON)
        assertNotNull(rcdmMedicationStatementJSON)
        assertNotNull(rcdmPatient)
        assertNotNull(rcdmMedicationStatement.id)
        assertNotNull(rcdmMedicationStatement.meta)
        assertEquals(
            RoninProfile.MEDICATION_STATEMENT.value,
            rcdmMedicationStatement.meta!!.profile[0].value,
        )
        assertEquals(3, rcdmMedicationStatement.identifier.size)
        val patientFHIRId =
            rcdmMedicationStatement.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenantId =
            rcdmMedicationStatement.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenantId-$patientFHIRId", rcdmMedicationStatement.id?.value.toString())
        assertEquals(emptyList<Reference>(), rcdmMedicationStatement.basedOn)
        assertEquals(emptyList<Reference>(), rcdmMedicationStatement.partOf)
        assertNotNull(rcdmMedicationStatement.status)
        assertTrue(rcdmMedicationStatement.status in possibleMedicationStatementStatusCodes)
        assertEquals(emptyList<CodeableConcept>(), rcdmMedicationStatement.statusReason)
        assertNull(rcdmMedicationStatement.category)
        assertNotNull(rcdmMedicationStatement.medication)
        assertEquals(DynamicValueType.REFERENCE, rcdmMedicationStatement.medication?.type)
        assertEquals("Medication", (rcdmMedicationStatement.medication?.value as Reference).decomposedType())
        assertNotNull(rcdmMedicationStatement.subject)
        assertEquals("Patient", rcdmMedicationStatement.subject?.decomposedType())
        assertNull(rcdmMedicationStatement.context)
        assertNull(rcdmMedicationStatement.effective)
        assertNull(rcdmMedicationStatement.dateAsserted)
        assertNull(rcdmMedicationStatement.informationSource)
        assertEquals(emptyList<Reference>(), rcdmMedicationStatement.derivedFrom)
        assertEquals(emptyList<CodeableConcept>(), rcdmMedicationStatement.reasonCode)
        assertEquals(emptyList<Reference>(), rcdmMedicationStatement.reasonReference)
        assertEquals(emptyList<Annotation>(), rcdmMedicationStatement.reasonReference)
        assertEquals(emptyList<Dosage>(), rcdmMedicationStatement.dosage)
    }

    @Test
    fun `rcdmMedicationStatement validates`() {
        val medicationStatement = rcdmMedicationStatement(TENANT_MNEMONIC) {}
        val validation = service.validate(medicationStatement, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
    }

    @Test
    fun `rcdmMedicationStatement validates with identifier added`() {
        val medicationStatement =
            rcdmMedicationStatement(TENANT_MNEMONIC) {
                identifier of listOf(Identifier(value = "identifier".asFHIR()))
            }
        val validation = service.validate(medicationStatement, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals(4, medicationStatement.identifier.size)
        val values = medicationStatement.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 4)
        assertTrue(values.contains("identifier".asFHIR()))
        assertTrue(values.contains(TENANT_MNEMONIC.asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        // the fourth value is a generated identifier string
    }

    @Test
    fun `rcdmMedicationStatement - valid subject input - validate succeeds`() {
        val medicationStatement =
            rcdmMedicationStatement(TENANT_MNEMONIC) {
                subject of rcdmReference("Patient", "456")
            }
        val validation = service.validate(medicationStatement, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/456", medicationStatement.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmMedicationStatement validates`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val medicationStatement = rcdmPatient.rcdmMedicationStatement {}
        assertEquals("Patient/${rcdmPatient.id?.value}", medicationStatement.subject?.reference?.value)
        val validation = service.validate(medicationStatement, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
    }

    @Test
    fun `rcdmPatient rcdmMedicationStatement - base patient overrides invalid subject input - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val medicationStatement =
            rcdmPatient.rcdmMedicationStatement {
                subject of reference("Patient", "456")
            }
        val validation = service.validate(medicationStatement, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/${rcdmPatient.id?.value}", medicationStatement.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmMedicationStatement - valid subject input overrides base patient - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val medicationStatement =
            rcdmPatient.rcdmMedicationStatement {
                subject of rcdmReference("Patient", "456")
            }
        val validation = service.validate(medicationStatement, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/456", medicationStatement.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmMedicationStatement - fhir id input for both - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) { id of "99" }
        val medicationStatement =
            rcdmPatient.rcdmMedicationStatement {
                id of "88"
            }
        val validation = service.validate(medicationStatement, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals(3, medicationStatement.identifier.size)
        val values = medicationStatement.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 3)
        assertTrue(values.contains("88".asFHIR()))
        assertTrue(values.contains(TENANT_MNEMONIC.asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        assertEquals("test-88", medicationStatement.id?.value)
        assertEquals("test-99", rcdmPatient.id?.value)
        assertEquals("Patient/test-99", medicationStatement.subject?.reference?.value)
    }
}
