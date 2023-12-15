package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.datatypes.reference
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
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
class RoninDocumentReferenceGeneratorTest : BaseGeneratorSpringTest() {
    @Test
    fun `example use for rcdmDocumentReference`() {
        // create document reference resource with attributes you need, provide the tenant
        val rcdmDocumentReference =
            rcdmDocumentReference(TENANT_MNEMONIC, "binaryId") {
                // to test an attribute like status - provide the value
                status of Code("on-hold")
            }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val rcdmDocumentReferenceJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rcdmDocumentReference)

        // Uncomment to take a peek at the JSON
        // println(rcdmDocumentReferenceJSON)
        assertNotNull(rcdmDocumentReferenceJSON)
    }

    @Test
    fun `example use for rcdmPatient rcdmDocumentReference - missing required fields generated`() {
        // create patient and document reference for tenant
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val rcdmDocumentReference = rcdmPatient.rcdmDocumentReference("binaryId") {}

        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val rcdmDocumentReferenceJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rcdmDocumentReference)

        // Uncomment to take a peek at the JSON
        // println(rcdmDocumentReferenceJSON)
        assertNotNull(rcdmDocumentReferenceJSON)
        assertNotNull(rcdmPatient)
        assertTrue(rcdmPatient.id?.value?.startsWith("test-") == true)
        assertNotNull(rcdmDocumentReference.meta)
        assertEquals(
            RoninProfile.DOCUMENT_REFERENCE.value,
            rcdmDocumentReference.meta!!.profile[0].value,
        )
        assertEquals(3, rcdmDocumentReference.identifier.size)
        assertNotNull(rcdmDocumentReference.status)
        assertTrue(rcdmDocumentReference.status in possibleDocumentReferenceStatusCodes)
        assertNotNull(rcdmDocumentReference.type)
        assertTrue(rcdmDocumentReference.type?.coding?.first() in possibleDocumentReferenceTypeCodes)
        assertEquals(1, rcdmDocumentReference.category.size)
        assertTrue(rcdmDocumentReference.category.first().coding.first() in possibleDocumentReferenceCategoryCodes)
        assertNotNull(rcdmDocumentReference.subject)
        assertTrue(rcdmDocumentReference.status in possibleDocumentReferenceStatusCodes)
        assertNotNull(rcdmDocumentReference.id)
        val patientFHIRId =
            rcdmDocumentReference.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant =
            rcdmDocumentReference.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", rcdmDocumentReference.id?.value.toString())
        assertEquals(TENANT_MNEMONIC, tenant)
    }

    @Test
    fun `rcdmDocumentReference validates`() {
        val documentReference = rcdmDocumentReference(TENANT_MNEMONIC, "binaryId") {}
        val validation = service.validate(documentReference, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
    }

    @Test
    fun `rcdmDocumentReference validates with identifier added`() {
        val documentReference =
            rcdmDocumentReference(TENANT_MNEMONIC, "binaryId") {
                identifier of listOf(Identifier(value = "identifier".asFHIR()))
            }
        val validation = service.validate(documentReference, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals(4, documentReference.identifier.size)
        val values = documentReference.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 4)
        assertTrue(values.contains("identifier".asFHIR()))
        assertTrue(values.contains(TENANT_MNEMONIC.asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        // the fourth value is a generated identifier string
    }

    @Test
    fun `rcdmDocumentReference - valid subject input - validate succeeds`() {
        val documentReference =
            rcdmDocumentReference(TENANT_MNEMONIC, "binaryId") {
                subject of rcdmReference("Patient", "456")
            }
        val validation = service.validate(documentReference, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/456", documentReference.subject?.reference?.value)
    }

    @Test
    fun `rcdmDocumentReference - valid type input - validate succeeds`() {
        val documentReference =
            rcdmDocumentReference(TENANT_MNEMONIC, "binaryId") {
                type of
                    codeableConcept {
                        coding of
                            listOf(
                                coding {
                                    system of CodeSystem.LOINC.uri
                                    code of Code("100")
                                    display of "Fake"
                                },
                            )
                    }
            }
        val validation = service.validate(documentReference, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals(Code("100"), documentReference.type?.coding?.first()?.code)
    }

    @Test
    fun `rcdmDocumentReference - valid category input - validate succeeds`() {
        val documentReference =
            rcdmDocumentReference(TENANT_MNEMONIC, "binaryId") {
                category of
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.DOCUMENT_REFERENCE_CATEGORY.uri,
                                        code = Code("clinical-note"),
                                        display = "Clinical Note".asFHIR(),
                                    ),
                                ),
                        ),
                    )
            }
        val validation = service.validate(documentReference, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Clinical Note", documentReference.category.first().coding.first().display?.value)
    }

    @Test
    fun `rcdmPatient rcdmDocumentReference validates`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val documentReference = rcdmPatient.rcdmDocumentReference("binaryId") {}
        assertEquals("Patient/${rcdmPatient.id?.value}", documentReference.subject?.reference?.value)
        val validation = service.validate(documentReference, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
    }

    @Test
    fun `rcdmPatient rcdmDocumentReference - valid subject input overrides base patient - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val documentReference =
            rcdmPatient.rcdmDocumentReference("binaryId") {
                subject of rcdmReference("Patient", "456")
            }
        val validation = service.validate(documentReference, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/456", documentReference.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmDocumentReference - base patient overrides invalid subject input - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val documentReference =
            rcdmPatient.rcdmDocumentReference("binaryId") {
                subject of reference("Patient", "456")
            }
        val validation = service.validate(documentReference, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/${rcdmPatient.id?.value}", documentReference.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmDocumentReference - fhir id input for both - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) { id of "99" }
        val documentReference =
            rcdmPatient.rcdmDocumentReference("binaryId") {
                id of "88"
            }
        val validation = service.validate(documentReference, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals(3, documentReference.identifier.size)
        val values = documentReference.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 3)
        assertTrue(values.contains("88".asFHIR()))
        assertTrue(values.contains(TENANT_MNEMONIC.asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        assertEquals("test-88", documentReference.id?.value)
        assertEquals("test-99", rcdmPatient.id?.value)
        assertEquals("Patient/test-99", documentReference.subject?.reference?.value)
    }
}
