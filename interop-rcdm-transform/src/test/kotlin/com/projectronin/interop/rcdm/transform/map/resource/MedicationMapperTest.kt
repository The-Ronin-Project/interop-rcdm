package com.projectronin.interop.rcdm.transform.map.resource

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MedicationMapperTest {
    private val mapper = MedicationMapper(mockk())

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "tenant"
    }

    @Test
    fun `supported resource is Appointment`() {
        assertEquals(Medication::class, mapper.supportedResource)
    }

    @Test
    fun `copies extension if status is present`() {
        val mappedCode = CodeableConcept(text = "Cool Code".asFHIR())

        val mappedExtension = Extension(
            url = RoninExtension.TENANT_SOURCE_MEDICATION_CODE.uri,
            value = DynamicValue(
                type = DynamicValueType.CODEABLE_CONCEPT,
                value = mappedCode
            )
        )
        val medication = Medication(
            code = mappedCode
        )
        val (mappedResource, validation) = mapper.map(medication, tenant, null)
        mappedResource!!
        assertEquals(mappedCode, mappedResource.code)
        assertEquals(listOf(mappedExtension), mappedResource.extension)

        assertFalse(validation.hasIssues())
    }

    @Test
    fun `doesn't copy extension if status is absent`() {
        val medication = Medication()
        val (mappedResource, validation) = mapper.map(medication, tenant, null)
        mappedResource!!
        assertNull(mappedResource.code)
        assertEquals(0, mappedResource.extension.size)

        assertFalse(validation.hasIssues())
    }
}
