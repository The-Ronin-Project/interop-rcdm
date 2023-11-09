package com.projectronin.interop.rcdm.validate

import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.validation.ValidationClient
import com.projectronin.interop.rcdm.validate.element.ElementValidator
import com.projectronin.interop.rcdm.validate.profile.ProfileValidator
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import com.projectronin.interop.fhir.validate.ProfileValidator as R4ProfileValidator

class ValidationServiceTest {
    private val r4ProfileTransformer = mockk<R4ProfileValidator<Patient>> {
        every { validate(any(), LocationContext(Patient::class)) } returns Validation()
    }

    private val profileValidator = mockk<ProfileValidator<Patient>> {
        every { supportedResource } returns Patient::class
        every { r4Validator } returns r4ProfileTransformer
        every { profile } returns RoninProfile.PATIENT
        every { qualifies(any()) } returns true
    }

    private val validationError =
        FHIRError(severity = ValidationIssueSeverity.ERROR, code = "Error", description = "Error!", location = null)
    private val failedValidation = Validation().apply { checkTrue(false, validationError, null) }

    private val validationClient = mockk<ValidationClient> {
        every { reportIssues(any(), any<Patient>(), any()) } answers { UUID.randomUUID() }
    }

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `no validators defined for resource`() {
        val patient = Patient()

        val service = ValidationService(validationClient, listOf(), listOf())
        val exception = assertThrows<IllegalStateException> { service.validate(patient, tenant) }
        assertEquals("No Validators found for Patient", exception.message)
    }

    @Test
    fun `no qualifying validators`() {
        val meta = Meta(
            profile = listOf(Canonical("http://example.org/profile"))
        )
        val patient = Patient(
            id = Id("1234"),
            meta = meta
        )

        val profileValidator = mockk<ProfileValidator<Patient>> {
            every { supportedResource } returns Patient::class
            every { qualifies(patient) } returns false
        }

        val service = ValidationService(validationClient, listOf(profileValidator), listOf())
        val exception = assertThrows<IllegalStateException> { service.validate(patient, tenant) }
        assertEquals("No qualified validators found for Patient with id 1234 and meta $meta", exception.message)
    }

    @Test
    fun `resource validator returns validation error`() {
        val patient = Patient()

        every { profileValidator.validate(patient, LocationContext(Patient::class)) } returns failedValidation

        val service = ValidationService(validationClient, listOf(profileValidator), listOf())
        val response = service.validate(patient, tenant)
        assertFalse(response)

        val validation = slot<Validation>()
        verify(exactly = 1) { validationClient.reportIssues(capture(validation), patient, "test") }
        assertEquals(1, validation.captured.issues().size)
    }

    @Test
    fun `resource validator does not return validation error`() {
        val patient = Patient()

        every { profileValidator.validate(patient, LocationContext(Patient::class)) } returns Validation()

        val service = ValidationService(validationClient, listOf(profileValidator), listOf())
        val response = service.validate(patient, tenant)
        assertTrue(response)

        verify { validationClient wasNot Called }

        verify(exactly = 1) { profileValidator.qualifies(patient) }
        verify(exactly = 1) { profileValidator.validate(patient, LocationContext(Patient::class)) }
    }

    @Test
    fun `validates against multiple qualifying resource validators`() {
        val patient = Patient()

        every { profileValidator.validate(patient, LocationContext(Patient::class)) } returns Validation()

        val service = ValidationService(validationClient, listOf(profileValidator, profileValidator), listOf())
        val response = service.validate(patient, tenant)
        assertTrue(response)

        verify { validationClient wasNot Called }

        verify(exactly = 2) { profileValidator.qualifies(patient) }
        verify(exactly = 2) { profileValidator.validate(patient, LocationContext(Patient::class)) }
    }

    @Test
    fun `resource validates against R4 when single validator found`() {
        val patient = Patient()

        every { profileValidator.validate(patient, LocationContext(Patient::class)) } returns Validation()

        val service = ValidationService(validationClient, listOf(profileValidator), listOf())
        val response = service.validate(patient, tenant)
        assertTrue(response)

        verify { validationClient wasNot Called }

        verify(exactly = 1) { r4ProfileTransformer.validate(patient, LocationContext(Patient::class)) }
    }

