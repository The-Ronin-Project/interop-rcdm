package com.projectronin.interop.rcdm.transform.map.element

import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ContactPointMapperTest {
    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "test"
        }
    private val patient = mockk<Patient>()

    private val registryClient = mockk<NormalizationRegistryClient>()
    private val mapper = ContactPointMapper(registryClient)

    @Test
    fun `maps with null system`() {
        val initial =
            ContactPoint(
                system = null,
            )
        val validation = Validation()

        val mapped = mapper.map(initial, patient, tenant, LocationContext(Patient::class), validation, null)
        assertEquals(initial, mapped)
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `maps with null system value`() {
        val initial =
            ContactPoint(
                system = Code(null),
            )
        val validation = Validation()

        val mapped = mapper.map(initial, patient, tenant, LocationContext(Patient::class), validation, null)
        assertEquals(initial, mapped)
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `maps with system value that has no mapping`() {
        val initial =
            ContactPoint(
                system = Code("unmapped-system"),
            )
        val validation = Validation()

        every {
            registryClient.getConceptMappingForEnum(
                "test",
                "Patient.telecom.system",
                any(),
                ContactPointSystem::class,
                RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                patient,
                null,
            )
        } returns null

        val mapped = mapper.map(initial, patient, tenant, LocationContext(Patient::class), validation, null)
        assertEquals(initial, mapped)
        assertEquals(1, validation.issues().size)
    }

    @Test
    fun `maps with system value that is mapped`() {
        val initial =
            ContactPoint(
                system = Code("mapped-system"),
            )
        val validation = Validation()

        val mockExtension = mockk<Extension>()
        every {
            registryClient.getConceptMappingForEnum(
                "test",
                "Patient.telecom.system",
                any(),
                ContactPointSystem::class,
                RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                patient,
                null,
            )
        } returns
            mockk {
                every { coding.code?.value } returns "email"
                every { extension } returns mockExtension
                every { metadata } returns listOf()
            }

        val mapped = mapper.map(initial, patient, tenant, LocationContext(Patient::class), validation, null)

        val expected =
            ContactPoint(
                system = Code(value = "email", extension = listOf(mockExtension)),
            )
        assertEquals(expected, mapped)
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `maps with null use`() {
        val initial =
            ContactPoint(
                use = null,
            )
        val validation = Validation()

        val mapped = mapper.map(initial, patient, tenant, LocationContext(Patient::class), validation, null)
        assertEquals(initial, mapped)
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `maps with null use value`() {
        val initial =
            ContactPoint(
                use = Code(null),
            )
        val validation = Validation()

        val mapped = mapper.map(initial, patient, tenant, LocationContext(Patient::class), validation, null)
        assertEquals(initial, mapped)
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `maps with use value that has no mapping`() {
        val initial =
            ContactPoint(
                use = Code("unmapped-use"),
            )
        val validation = Validation()

        every {
            registryClient.getConceptMappingForEnum(
                "test",
                "Patient.telecom.use",
                any(),
                ContactPointUse::class,
                RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                patient,
                null,
            )
        } returns null

        val mapped = mapper.map(initial, patient, tenant, LocationContext(Patient::class), validation, null)
        assertEquals(initial, mapped)
        assertEquals(1, validation.issues().size)
    }

    @Test
    fun `maps with use value that is mapped`() {
        val initial =
            ContactPoint(
                use = Code("mapped-use"),
            )
        val validation = Validation()

        val mockExtension = mockk<Extension>()
        every {
            registryClient.getConceptMappingForEnum(
                "test",
                "Patient.telecom.use",
                any(),
                ContactPointUse::class,
                RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                patient,
                null,
            )
        } returns
            mockk {
                every { coding.code?.value } returns "temp"
                every { extension } returns mockExtension
                every { metadata } returns listOf()
            }

        val mapped = mapper.map(initial, patient, tenant, LocationContext(Patient::class), validation, null)

        val expected =
            ContactPoint(
                use = Code(value = "temp", extension = listOf(mockExtension)),
            )
        assertEquals(expected, mapped)
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `maps with use and system that are mapped`() {
        val initial =
            ContactPoint(
                system = Code("mapped-system"),
                use = Code("mapped-use"),
            )
        val validation = Validation()

        val mockSystemExtension = mockk<Extension>()
        every {
            registryClient.getConceptMappingForEnum(
                "test",
                "Patient.telecom.system",
                any(),
                ContactPointSystem::class,
                RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                patient,
                null,
            )
        } returns
            mockk {
                every { coding.code?.value } returns "email"
                every { extension } returns mockSystemExtension
                every { metadata } returns listOf()
            }

        val mockUseExtension = mockk<Extension>()
        every {
            registryClient.getConceptMappingForEnum(
                "test",
                "Patient.telecom.use",
                any(),
                ContactPointUse::class,
                RoninExtension.TENANT_SOURCE_TELECOM_USE.value,
                patient,
                null,
            )
        } returns
            mockk {
                every { coding.code?.value } returns "temp"
                every { extension } returns mockUseExtension
                every { metadata } returns listOf()
            }

        val mapped = mapper.map(initial, patient, tenant, LocationContext(Patient::class), validation, null)

        val expected =
            ContactPoint(
                system = Code(value = "email", extension = listOf(mockSystemExtension)),
                use = Code(value = "temp", extension = listOf(mockUseExtension)),
            )
        assertEquals(expected, mapped)
        assertEquals(0, validation.issues().size)
    }
}
