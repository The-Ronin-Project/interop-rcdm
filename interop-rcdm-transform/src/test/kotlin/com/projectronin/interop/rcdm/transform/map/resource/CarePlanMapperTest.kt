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
import com.projectronin.interop.fhir.r4.resource.CarePlan
import com.projectronin.interop.fhir.r4.valueset.CarePlanIntent
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
class CarePlanMapperTest {
    private val registryClient = mockk<NormalizationRegistryClient>()
    private val mapper = CarePlanMapper(registryClient)
    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "tenant"
        }
    private val category1 =
        CodeableConcept(
            coding =
                listOf(
                    Coding(system = Uri("something-here-1"), code = Code("54321")),
                ),
        )
    private val category2 =
        CodeableConcept(
            coding =
                listOf(
                    Coding(system = Uri("something-here-2"), code = Code("87654")),
                ),
        )

    @Test
    fun `supported resource is CarePlan`() {
        assertEquals(CarePlan::class, mapper.supportedResource)
    }

    @Test
    fun `maps category`() {
        val carePlan =
            CarePlan(
                status = RequestStatus.ACTIVE.asCode(),
                category = listOf(category1),
                intent = CarePlanIntent.ORDER.asCode(),
                subject = Reference(reference = "Patient".asFHIR()),
            )
        val mappedCategory =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("something-mapped-here"), code = Code("89012")),
                    ),
            )
        val mappedExtensionsCategory =
            Extension(
                url = RoninExtension.TENANT_SOURCE_CARE_PLAN_CATEGORY.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, category1),
            )
        every {
            registryClient.getConceptMapping(
                "tenant",
                "CarePlan.category",
                category1,
                carePlan,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedCategory, mappedExtensionsCategory, listOf())
        val (mappedResource, validation) = mapper.map(carePlan, tenant, null)
        mappedResource!!
        assertEquals(listOf(mappedExtensionsCategory), mappedResource.extension)
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `maps multiple category`() {
        val carePlan =
            CarePlan(
                status = RequestStatus.ACTIVE.asCode(),
                category = listOf(category1, category2),
                intent = CarePlanIntent.ORDER.asCode(),
                subject = Reference(reference = "Patient".asFHIR()),
            )
        val mappedCategory1 =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("something-mapped-here-1"), code = Code("89012")),
                    ),
            )
        val mappedCategory2 =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("something-mapped-here-2"), code = Code("21098")),
                    ),
            )
        val mappedExtensionsCategory1 =
            Extension(
                url = RoninExtension.TENANT_SOURCE_CARE_PLAN_CATEGORY.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, category1),
            )
        val mappedExtensionsCategory2 =
            Extension(
                url = RoninExtension.TENANT_SOURCE_CARE_PLAN_CATEGORY.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, category2),
            )
        every {
            registryClient.getConceptMapping(
                "tenant",
                "CarePlan.category",
                category1,
                carePlan,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedCategory1, mappedExtensionsCategory1, listOf())
        every {
            registryClient.getConceptMapping(
                "tenant",
                "CarePlan.category",
                category2,
                carePlan,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedCategory2, mappedExtensionsCategory2, listOf())

        val (mappedResource, validation) = mapper.map(carePlan, tenant, null)
        mappedResource!!
        assertEquals(listOf(mappedExtensionsCategory1, mappedExtensionsCategory2), mappedResource.extension)
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `fails to map category`() {
        val carePlan =
            CarePlan(
                status = RequestStatus.ACTIVE.asCode(),
                category = listOf(category1),
                intent = CarePlanIntent.ORDER.asCode(),
                subject = Reference(reference = "Patient".asFHIR()),
            )

        every {
            registryClient.getConceptMapping(
                "tenant",
                "CarePlan.category",
                category1,
                carePlan,
                null,
            )
        } returns null

        val (mappedResource, validation) = mapper.map(carePlan, tenant, null)
        mappedResource!!
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR NOV_CONMAP_LOOKUP: Tenant source value '54321' has no target defined in any CarePlan.category concept map for tenant 'tenant' @ CarePlan.category",
            validation.issues().first().toString(),
        )
    }
}
