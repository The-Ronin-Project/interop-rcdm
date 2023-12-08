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
import com.projectronin.interop.fhir.r4.resource.Procedure
import com.projectronin.interop.fhir.r4.valueset.EventStatus
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.registry.model.ConceptMapCodeableConcept
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@Suppress("ktlint:standard:max-line-length")
class ProcedureMapperTest {
    private val registryClient = mockk<NormalizationRegistryClient>()
    private val mapper = ProcedureMapper(registryClient)

    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "tenant"
        }

    @Test
    fun `supported resource is Procedure`() {
        assertEquals(Procedure::class, mapper.supportedResource)
    }

    @Test
    fun `maps when no mappable elements`() {
        val procedure =
            Procedure(
                status = Code(EventStatus.COMPLETED.code),
                code = null,
                category = null,
                subject = Reference(reference = "Patient".asFHIR()),
            )

        val (mappedResource, validation) = mapper.map(procedure, tenant, null)
        assertEquals(procedure, mappedResource)
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `maps code`() {
        val code =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val procedure =
            Procedure(
                status = Code(EventStatus.COMPLETED.code),
                code = code,
                subject = Reference(reference = "Patient".asFHIR()),
            )

        val mappedCode =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("67890")),
                    ),
            )
        val mappedExtension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_PROCEDURE_CODE.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, code),
            )
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Procedure.code",
                code,
                procedure,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedCode, mappedExtension, listOf())

        val (mappedResource, validation) = mapper.map(procedure, tenant, null)
        mappedResource!!
        assertEquals(mappedCode, mappedResource.code)
        assertEquals(listOf(mappedExtension), mappedResource.extension)
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `maps category and code`() {
        val code =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val category =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("something-here"), code = Code("54321")),
                    ),
            )
        val procedure =
            Procedure(
                status = Code(EventStatus.COMPLETED.code),
                code = code,
                category = category,
                subject = Reference(reference = "Patient".asFHIR()),
            )

        val mappedCode =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("67890")),
                    ),
            )
        val mappedCategory =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("something-here"), code = Code("89012")),
                    ),
            )
        val mappedExtensionsCode =
            Extension(
                url = RoninExtension.TENANT_SOURCE_PROCEDURE_CODE.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, code),
            )
        val mappedExtensionsCategory =
            Extension(
                url = RoninExtension.TENANT_SOURCE_PROCEDURE_CATEGORY.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, code),
            )

        every {
            registryClient.getConceptMapping(
                "tenant",
                "Procedure.code",
                code,
                procedure,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedCode, mappedExtensionsCode, listOf())
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Procedure.category",
                category,
                procedure,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedCategory, mappedExtensionsCategory, listOf())
        val (mappedResource, validation) = mapper.map(procedure, tenant, null)
        mappedResource!!
        assertEquals(mappedCode, mappedResource.code)
        assertEquals(listOf(mappedExtensionsCode, mappedExtensionsCategory), mappedResource.extension)
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `failed code mapping`() {
        val code =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val procedure =
            Procedure(
                status = Code(EventStatus.COMPLETED.code),
                code = code,
                subject = Reference(reference = "Patient".asFHIR()),
            )

        every {
            registryClient.getConceptMapping(
                "tenant",
                "Procedure.code",
                code,
                procedure,
                null,
            )
        } returns null

        val (mappedResource, validation) = mapper.map(procedure, tenant, null)
        assertEquals(procedure, mappedResource)

        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR NOV_CONMAP_LOOKUP: Tenant source value '12345' has no target defined in any Procedure.code concept map for tenant 'tenant' @ Procedure.code",
            validation.issues().first().toString(),
        )
    }
}
