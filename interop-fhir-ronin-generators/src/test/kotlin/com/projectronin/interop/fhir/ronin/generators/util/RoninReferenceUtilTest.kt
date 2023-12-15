package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.ReferenceGenerator
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.resource.TENANT_MNEMONIC
import com.projectronin.test.data.generator.NullDataGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Suppress("ktlint:standard:max-line-length")
class RoninReferenceUtilTest {
    private val subjectOptions = listOf("Location", "Group")
    private val subject = ReferenceGenerator()

    @Test
    fun `generate rcdm reference`() {
        val roninRef = rcdmReference("Patient", "1234")
        assertEquals(roninRef.type?.value, "Patient")
        assertEquals(roninRef.reference, "Patient/1234".asFHIR())
        assertEquals(roninRef.type?.extension, dataAuthorityExtension)
    }

    @Test
    fun `generate rcdm reference for profile when r4 reference generator is the initial input`() {
        val subjectReference = subject.generate()
        val roninRef = generateReference(subjectReference, subjectOptions, TENANT_MNEMONIC, "Group", "99")
        assertEquals(roninRef.reference, "Group/test-99".asFHIR())
        assertEquals(roninRef.type?.extension, dataAuthorityExtension)
        assertTrue(roninRef.type?.value in subjectOptions)
    }

    @Test
    fun `generate subject when no input id and r4 reference generator is the initial input`() {
        val subjectReference = subject.generate()
        val roninSubject = generateReference(subjectReference, subjectOptions, TENANT_MNEMONIC, "Group")
        assertEquals(roninSubject.type?.extension, dataAuthorityExtension)
        assertTrue(roninSubject.type?.value in subjectOptions)
    }

    @Test
    fun `generate subject when empty input id and r4 reference generator is the initial input`() {
        val subjectReference = subject.generate()
        val roninSubject = generateReference(subjectReference, subjectOptions, TENANT_MNEMONIC, "Group", "")
        assertEquals(roninSubject.type?.extension, dataAuthorityExtension)
        assertTrue(roninSubject.type?.value in subjectOptions)
    }

    @Test
    fun `generate subject when no input type and r4 reference generator is the initial input`() {
        val subjectReference = subject.generate()
        val roninSubject = generateReference(subjectReference, subjectOptions, TENANT_MNEMONIC, null, "123")
        assertEquals(roninSubject.type?.extension, dataAuthorityExtension)
        assertTrue(roninSubject.type?.value in subjectOptions)
    }

    @Test
    fun `generate subject when empty input type and r4 reference generator is the initial input`() {
        val subjectReference = subject.generate()
        val roninSubject = generateReference(subjectReference, subjectOptions, TENANT_MNEMONIC, "", "123")
        assertEquals(roninSubject.type?.extension, dataAuthorityExtension)
        assertTrue(roninSubject.type?.value in subjectOptions)
    }

    @Test
    fun `generate subject when good input type, no input id, and unusable reference provided`() {
        val subjectReference = Reference(reference = "Practitioner/123".asFHIR())
        assertEquals("Practitioner/123", subjectReference.reference?.value)
        assertNull(subjectReference.type)

        val roninSubject = generateReference(subjectReference, subjectOptions, TENANT_MNEMONIC, "Group")
        assertNotEquals("Practitioner/123", roninSubject.reference?.value)
        assertEquals(roninSubject.type?.extension, dataAuthorityExtension)
        assertEquals("Group", roninSubject.type?.value)
    }

    @Test
    fun `generate subject when good input type, empty input id, and unusable reference provided`() {
        val subjectReference = Reference(reference = "Practitioner/123".asFHIR())
        assertEquals("Practitioner/123", subjectReference.reference?.value)
        assertNull(subjectReference.type)

        val roninSubject = generateReference(subjectReference, subjectOptions, TENANT_MNEMONIC, "Group", "")
        assertNotEquals("Practitioner/123", roninSubject.reference?.value)
        assertEquals(roninSubject.type?.extension, dataAuthorityExtension)
        assertTrue(roninSubject.type?.value in subjectOptions)
    }

    @Test
    fun `generate subject when no input type and unusable reference provided`() {
        val subjectReference = Reference(reference = "Practitioner/123".asFHIR())
        assertEquals("Practitioner/123", subjectReference.reference?.value)
        assertNull(subjectReference.type)

        val roninSubject = generateReference(subjectReference, subjectOptions, TENANT_MNEMONIC, null, "123")
        assertNotEquals("Practitioner/123", roninSubject.reference?.value)
        assertEquals(roninSubject.type?.extension, dataAuthorityExtension)
        assertTrue(roninSubject.type?.value in subjectOptions)
    }

    @Test
    fun `generate subject when empty input type and unusable reference provided`() {
        val subjectReference = Reference(reference = "Practitioner/123".asFHIR())
        assertEquals("Practitioner/123", subjectReference.reference?.value)
        assertNull(subjectReference.type)

        val roninSubject = generateReference(subjectReference, subjectOptions, TENANT_MNEMONIC, "", "123")
        assertNotEquals("Practitioner/123", roninSubject.reference?.value)
        assertEquals(roninSubject.type?.extension, dataAuthorityExtension)
        assertTrue(roninSubject.type?.value in subjectOptions)
    }

