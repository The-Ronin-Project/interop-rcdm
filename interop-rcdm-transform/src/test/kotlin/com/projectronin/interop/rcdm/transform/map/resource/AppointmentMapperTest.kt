package com.projectronin.interop.rcdm.transform.map.resource

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.registry.model.ConceptMapCoding
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AppointmentMapperTest {
    private val registryClient = mockk<NormalizationRegistryClient>()
    private val mapper = AppointmentMapper(registryClient)

    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "tenant"
        }

    @Test
    fun `supported resource is Appointment`() {
        assertEquals(Appointment::class, mapper.supportedResource)
    }

    @Test
    fun `concept mapping finds status`() {
        val status = Code("input")

        val appointment =
            Appointment(
                status = status,
                participant = emptyList(),
            )

        val mappedExtension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_APPOINTMENT_STATUS.uri,
                value = DynamicValue(DynamicValueType.CODING, status),
            )

        val mappedStatus = Code("booked")

        every {
            registryClient.getConceptMappingForEnum(
                tenantMnemonic = "tenant",
                elementName = "Appointment.status",
                coding = any(),
                enumClass = AppointmentStatus::class,
                enumExtensionUrl = RoninExtension.TENANT_SOURCE_APPOINTMENT_STATUS.value,
                resource = appointment,
            )
        } returns
            ConceptMapCoding(
                coding = Coding(code = Code("booked")),
                extension = mappedExtension,
                metadata = emptyList(),
            )

        val (mappedResource, validation) = mapper.map(appointment, tenant, null)
        mappedResource!!
        assertEquals(mappedStatus, mappedResource.status)
        assertEquals(listOf(mappedExtension), mappedResource.extension)

        assertFalse(validation.hasIssues())
    }

    @Test
    fun `concept mapping finds status with ID`() {
        val status = Code("input")

        val appointment =
            Appointment(
                status = status,
                participant = emptyList(),
            )

        val mappedExtension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_APPOINTMENT_STATUS.uri,
                value = DynamicValue(DynamicValueType.CODING, status),
            )

        every {
            registryClient.getConceptMappingForEnum(
                tenantMnemonic = "tenant",
                elementName = "Appointment.status",
                coding = any(),
                enumClass = AppointmentStatus::class,
                enumExtensionUrl = RoninExtension.TENANT_SOURCE_APPOINTMENT_STATUS.value,
                resource = appointment,
            )
        } returns
            ConceptMapCoding(
                coding = Coding(code = Code("booked"), id = "booked-id".asFHIR()),
                extension = mappedExtension,
                metadata = emptyList(),
            )

        val (mappedResource, validation) = mapper.map(appointment, tenant, null)
        mappedResource!!
        assertEquals(Code("booked", id = "booked-id".asFHIR()), mappedResource.status)
        assertEquals(listOf(mappedExtension), mappedResource.extension)

        assertFalse(validation.hasIssues())
    }

    @Test
    fun `null status stays`() {
        val status = Code(value = null)

        val appointment =
            Appointment(
                status = status,
                participant = emptyList(),
            )

        val (mappedResource, validation) = mapper.map(appointment, tenant, null)
        mappedResource!!
        assertNull(mappedResource.status?.value)

        assertFalse(validation.hasIssues())
    }
}
