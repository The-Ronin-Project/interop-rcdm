package com.projectronin.interop.rcdm.transform.map.resource

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.util.dataAuthorityIdentifier
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.registry.model.ConceptMapCodeableConcept
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class ConditionMapperTest {
    private val registryClient = mockk<NormalizationRegistryClient>()
    private val mapper = ConditionMapper(registryClient, "unmapped1,unmapped2")

    private val mappedTenant = mockk<Tenant> {
        every { mnemonic } returns "tenant"
    }

    private val unmappedTenant = mockk<Tenant> {
        every { mnemonic } returns "unmapped2"
    }

    @Test
    fun `supported resource is Condition`() {
        assertEquals(Condition::class, mapper.supportedResource)
    }

    @Test
    fun `tenant in not mapped list with null code`() {
        val condition = Condition(
            subject = Reference(display = "Subject".asFHIR())
        )

        val (mappedResource, validation) = mapper.map(condition, unmappedTenant, null)
        mappedResource!!
        assertEquals(listOf<Extension>(), mappedResource.extension)

        assertFalse(validation.hasIssues())
    }

    @Test
    fun `tenant in not mapped list with code`() {
        val code = CodeableConcept(
            coding = listOf(
                Coding(system = Uri("http://snomed.info/sct"), code = Code("12345"))
            )
        )
        val condition = Condition(
            subject = Reference(display = "Subject".asFHIR()),
            code = code
        )

        val (mappedResource, validation) = mapper.map(condition, unmappedTenant, null)
        mappedResource!!
        assertEquals(
            listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_CONDITION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, code)
                )
            ),
            mappedResource.extension
        )

        assertFalse(validation.hasIssues())
    }

    @Test
    fun `tenant in not mapped list with code and extensions`() {
        val code = CodeableConcept(
            coding = listOf(
                Coding(system = Uri("http://snomed.info/sct"), code = Code("12345"))
            )
        )
        val dataAuthorityExtension = Extension(
            url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/ronin-dataAuthorityIdentifier"),
            value = DynamicValue(
                type = DynamicValueType.IDENTIFIER,
                dataAuthorityIdentifier
            )
        )
        val condition = Condition(
            subject = Reference(display = "Subject".asFHIR()),
            code = code,
            extension = listOf(dataAuthorityExtension)
        )

        val (mappedResource, validation) = mapper.map(condition, unmappedTenant, null)
        mappedResource!!
        assertEquals(
            listOf(
                dataAuthorityExtension,
                Extension(
                    url = RoninExtension.TENANT_SOURCE_CONDITION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, code)
                )
            ),
            mappedResource.extension
        )

        assertFalse(validation.hasIssues())
    }

    @Test
    fun `null code on Condition`() {
        val condition = Condition(
            subject = Reference(display = "Subject".asFHIR())
        )

        val (mappedResource, validation) = mapper.map(condition, mappedTenant, null)
        assertEquals(condition, mappedResource)
        assertFalse(validation.hasIssues())
    }

    @Test
    fun `concept mapping finds no code`() {
        val code = CodeableConcept(
            coding = listOf(
                Coding(system = Uri("http://snomed.info/sct"), code = Code("12345"))
            )
        )
        val condition = Condition(
            subject = Reference(display = "Subject".asFHIR()),
            code = code
        )

        every { registryClient.getConceptMapping("tenant", "Condition.code", code, condition, null) } returns null

        val (mappedResource, validation) = mapper.map(condition, mappedTenant, null)
        assertEquals(condition, mappedResource)

        val exception = assertThrows<IllegalArgumentException> { validation.alertIfErrors() }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value '12345' has no target defined in any Condition.code concept map for tenant 'tenant' @ Condition.code",
            exception.message
        )
    }

    @Test
    fun `concept mapping finds code`() {
        val code = CodeableConcept(
            coding = listOf(
                Coding(system = Uri("http://snomed.info/sct"), code = Code("12345"))
            )
        )
        val condition = Condition(
            subject = Reference(display = "Subject".asFHIR()),
            code = code
        )

        val mappedCode = CodeableConcept(
            coding = listOf(
                Coding(system = Uri("http://snomed.info/sct"), code = Code("67890"))
            )
        )
        val mappedExtension = Extension(
            url = RoninExtension.TENANT_SOURCE_CONDITION_CODE.uri,
            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, code)
        )
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Condition.code",
                code,
                condition,
                null
            )
        } returns ConceptMapCodeableConcept(mappedCode, mappedExtension, listOf())

        val (mappedResource, validation) = mapper.map(condition, mappedTenant, null)
        mappedResource!!
        assertEquals(mappedCode, mappedResource.code)
        assertEquals(listOf(mappedExtension), mappedResource.extension)

        assertFalse(validation.hasIssues())
    }

    @Test
    fun `concept mapping finds code for condition with extensions`() {
        val code = CodeableConcept(
            coding = listOf(
                Coding(system = Uri("http://snomed.info/sct"), code = Code("12345"))
            )
        )
        val dataAuthorityExtension = Extension(
            url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/ronin-dataAuthorityIdentifier"),
            value = DynamicValue(
                type = DynamicValueType.IDENTIFIER,
                dataAuthorityIdentifier
            )
        )
        val condition = Condition(
            subject = Reference(display = "Subject".asFHIR()),
            code = code,
            extension = listOf(dataAuthorityExtension)
        )

        val mappedCode = CodeableConcept(
            coding = listOf(
                Coding(system = Uri("http://snomed.info/sct"), code = Code("67890"))
            )
        )
        val mappedExtension = Extension(
            url = RoninExtension.TENANT_SOURCE_CONDITION_CODE.uri,
            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, code)
        )
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Condition.code",
                code,
                condition,
                null
            )
        } returns ConceptMapCodeableConcept(mappedCode, mappedExtension, listOf())

        val (mappedResource, validation) = mapper.map(condition, mappedTenant, null)
        mappedResource!!
        assertEquals(mappedCode, mappedResource.code)
        assertEquals(listOf(dataAuthorityExtension, mappedExtension), mappedResource.extension)

        assertFalse(validation.hasIssues())
    }

    @Test
    fun `honors force cache reload when mapping`() {
        val code = CodeableConcept(
            coding = listOf(
                Coding(system = Uri("http://snomed.info/sct"), code = Code("12345"))
            )
        )
        val condition = Condition(
            subject = Reference(display = "Subject".asFHIR()),
            code = code
        )

        val cacheReload = LocalDateTime.now()

        val mappedCode = CodeableConcept(
            coding = listOf(
                Coding(system = Uri("http://snomed.info/sct"), code = Code("67890"))
            )
        )
        val mappedExtension = Extension(
            url = RoninExtension.TENANT_SOURCE_CONDITION_CODE.uri,
            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, code)
        )
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Condition.code",
                code,
                condition,
                cacheReload
            )
        } returns ConceptMapCodeableConcept(mappedCode, mappedExtension, listOf())

        val (mappedResource, validation) = mapper.map(condition, mappedTenant, cacheReload)
        mappedResource!!
        assertEquals(mappedCode, mappedResource.code)
        assertEquals(listOf(mappedExtension), mappedResource.extension)

        assertFalse(validation.hasIssues())
    }
}
