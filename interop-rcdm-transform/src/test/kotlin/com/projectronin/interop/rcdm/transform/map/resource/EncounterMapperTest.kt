package com.projectronin.interop.rcdm.transform.map.resource

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Encounter
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class EncounterMapperTest {
    private val registryClient = mockk<NormalizationRegistryClient>()
    private val mapper = EncounterMapper(registryClient)

    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "tenant"
        }

    @Test
    fun `supported resource is Encounter`() {
        assertEquals(Encounter::class, mapper.supportedResource)
    }

    @Test
    fun `concept mapping adds class extension`() {
        val status = Code("input")
        val coding = Coding(FHIRString("coding"))

        val encounter =
            Encounter(
                status = status,
                `class` = coding,
            )

        val mappedExtension =
            Extension(
                url = Uri(RoninExtension.TENANT_SOURCE_ENCOUNTER_CLASS.value),
                value =
                    DynamicValue(
                        type = DynamicValueType.CODING,
                        value = coding,
                    ),
            )

        val (mappedResource, validation) = mapper.map(encounter, tenant, null)
        mappedResource!!
        assertEquals(listOf(mappedExtension), mappedResource.extension)

        assertFalse(validation.hasIssues())
    }
}
