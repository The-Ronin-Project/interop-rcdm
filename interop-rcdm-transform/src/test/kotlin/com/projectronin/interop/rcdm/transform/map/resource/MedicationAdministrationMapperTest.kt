package com.projectronin.interop.rcdm.transform.map.resource

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.MedicationAdministration
import com.projectronin.interop.fhir.r4.valueset.MedicationAdministrationStatus
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.registry.model.ConceptMapCoding
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class MedicationAdministrationMapperTest {
    private val registryClient = mockk<NormalizationRegistryClient>()
    private val mapper = MedicationAdministrationMapper(registryClient)

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "tenant"
    }

    @Test
    fun `supported resource is MedicationAdministration`() {
        assertEquals(MedicationAdministration::class, mapper.supportedResource)
    }

    @Test
    fun `concept mapping finds status`() {
        val status = Code("input")

        val medAdmin = MedicationAdministration(
            status = status
        )

        val mappedExtension = Extension(
            url = RoninExtension.TENANT_SOURCE_MEDICATION_ADMINISTRATION_STATUS.uri,
            value = DynamicValue(DynamicValueType.CODING, status)
        )

        val mappedStatus = Code("completed")

        every {
            registryClient.getConceptMappingForEnum(
                tenantMnemonic = "tenant",
                elementName = "MedicationAdministration.status",
                coding = any(),
                enumClass = MedicationAdministrationStatus::class,
                enumExtensionUrl = RoninExtension.TENANT_SOURCE_MEDICATION_ADMINISTRATION_STATUS.value,
                resource = medAdmin
            )
        } returns ConceptMapCoding(
            coding = Coding(code = Code("completed")),
            extension = mappedExtension,
            metadata = emptyList()
        )

        val (mappedResource, validation) = mapper.map(medAdmin, tenant, null)
        mappedResource!!
        assertEquals(mappedStatus, mappedResource.status)
        assertEquals(listOf(mappedExtension), mappedResource.extension)

        assertFalse(validation.hasIssues())
    }

    @Test
    fun `null status stays`() {
        val status = Code(value = null)

        val medAdmin = MedicationAdministration(
            status = status
        )

        val (mappedResource, validation) = mapper.map(medAdmin, tenant, null)
        mappedResource!!
        Assertions.assertNull(mappedResource.status?.value)

        Assertions.assertFalse(validation.hasIssues())
    }
}
