package com.projectronin.interop.rcdm.transform.map.resource

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.fhir.r4.valueset.DocumentReferenceStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.registry.model.ConceptMapCodeableConcept
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class DocumentReferenceMapperTest {
    private val registryClient = mockk<NormalizationRegistryClient>()
    private val mapper = DocumentReferenceMapper(registryClient)

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `no type provided`() {
        val documentReference = DocumentReference(
            status = DocumentReferenceStatus.CURRENT.asCode(),
            type = null
        )
        val (mappedResource, validation) = mapper.map(documentReference, tenant, null)

        assertEquals(documentReference, mappedResource)
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `no concept mapping found for type`() {
        val type = CodeableConcept(
            coding = listOf(
                Coding(system = Uri("http://loinc.org"), code = Code("34806-0"))
            )
        )
        val documentReference = DocumentReference(
            status = DocumentReferenceStatus.CURRENT.asCode(),
            type = type
        )

        every {
            registryClient.getConceptMapping(
                "test",
                "DocumentReference.type",
                type,
                documentReference,
                null
            )
        } returns null

        val (mappedResource, validation) = mapper.map(documentReference, tenant, null)

        assertEquals(documentReference, mappedResource)

        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR NOV_CONMAP_LOOKUP: Tenant source value '34806-0' has no target defined in any DocumentReference.type concept map for tenant 'test' @ DocumentReference.type",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `concept mapping found for type`() {
        val type = CodeableConcept(
            coding = listOf(
                Coding(system = Uri("http://loinc.org"), code = Code("34806-0"))
            )
        )
        val documentReference = DocumentReference(
            status = DocumentReferenceStatus.CURRENT.asCode(),
            type = type
        )

        val mappedType = CodeableConcept(
            coding = listOf(
                Coding(system = Uri("http://loinc.org"), code = Code("34806-1"))
            )
        )
        val mappedExtension = Extension(
            url = RoninExtension.TENANT_SOURCE_DOCUMENT_REFERENCE_TYPE.uri,
            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, type)
        )

        every {
            registryClient.getConceptMapping(
                "test",
                "DocumentReference.type",
                type,
                documentReference,
                null
            )
        } returns ConceptMapCodeableConcept(mappedType, mappedExtension, listOf())

        val (mappedResource, validation) = mapper.map(documentReference, tenant, null)

        mappedResource!!
        assertEquals(mappedType, mappedResource.type)
        assertEquals(listOf(mappedExtension), mappedResource.extension)

        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `honors forced cache reload`() {
        val type = CodeableConcept(
            coding = listOf(
                Coding(system = Uri("http://loinc.org"), code = Code("34806-0"))
            )
        )
        val documentReference = DocumentReference(
            status = DocumentReferenceStatus.CURRENT.asCode(),
            type = type
        )

        val mappedType = CodeableConcept(
            coding = listOf(
                Coding(system = Uri("http://loinc.org"), code = Code("34806-1"))
            )
        )
        val mappedExtension = Extension(
            url = RoninExtension.TENANT_SOURCE_DOCUMENT_REFERENCE_TYPE.uri,
            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, type)
        )

        val forceCacheReloadTS = LocalDateTime.now()

        every {
            registryClient.getConceptMapping(
                "test",
                "DocumentReference.type",
                type,
                documentReference,
                forceCacheReloadTS
            )
        } returns ConceptMapCodeableConcept(mappedType, mappedExtension, listOf())

        val (mappedResource, validation) = mapper.map(documentReference, tenant, forceCacheReloadTS)

        mappedResource!!
        assertEquals(mappedType, mappedResource.type)
        assertEquals(listOf(mappedExtension), mappedResource.extension)

        assertEquals(0, validation.issues().size)
    }
}