    @Test
    fun `generate subject when no input type or id and unusable reference provided`() {
        val subjectReference = Reference(reference = "Practitioner/123".asFHIR())
        assertEquals("Practitioner/123", subjectReference.reference?.value)
        assertNull(subjectReference.type)

        val roninSubject = generateReference(subjectReference, subjectOptions, TENANT_MNEMONIC)
        assertNotEquals("Practitioner/123", roninSubject.reference?.value)
        assertEquals(roninSubject.type?.extension, dataAuthorityExtension)
        assertTrue(roninSubject.type?.value in subjectOptions)
    }

    @Test
    fun `use reference provided when no input id and reference is valid rcdm`() {
        val subjectReference = rcdmReference("Practitioner", "test-123")
        assertEquals(dataAuthorityExtension, subjectReference.type?.extension)
        assertEquals("Practitioner", subjectReference.type?.value)

        val roninSubject = generateReference(subjectReference, subjectOptions, TENANT_MNEMONIC, "Patient")
        assertEquals(subjectReference, roninSubject)
    }

    @Test
    fun `throws error when bad input type, no input id, and unusable reference provided`() {
        val subjectReference = Reference(reference = "Practitioner/123".asFHIR())
        assertEquals("Practitioner/123", subjectReference.reference?.value)
        assertNull(subjectReference.type)

        val exception =
            assertThrows<IllegalArgumentException> {
                generateReference(subjectReference, subjectOptions, TENANT_MNEMONIC, "Patient", "")
            }
        assertEquals(
            "Patient is not one of Location, Group",
            exception.message,
        )
    }

    @Test
    fun `throws error when bad input type, empty input id, and unusable reference provided`() {
        val subjectReference = Reference(reference = "Practitioner/123".asFHIR())
        assertEquals("Practitioner/123", subjectReference.reference?.value)
        assertNull(subjectReference.type)

        val exception =
            assertThrows<IllegalArgumentException> {
                generateReference(subjectReference, subjectOptions, TENANT_MNEMONIC, "Patient", "")
            }
        assertEquals(
            "Patient is not one of Location, Group",
            exception.message,
        )
    }

    @Test
    fun `generate optional reference with null initial input and type is in allowed types and non-null id`() {
        val subjectReference = null
        val roninRef = generateOptionalReference(subjectReference, listOf("Patient"), TENANT_MNEMONIC, "Patient", "1234")
        assertEquals("Patient", roninRef?.type?.value)
        assertEquals("Patient/test-1234".asFHIR(), roninRef?.reference)
        assertEquals(dataAuthorityExtension, roninRef?.type?.extension)
    }

    @Test
    fun `throw error from generate optional reference with bad (non-empty) initial input and type is not in allowed types and non-null id`() {
        val subjectReference = Reference(reference = "ref".asFHIR())

        val exception =
            assertThrows<IllegalArgumentException> {
                generateOptionalReference(subjectReference, listOf("Patient"), TENANT_MNEMONIC, "Practitioner", "1234")
            }
        assertEquals(
            "Practitioner is not Patient",
            exception.message,
        )
    }

    @Test
    fun `generate optional reference with null generated initial input and type is in allowed types and non-null id`() {
        val subjectReference = NullDataGenerator<Reference>().generate()
        val roninRef = generateOptionalReference(subjectReference, listOf("Patient"), TENANT_MNEMONIC, "Patient", "1234")
        assertEquals("Patient", roninRef?.decomposedType())
        assertEquals("Patient/test-1234".asFHIR(), roninRef?.reference)
        assertEquals(dataAuthorityExtension, roninRef?.type?.extension)
    }

    @Test
    fun `throw error from generate optional reference with bad (non-empty) initial input and type is not in allowed types and empty id`() {
        val subjectReference = Reference(reference = "ref".asFHIR())

        val exception =
            assertThrows<IllegalArgumentException> {
                generateOptionalReference(subjectReference, listOf("Patient"), TENANT_MNEMONIC, "Practitioner", "")
            }
        assertEquals(
            "Practitioner is not Patient",
            exception.message,
        )
    }

    @Test
    fun `generate optional reference with bad (non-empty) initial input and type is in allowed types and null id`() {
        val subjectReference = Reference(reference = "ref".asFHIR())
        val roninRef = generateOptionalReference(subjectReference, listOf("Patient"), TENANT_MNEMONIC, "Patient", null)
        assertEquals("Patient", roninRef?.decomposedType())
        assertNotEquals(5, roninRef?.decomposedId()?.length)
        assertEquals(dataAuthorityExtension, roninRef?.type?.extension)
    }

    @Test
    fun `throw error from optional reference with bad (non-empty) initial input and type is not in allowed types and non-null id`() {
        val subjectReference = Reference(reference = "ref".asFHIR())

        val exception =
            assertThrows<IllegalArgumentException> {
                generateOptionalReference(subjectReference, listOf("Patient"), TENANT_MNEMONIC, "Practitioner", "1234")
            }
        assertEquals(
            "Practitioner is not Patient",
            exception.message,
        )
    }
}