    @Test
    fun `resource validates against R4 and includes issues when single validator found`() {
        val patient = Patient()

        every { profileValidator.validate(patient, LocationContext(Patient::class)) } returns Validation()

        every { r4ProfileTransformer.validate(patient, LocationContext(Patient::class)) } returns failedValidation

        val service = ValidationService(validationClient, listOf(profileValidator), listOf())
        val response = service.validate(patient, tenant)
        assertFalse(response)

        val validation = slot<Validation>()
        verify(exactly = 1) { validationClient.reportIssues(capture(validation), patient, "test") }
        assertEquals(1, validation.captured.issues().size)

        verify(exactly = 1) { r4ProfileTransformer.validate(patient, LocationContext(Patient::class)) }
    }

    @Test
    fun `resource validates against single R4 with multiple validators with same R4 validator found`() {
        val patient = Patient()

        every { profileValidator.validate(patient, LocationContext(Patient::class)) } returns Validation()

        val service = ValidationService(validationClient, listOf(profileValidator, profileValidator), listOf())
        val response = service.validate(patient, tenant)
        assertTrue(response)

        verify { validationClient wasNot Called }

        verify(exactly = 2) { profileValidator.qualifies(patient) }
        verify(exactly = 2) { profileValidator.validate(patient, LocationContext(Patient::class)) }
        verify(exactly = 1) { r4ProfileTransformer.validate(patient, LocationContext(Patient::class)) }
    }

    @Test
    fun `resource validates against multiple R4 validators`() {
        val patient = Patient()

        every { profileValidator.validate(patient, LocationContext(Patient::class)) } returns Validation()

        val r4ProfileTransformer2 = mockk<R4ProfileValidator<Patient>> {
            every { validate(any(), LocationContext(Patient::class)) } returns Validation()
        }

        val profileValidator2 = mockk<ProfileValidator<Patient>> {
            every { supportedResource } returns Patient::class
            every { r4Validator } returns r4ProfileTransformer2
            every { profile } returns RoninProfile.PATIENT
            every { qualifies(any()) } returns true
            every { validate(patient, LocationContext(Patient::class)) } returns Validation()
        }

        val service = ValidationService(validationClient, listOf(profileValidator, profileValidator2), listOf())
        val response = service.validate(patient, tenant)
        assertTrue(response)

        verify { validationClient wasNot Called }

        verify(exactly = 1) { profileValidator.qualifies(patient) }
        verify(exactly = 1) { profileValidator2.qualifies(patient) }
        verify(exactly = 1) { profileValidator.validate(patient, LocationContext(Patient::class)) }
        verify(exactly = 1) { profileValidator2.validate(patient, LocationContext(Patient::class)) }
        verify(exactly = 1) { r4ProfileTransformer.validate(patient, LocationContext(Patient::class)) }
        verify(exactly = 1) { r4ProfileTransformer2.validate(patient, LocationContext(Patient::class)) }
    }

    @Test
    fun `validates against contained element with validator`() {
        val reference = Reference(reference = FHIRString("Organization/1234"))
        val patient = Patient(
            managingOrganization = reference
        )

        every { profileValidator.validate(patient, LocationContext(Patient::class)) } returns Validation()

        val referenceValidator = mockk<ElementValidator<Reference>> {
            every { supportedElement } returns Reference::class
            every {
                validate(
                    reference,
                    listOf(RoninProfile.PATIENT),
                    LocationContext(Patient::managingOrganization)
                )
            } returns failedValidation
        }

        val service = ValidationService(validationClient, listOf(profileValidator), listOf(referenceValidator))
        val response = service.validate(patient, tenant)
        assertFalse(response)

        val validation = slot<Validation>()
        verify(exactly = 1) { validationClient.reportIssues(capture(validation), patient, "test") }
        assertEquals(1, validation.captured.issues().size)

        verify(exactly = 1) {
            referenceValidator.validate(
                reference,
                listOf(RoninProfile.PATIENT),
                LocationContext(Patient::managingOrganization)
            )
        }
    }

    @Test
    fun `validates against contained element with no validator`() {
        val reference = Reference(reference = FHIRString("Organization/1234"))
        val patient = Patient(
            managingOrganization = reference
        )

        every { profileValidator.validate(patient, LocationContext(Patient::class)) } returns Validation()

        val service = ValidationService(validationClient, listOf(profileValidator), listOf())
        val response = service.validate(patient, tenant)
        assertTrue(response)

        verify { validationClient wasNot Called }
    }

