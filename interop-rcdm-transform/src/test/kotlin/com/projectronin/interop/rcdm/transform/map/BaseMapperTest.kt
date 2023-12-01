package com.projectronin.interop.rcdm.transform.map

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.registry.model.ConceptMapCodeableConcept
import com.projectronin.interop.rcdm.registry.model.ConceptMapCoding
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.reflect.KProperty1

class BaseMapperTest {
    private val registryClient = mockk<NormalizationRegistryClient>()
    private val nonReifiedMapper = object : BaseMapper<Patient>(registryClient) {
        fun <R : Resource<R>> conceptMap(
            codeableConcept: CodeableConcept,
            elementProperty: KProperty1<Patient, *>,
            resource: R,
            tenant: Tenant,
            parentContext: LocationContext,
            validation: Validation,
            forceCacheReloadTS: LocalDateTime?
        ): ConceptMapCodeableConcept? {
            return getConceptMapping(
                codeableConcept,
                elementProperty,
                resource,
                tenant,
                parentContext,
                validation,
                forceCacheReloadTS
            )
        }
    }

    private val contactPointMapper = object : BaseMapper<ContactPoint>(registryClient) {
        fun conceptMapUse(
            value: String,
            elementProperty: KProperty1<ContactPoint, *>,
            elementName: String,
            resource: Patient,
            extension: RoninExtension,
            tenant: Tenant,
            parentContext: LocationContext,
            validation: Validation,
            forceCacheReloadTS: LocalDateTime?
        ): ConceptMapCoding? {
            return getConceptMappingForEnum<ContactPointUse, Patient>(
                value,
                elementProperty,
                elementName,
                resource,
                extension,
                tenant,
                parentContext,
                validation,
                forceCacheReloadTS
            )
        }
    }

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `getConceptMapping returns the concept mapping from registry client`() {
        val initialCodeableContext = mockk<CodeableConcept> {
            every { coding } returns listOf(
                mockk {
                    every { code?.value } returns "value"
                }
            )
        }
        val property = Patient::maritalStatus
        val patient = mockk<Patient>()
        val context = LocationContext(Patient::class)
        val validation = Validation()

        val mappedResult = mockk<ConceptMapCodeableConcept> {
            every { metadata } returns listOf(
                mockk()
            )
        }
        every {
            registryClient.getConceptMapping(
                "test",
                "Patient.maritalStatus",
                initialCodeableContext,
                patient,
                null
            )
        } returns mappedResult

        val result = nonReifiedMapper.conceptMap(
            initialCodeableContext,
            property,
            patient,
            tenant,
            context,
            validation,
            null
        )
        assertEquals(mappedResult, result)

        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `getConceptMapping reports a validation error if lookup fails`() {
        val initialCodeableContext = mockk<CodeableConcept> {
            every { coding } returns listOf(
                mockk {
                    every { code?.value } returns "value"
                }
            )
        }
        val property = Patient::maritalStatus
        val patient = mockk<Patient>()
        val context = LocationContext(Patient::class)
        val validation = Validation()

        every {
            registryClient.getConceptMapping(
                "test",
                "Patient.maritalStatus",
                initialCodeableContext,
                patient,
                null
            )
        } returns null

        val result = nonReifiedMapper.conceptMap(
            initialCodeableContext,
            property,
            patient,
            tenant,
            context,
            validation,
            null
        )
        assertNull(result)

        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR NOV_CONMAP_LOOKUP: Tenant source value 'value' has no target defined in any Patient.maritalStatus concept map for tenant 'test' @ Patient.maritalStatus",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `getConceptMappingForEnum finds no mapping`() {
        val value = "unknown-use"
        val property = ContactPoint::use
        val elementName = "use"
        val patient = mockk<Patient>()
        val extension = RoninExtension.TENANT_SOURCE_TELECOM_USE
        val context = LocationContext("Patient", "telecom[0]")
        val validation = Validation()

        val valueCoding = Coding(
            system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
            code = Code(value = value)
        )
        every {
            registryClient.getConceptMappingForEnum(
                "test",
                elementName,
                valueCoding,
                ContactPointUse::class,
                extension.value,
                patient,
                null
            )
        } returns null

        val response = contactPointMapper.conceptMapUse(
            value,
            property,
            elementName,
            patient,
            extension,
            tenant,
            context,
            validation,
            null
        )
        assertNull(response)

        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR NOV_CONMAP_LOOKUP: Tenant source value '$value' has no target defined in http://projectronin.io/fhir/CodeSystem/test/ContactPointUse @ Patient.telecom[0].use",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `getConceptMappingForEnum finds mapping with invalid enum`() {
        val value = "unknown-use"
        val property = ContactPoint::use
        val elementName = "use"
        val patient = mockk<Patient>()
        val extension = RoninExtension.TENANT_SOURCE_TELECOM_USE
        val context = LocationContext("Patient", "telecom[0]")
        val validation = Validation()

        val valueCoding = Coding(
            system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
            code = Code(value = value)
        )

        val conceptMapCoding = mockk<ConceptMapCoding> {
            every { coding.code?.value } returns "still-invalid-use"
            every { metadata } returns listOf(
                mockk()
            )
        }
        every {
            registryClient.getConceptMappingForEnum(
                "test",
                elementName,
                valueCoding,
                ContactPointUse::class,
                extension.value,
                patient,
                null
            )
        } returns conceptMapCoding

        val response = contactPointMapper.conceptMapUse(
            value,
            property,
            elementName,
            patient,
            extension,
            tenant,
            context,
            validation,
            null
        )
        assertEquals(conceptMapCoding, response)

        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR INV_CONMAP_VALUE_SET: http://projectronin.io/fhir/CodeSystem/test/ContactPointUse mapped '$value' to 'still-invalid-use' which is outside of required value set @ Patient.telecom[0].use",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `getConceptMappingForEnum finds mapping with valid enum`() {
        val value = "unknown-use"
        val property = ContactPoint::use
        val elementName = "use"
        val patient = mockk<Patient>()
        val extension = RoninExtension.TENANT_SOURCE_TELECOM_USE
        val context = LocationContext("Patient", "telecom[0]")
        val validation = Validation()

        val valueCoding = Coding(
            system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
            code = Code(value = value)
        )

        val conceptMapCoding = mockk<ConceptMapCoding> {
            every { coding.code?.value } returns ContactPointUse.HOME.code
            every { metadata } returns listOf(
                mockk()
            )
        }
        every {
            registryClient.getConceptMappingForEnum(
                "test",
                elementName,
                valueCoding,
                ContactPointUse::class,
                extension.value,
                patient,
                null
            )
        } returns conceptMapCoding

        val response = contactPointMapper.conceptMapUse(
            value,
            property,
            elementName,
            patient,
            extension,
            tenant,
            context,
            validation,
            null
        )
        assertEquals(conceptMapCoding, response)

        assertEquals(0, validation.issues().size)
    }
}
