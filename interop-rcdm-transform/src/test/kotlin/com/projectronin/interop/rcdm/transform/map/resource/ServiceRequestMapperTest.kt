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
import com.projectronin.interop.fhir.r4.resource.ServiceRequest
import com.projectronin.interop.fhir.r4.valueset.RequestIntent
import com.projectronin.interop.fhir.r4.valueset.RequestStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.registry.model.ConceptMapCodeableConcept
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@Suppress("ktlint:standard:max-line-length")
class ServiceRequestMapperTest {
    private val registryClient = mockk<NormalizationRegistryClient>()
    private val mapper = ServiceRequestMapper(registryClient)
    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "tenant"
        }
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

    @Test
    fun `supported resource is ServiceRequest`() {
        assertEquals(ServiceRequest::class, mapper.supportedResource)
    }

    @Test
    fun `maps category and code - both required`() {
        val serviceRequest =
            ServiceRequest(
                status = RequestStatus.ACTIVE.asCode(),
                category = listOf(category),
                code = code,
                intent = RequestIntent.ORDER.asCode(),
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
                url = RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CODE.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, code),
            )
        val mappedExtensionsCategory =
            Extension(
                url = RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CATEGORY.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, code),
            )

        every {
            registryClient.getConceptMapping(
                "tenant",
                "ServiceRequest.code",
                code,
                serviceRequest,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedCode, mappedExtensionsCode, listOf())
        every {
            registryClient.getConceptMapping(
                "tenant",
                "ServiceRequest.category",
                category,
                serviceRequest,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedCategory, mappedExtensionsCategory, listOf())
        val (mappedResource, validation) = mapper.map(serviceRequest, tenant, null)
        mappedResource!!
        assertEquals(mappedCode, mappedResource.code)
        assertEquals(listOf(mappedExtensionsCategory, mappedExtensionsCode), mappedResource.extension)
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `fails to map category and code - both required`() {
        val serviceRequest =
            ServiceRequest(
                status = RequestStatus.ACTIVE.asCode(),
                category = listOf(category),
                code = code,
                intent = RequestIntent.ORDER.asCode(),
                subject = Reference(reference = "Patient".asFHIR()),
            )

        every {
            registryClient.getConceptMapping(
                "tenant",
                "ServiceRequest.code",
                code,
                serviceRequest,
                null,
            )
        } returns null
        every {
            registryClient.getConceptMapping(
                "tenant",
                "ServiceRequest.category",
                category,
                serviceRequest,
                null,
            )
        } returns null
        val (mappedResource, validation) = mapper.map(serviceRequest, tenant, null)
        mappedResource!!
        assertEquals(2, validation.issues().size)
        assertEquals(
            "ERROR NOV_CONMAP_LOOKUP: Tenant source value '54321' has no target defined in any ServiceRequest.category concept map for tenant 'tenant' @ ServiceRequest.category",
            validation.issues().first().toString(),
        )
        assertEquals(
            "ERROR NOV_CONMAP_LOOKUP: Tenant source value '12345' has no target defined in any ServiceRequest.code concept map for tenant 'tenant' @ ServiceRequest.code",
            validation.issues()[1].toString(),
        )
    }

    @Test
    fun `failed code mapping`() {
        val serviceRequest =
            ServiceRequest(
                status = RequestStatus.ACTIVE.asCode(),
                category = listOf(category),
                code = code,
                intent = RequestIntent.ORDER.asCode(),
                subject = Reference(reference = "Patient".asFHIR()),
            )
        val mappedCategory =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("something-here"), code = Code("89012")),
                    ),
            )
        val mappedExtensionsCategory =
            Extension(
                url = RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CATEGORY.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, code),
            )

        every {
            registryClient.getConceptMapping(
                "tenant",
                "ServiceRequest.code",
                code,
                serviceRequest,
                null,
            )
        } returns null

        every {
            registryClient.getConceptMapping(
                "tenant",
                "ServiceRequest.category",
                category,
                serviceRequest,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedCategory, mappedExtensionsCategory, listOf())

        val (mappedResource, validation) = mapper.map(serviceRequest, tenant, null)
        mappedResource!!
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR NOV_CONMAP_LOOKUP: Tenant source value '12345' has no target defined in any ServiceRequest.code concept map for tenant 'tenant' @ ServiceRequest.code",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `failed category mapping`() {
        val serviceRequest =
            ServiceRequest(
                status = RequestStatus.ACTIVE.asCode(),
                category = listOf(category),
                code = code,
                intent = RequestIntent.ORDER.asCode(),
                subject = Reference(reference = "Patient".asFHIR()),
            )
        val mappedCode =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("67890")),
                    ),
            )
        val mappedExtensionsCode =
            Extension(
                url = RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CODE.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, code),
            )
        every {
            registryClient.getConceptMapping(
                "tenant",
                "ServiceRequest.code",
                code,
                serviceRequest,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedCode, mappedExtensionsCode, listOf())
        every {
            registryClient.getConceptMapping(
                "tenant",
                "ServiceRequest.category",
                category,
                serviceRequest,
                null,
            )
        } returns null

        val (mappedResource, validation) = mapper.map(serviceRequest, tenant, null)
        mappedResource!!
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR NOV_CONMAP_LOOKUP: Tenant source value '54321' has no target defined in any ServiceRequest.category concept map for tenant 'tenant' @ ServiceRequest.category",
            validation.issues().first().toString(),
        )
    }
}