    @Test
    fun `validates against nested element with validator`() {
        val identifier = Identifier(system = Uri("system"))
        val patient = Patient(
            managingOrganization = Reference(identifier = identifier)
        )

        every { profileValidator.validate(patient, LocationContext(Patient::class)) } returns Validation()

        val identifierValidator = mockk<ElementValidator<Identifier>> {
            every { supportedElement } returns Identifier::class
            every {
                validate(
                    identifier,
                    listOf(RoninProfile.PATIENT),
                    LocationContext("Patient", "managingOrganization.identifier")
                )
            } returns failedValidation
        }

        val service = ValidationService(validationClient, listOf(profileValidator), listOf(identifierValidator))
        val response = service.validate(patient, tenant)
        assertFalse(response)

        val validation = slot<Validation>()
        verify(exactly = 1) { validationClient.reportIssues(capture(validation), patient, "test") }
        assertEquals(1, validation.captured.issues().size)

        verify(exactly = 1) {
            identifierValidator.validate(
                identifier,
                listOf(RoninProfile.PATIENT),
                LocationContext("Patient", "managingOrganization.identifier")
            )
        }
    }

    @Test
    fun `validates against dynamic value with element with validator`() {
        val identifier = Identifier(system = Uri("system"))
        val patient = Patient(
            // This is not a valid type, but proves out this test.
            deceased = DynamicValue(
                DynamicValueType.IDENTIFIER,
                identifier
            )
        )

        every { profileValidator.validate(patient, LocationContext(Patient::class)) } returns Validation()

        val identifierValidator = mockk<ElementValidator<Identifier>> {
            every { supportedElement } returns Identifier::class
            every {
                validate(
                    identifier,
                    listOf(RoninProfile.PATIENT),
                    LocationContext(Patient::deceased)
                )
            } returns failedValidation
        }

        val service = ValidationService(validationClient, listOf(profileValidator), listOf(identifierValidator))
        val response = service.validate(patient, tenant)
        assertFalse(response)

        val validation = slot<Validation>()
        verify(exactly = 1) { validationClient.reportIssues(capture(validation), patient, "test") }
        assertEquals(1, validation.captured.issues().size)

        verify(exactly = 1) {
            identifierValidator.validate(
                identifier,
                listOf(RoninProfile.PATIENT),
                LocationContext(Patient::deceased)
            )
        }
    }

    @Test
    fun `validates against dynamic value with element with no validator`() {
        val identifier = Identifier(system = Uri("system"))
        val patient = Patient(
            // This is not a valid type, but proves out this test.
            deceased = DynamicValue(
                DynamicValueType.IDENTIFIER,
                identifier
            )
        )

        every { profileValidator.validate(patient, LocationContext(Patient::class)) } returns Validation()

        val service = ValidationService(validationClient, listOf(profileValidator), listOf())
        val response = service.validate(patient, tenant)
        assertTrue(response)

        verify { validationClient wasNot Called }
    }

    @Test
    fun `validates against collection of elements with validator`() {
        val identifier1 = Identifier(system = Uri("system"))
        val identifier2 = Identifier(value = FHIRString("value"))
        val identifier3 = Identifier(system = Uri("system"), value = FHIRString("value"))
        val patient = Patient(
            identifier = listOf(identifier1, identifier2, identifier3)
        )

        every { profileValidator.validate(patient, LocationContext(Patient::class)) } returns Validation()

        val identifierValidator = mockk<ElementValidator<Identifier>> {
            every { supportedElement } returns Identifier::class
            every {
                validate(
                    identifier1,
                    listOf(RoninProfile.PATIENT),
                    LocationContext("Patient", "identifier[0]")
                )
            } returns failedValidation
            every {
                validate(
                    identifier2,
                    listOf(RoninProfile.PATIENT),
                    LocationContext("Patient", "identifier[1]")
                )
            } returns failedValidation
            every {
                validate(
                    identifier3,
                    listOf(RoninProfile.PATIENT),
                    LocationContext("Patient", "identifier[2]")
                )
            } returns Validation()
        }

        val service = ValidationService(validationClient, listOf(profileValidator), listOf(identifierValidator))
        val response = service.validate(patient, tenant)
        assertFalse(response)

        val validation = slot<Validation>()
        verify(exactly = 1) { validationClient.reportIssues(capture(validation), patient, "test") }
        assertEquals(2, validation.captured.issues().size)

        verify(exactly = 3) { identifierValidator.validate(any(), listOf(RoninProfile.PATIENT), any()) }
    }

