package com.projectronin.interop.rcdm.transform.map

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.element.Element
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MappingServiceTest {
    private val tenant = mockk<Tenant>()
    private val validation = mockk<Validation>()

    @Test
    fun `no resource mapper defined for resource`() {
        val patient = Patient()

        val service = MappingService(listOf(), listOf())
        val exception = assertThrows<IllegalStateException> {
            service.map(patient, tenant, null)
        }
        assertEquals("No ResourceMapper defined for Patient", exception.message)
    }

    @Test
    fun `resource mapper returns null mapped resource`() {
        val patient = Patient()
        val mapper = mockk<ResourceMapper<Patient>> {
            every { supportedResource } returns Patient::class
            every { map(patient, tenant, null) } returns MapResponse(null, validation)
        }

        val service = MappingService(listOf(mapper), listOf())
        val response = service.map(patient, tenant, null)
        assertNull(response.mappedResource)
        assertEquals(validation, response.validation)
    }

    @Test
    fun `primitive elements are not changed`() {
        val resource = StringValueResource(FHIRString("example"))

        val resourceMapper = mockk<ResourceMapper<StringValueResource>> {
            every { supportedResource } returns StringValueResource::class
            every { map(resource, tenant, null) } returns MapResponse(resource, validation)
        }

        val fhirStringMapper = mockk<ElementMapper<FHIRString>> {
            every { supportedElement } returns FHIRString::class
        }

        val service = MappingService(listOf(resourceMapper), listOf(fhirStringMapper))
        val response = service.map(resource, tenant, null)
        assertEquals(resource, response.mappedResource)
        assertEquals(validation, response.validation)
    }

    @Test
    fun `elements attempt to map their properties when no element mapper defined`() {
        val element1 = NestedElement(FHIRString("value"))
        val element2 = NestingElement1(element1)
        val resource = NestedElementResource(element2)

        val resourceMapper = mockk<ResourceMapper<NestedElementResource>> {
            every { supportedResource } returns NestedElementResource::class
            every { map(resource, tenant, null) } returns MapResponse(resource, validation)
        }

        val newElement1 = NestedElement(FHIRString("new value"))
        val element1Mapper = mockk<ElementMapper<NestedElement>> {
            every { supportedElement } returns NestedElement::class
            every { map(element1, resource, tenant, any(), validation, null) } returns newElement1
        }

        val service = MappingService(listOf(resourceMapper), listOf(element1Mapper))
        val response = service.map(resource, tenant, null)

        val expectedResource = NestedElementResource(
            element = NestingElement1(
                element = newElement1
            )
        )
        assertEquals(expectedResource, response.mappedResource)
        assertEquals(validation, response.validation)
    }

    @Test
    fun `elements attempt to map their properties when element mapper defined`() {
        val element1 = NestedElement(FHIRString("value"))
        val element2 = NestingElement2(element1, FHIRString("other"))
        val resource = NestedElementResource2(element2)

        val resourceMapper = mockk<ResourceMapper<NestedElementResource2>> {
            every { supportedResource } returns NestedElementResource2::class
            every { map(resource, tenant, null) } returns MapResponse(resource, validation)
        }

        val newElement2 = NestingElement2(element1, FHIRString("new other"))
        val element2Mapper = mockk<ElementMapper<NestingElement2>> {
            every { supportedElement } returns NestingElement2::class
            every { map(element2, resource, tenant, any(), validation, null) } returns newElement2
        }

        val newElement1 = NestedElement(FHIRString("new value"))
        val element1Mapper = mockk<ElementMapper<NestedElement>> {
            every { supportedElement } returns NestedElement::class
            every { map(element1, resource, tenant, any(), validation, null) } returns newElement1
        }

        val service = MappingService(listOf(resourceMapper), listOf(element2Mapper, element1Mapper))
        val response = service.map(resource, tenant, null)

        val expectedResource = NestedElementResource2(
            element = NestingElement2(
                element = newElement1,
                other = FHIRString("new other")
            )
        )
        assertEquals(expectedResource, response.mappedResource)
        assertEquals(validation, response.validation)
    }

    @Test
    fun `elements are unchanged when element mapper returns null mapping`() {
        val element1 = NestedElement(FHIRString("value"))
        val element2 = NestingElement2(element1, FHIRString("other"))
        val resource = NestedElementResource2(element2)

        val resourceMapper = mockk<ResourceMapper<NestedElementResource2>> {
            every { supportedResource } returns NestedElementResource2::class
            every { map(resource, tenant, null) } returns MapResponse(resource, validation)
        }

        val element2Mapper = mockk<ElementMapper<NestingElement2>> {
            every { supportedElement } returns NestingElement2::class
            every { map(element2, resource, tenant, any(), validation, null) } returns null
        }

        val element1Mapper = mockk<ElementMapper<NestedElement>> {
            every { supportedElement } returns NestedElement::class
        }

        val service = MappingService(listOf(resourceMapper), listOf(element2Mapper, element1Mapper))
        val response = service.map(resource, tenant, null)

        assertEquals(resource, response.mappedResource)
        assertEquals(validation, response.validation)
    }

    @Test
    fun `dynamic values are not changed when not representing elements`() {
        val resource = DynamicValueResource(DynamicValue(DynamicValueType.STRING, FHIRString("example")))

        val resourceMapper = mockk<ResourceMapper<DynamicValueResource>> {
            every { supportedResource } returns DynamicValueResource::class
            every { map(resource, tenant, null) } returns MapResponse(resource, validation)
        }

        val fhirStringMapper = mockk<ElementMapper<FHIRString>> {
            every { supportedElement } returns FHIRString::class
        }

        val service = MappingService(listOf(resourceMapper), listOf(fhirStringMapper))
        val response = service.map(resource, tenant, null)
        assertEquals(resource, response.mappedResource)
        assertEquals(validation, response.validation)
    }

    @Test
    fun `dynamic values map their element-based values with mappers defined`() {
        val codeableConcept = CodeableConcept(text = FHIRString("text"))
        val resource = DynamicValueResource(DynamicValue(DynamicValueType.CODEABLE_CONCEPT, codeableConcept))

        val resourceMapper = mockk<ResourceMapper<DynamicValueResource>> {
            every { supportedResource } returns DynamicValueResource::class
            every { map(resource, tenant, null) } returns MapResponse(resource, validation)
        }

        val newCodeableConcept = CodeableConcept(text = FHIRString("new text"))
        val codeableConceptMapper = mockk<ElementMapper<CodeableConcept>> {
            every { supportedElement } returns CodeableConcept::class
            every { map(codeableConcept, resource, tenant, any(), validation, null) } returns newCodeableConcept
        }

        val service = MappingService(listOf(resourceMapper), listOf(codeableConceptMapper))
        val response = service.map(resource, tenant, null)

        val expectedResource = DynamicValueResource(
            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, newCodeableConcept)
        )
        assertEquals(expectedResource, response.mappedResource)
        assertEquals(validation, response.validation)
    }

    @Test
    fun `collection elements will not attempt mapping for primitive items`() {
        val resource = PrimitiveListResource(listOf(FHIRString("example"), FHIRString("example2")))

        val resourceMapper = mockk<ResourceMapper<PrimitiveListResource>> {
            every { supportedResource } returns PrimitiveListResource::class
            every { map(resource, tenant, null) } returns MapResponse(resource, validation)
        }

        val fhirStringMapper = mockk<ElementMapper<FHIRString>> {
            every { supportedElement } returns FHIRString::class
        }

        val service = MappingService(listOf(resourceMapper), listOf(fhirStringMapper))
        val response = service.map(resource, tenant, null)
        assertEquals(resource, response.mappedResource)
        assertEquals(validation, response.validation)
    }

    @Test
    fun `collection elements will attempt mapping for element-based items with mappers defined`() {
        val codeableConcept1 = CodeableConcept(text = FHIRString("text"))
        val codeableConcept2 = CodeableConcept(text = FHIRString("text2"))
        val resource = ElementListResource(listOf(codeableConcept1, codeableConcept2))

        val resourceMapper = mockk<ResourceMapper<ElementListResource>> {
            every { supportedResource } returns ElementListResource::class
            every { map(resource, tenant, null) } returns MapResponse(resource, validation)
        }

        val newCodeableConcept1 = CodeableConcept(text = FHIRString("new text"))
        val codeableConceptMapper = mockk<ElementMapper<CodeableConcept>> {
            every { supportedElement } returns CodeableConcept::class
            every { map(codeableConcept1, resource, tenant, any(), validation, null) } returns newCodeableConcept1
            every { map(codeableConcept2, resource, tenant, any(), validation, null) } returns null
        }

        val service = MappingService(listOf(resourceMapper), listOf(codeableConceptMapper))
        val response = service.map(resource, tenant, null)

        val expectedResource = ElementListResource(listOf(newCodeableConcept1, codeableConcept2))
        assertEquals(expectedResource, response.mappedResource)
        assertEquals(validation, response.validation)
    }

    data class StringValueResource(
        val value: FHIRString
    ) : DefaultResource<StringValueResource>()

    data class NestedElementResource(
        val element: NestingElement1
    ) : DefaultResource<NestedElementResource>()

    data class NestedElementResource2(
        val element: NestingElement2
    ) : DefaultResource<NestedElementResource2>()

    data class DynamicValueResource(
        val value: DynamicValue<Any>
    ) : DefaultResource<DynamicValueResource>()

    data class PrimitiveListResource(
        val value: List<FHIRString>
    ) : DefaultResource<PrimitiveListResource>()

    data class ElementListResource(
        val value: List<CodeableConcept>
    ) : DefaultResource<ElementListResource>()

    data class NestedElement(
        val string: FHIRString
    ) : DefaultElement<NestedElement>()

    data class NestingElement1(
        val element: NestedElement
    ) : DefaultElement<NestingElement1>()

    data class NestingElement2(
        val element: NestedElement,
        val other: FHIRString
    ) : DefaultElement<NestingElement2>()

    abstract class DefaultResource<T : Resource<T>> : Resource<T> {
        override val id: Id? = null
        override val implicitRules: Uri? = null
        override val language: Code? = null
        override var meta: Meta? = null
        override val resourceType: String = "TestResource"
    }

    abstract class DefaultElement<E : Element<E>> : Element<E> {
        override val extension: List<Extension> = listOf()
        override val id: FHIRString? = null
    }
}
