package com.projectronin.interop.rcdm.transform.map.resource

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.registry.model.ConceptMapCodeableConcept
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Suppress("ktlint:standard:max-line-length")
class MedicationMapperTest {
    private val registryClient = mockk<NormalizationRegistryClient>()
    private val mapper = MedicationMapper(registryClient)

    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "tenant"
        }

    @Test
    fun `supported resource is Medication`() {
        assertEquals(Medication::class, mapper.supportedResource)
    }

    @Test
    fun `null code on Medication`() {
        val medication = Medication(code = null)

        val (mappedResource, validation) = mapper.map(medication, tenant, null)
        assertEquals(medication, mappedResource)
        assertFalse(validation.hasIssues())
    }

    @Test
    fun `concept mapping finds no code`() {
        val code =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val medication = Medication(code = code)

        every {
            registryClient.getConceptMapping(
                "tenant",
                "Medication.code",
                code,
                medication,
                null,
            )
        } returns null

        val (mappedResource, validation) = mapper.map(medication, tenant, null)
        assertEquals(medication, mappedResource)

        val exception = assertThrows<IllegalArgumentException> { validation.alertIfErrors() }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value '12345' has no target defined in any Medication.code concept map for tenant 'tenant' @ Medication.code",
            exception.message,
        )
    }

    @Test
    fun `concept mapping finds code`() {
        val code =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val medication = Medication(code = code)

        val mappedCode =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("67890")),
                    ),
            )
        val mappedExtension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_MEDICATION_CODE.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, code),
            )
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Medication.code",
                code,
                medication,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedCode, mappedExtension, listOf())

        val (mappedResource, validation) = mapper.map(medication, tenant, null)
        mappedResource!!
        assertEquals(mappedCode, mappedResource.code)
        assertEquals(listOf(mappedExtension), mappedResource.extension)

        assertFalse(validation.hasIssues())
    }
}