    @Test
    fun `validates against collection of elements with no validator`() {
        val identifier1 = Identifier(system = Uri("system"))
        val identifier2 = Identifier(value = FHIRString("value"))
        val identifier3 = Identifier(system = Uri("system"), value = FHIRString("value"))
        val patient = Patient(
            identifier = listOf(identifier1, identifier2, identifier3)
        )

        every { profileValidator.validate(patient, LocationContext(Patient::class)) } returns Validation()

        val service = ValidationService(validationClient, listOf(profileValidator), listOf())
        val response = service.validate(patient, tenant)
        assertTrue(response)

        verify { validationClient wasNot Called }
    }

    @Test
    fun `validates against collection of dynamic values with element with validator`() {
        data class TestResource(
            val values: List<DynamicValue<Any>>
        ) : DefaultResource<TestResource>()

        val identifier1 = Identifier(system = Uri("system"))
        val identifier2 = Identifier(value = FHIRString("value"))
        val identifier3 = Identifier(system = Uri("system"), value = FHIRString("value"))
        val resource = TestResource(
            values = listOf(
                DynamicValue(DynamicValueType.IDENTIFIER, identifier1),
                DynamicValue(DynamicValueType.IDENTIFIER, identifier2),
                DynamicValue(DynamicValueType.IDENTIFIER, identifier3)
            )
        )

        val r4ProfileTransformer = mockk<R4ProfileValidator<TestResource>> {
            every { validate(any(), LocationContext(TestResource::class)) } returns Validation()
        }

        val profileValidator = mockk<ProfileValidator<TestResource>> {
            every { supportedResource } returns TestResource::class
            every { r4Validator } returns r4ProfileTransformer
            every { profile } returns RoninProfile.APPOINTMENT
            every { qualifies(any()) } returns true
            every { validate(resource, LocationContext(TestResource::class)) } returns Validation()
        }

        val identifierValidator = mockk<ElementValidator<Identifier>> {
            every { supportedElement } returns Identifier::class
            every {
                validate(
                    identifier1,
                    listOf(RoninProfile.APPOINTMENT),
                    LocationContext("TestResource", "values[0]")
                )
            } returns failedValidation
            every {
                validate(
                    identifier2,
                    listOf(RoninProfile.APPOINTMENT),
                    LocationContext("TestResource", "values[1]")
                )
            } returns failedValidation
            every {
                validate(
                    identifier3,
                    listOf(RoninProfile.APPOINTMENT),
                    LocationContext("TestResource", "values[2]")
                )
            } returns Validation()
        }

        every { validationClient.reportIssues(any(), any<TestResource>(), any()) } returns UUID.randomUUID()

        val service = ValidationService(validationClient, listOf(profileValidator), listOf(identifierValidator))
        val response = service.validate(resource, tenant)
        assertFalse(response)

        val validation = slot<Validation>()
        verify(exactly = 1) { validationClient.reportIssues(capture(validation), resource, "test") }
        assertEquals(2, validation.captured.issues().size)

        verify(exactly = 3) { identifierValidator.validate(any(), listOf(RoninProfile.APPOINTMENT), any()) }
    }

    @Test
    fun `validates against collection of dynamic values with element with no validator`() {
        data class TestResource(
            val values: List<DynamicValue<Any>>
        ) : DefaultResource<TestResource>()

        val identifier1 = Identifier(system = Uri("system"))
        val identifier2 = Identifier(value = FHIRString("value"))
        val identifier3 = Identifier(system = Uri("system"), value = FHIRString("value"))
        val resource = TestResource(
            values = listOf(
                DynamicValue(DynamicValueType.IDENTIFIER, identifier1),
                DynamicValue(DynamicValueType.IDENTIFIER, identifier2),
                DynamicValue(DynamicValueType.IDENTIFIER, identifier3)
            )
        )

        val r4ProfileTransformer = mockk<R4ProfileValidator<TestResource>> {
            every { validate(any(), LocationContext(TestResource::class)) } returns Validation()
        }

        val profileValidator = mockk<ProfileValidator<TestResource>> {
            every { supportedResource } returns TestResource::class
            every { r4Validator } returns r4ProfileTransformer
            every { profile } returns RoninProfile.APPOINTMENT
            every { qualifies(any()) } returns true
            every { validate(resource, LocationContext(TestResource::class)) } returns Validation()
        }

        val service = ValidationService(validationClient, listOf(profileValidator), listOf())
        val response = service.validate(resource, tenant)
        assertTrue(response)

        verify { validationClient wasNot Called }
    }

    abstract class DefaultResource<T : Resource<T>> : Resource<T> {
        override val id: Id? = null
        override val implicitRules: Uri? = null
        override val language: Code? = null
        override var meta: Meta? = null
        override val resourceType: String = "TestResource"
    }
}
