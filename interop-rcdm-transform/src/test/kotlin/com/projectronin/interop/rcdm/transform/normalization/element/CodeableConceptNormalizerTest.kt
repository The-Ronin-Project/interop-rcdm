package com.projectronin.interop.rcdm.transform.normalization.element

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class CodeableConceptNormalizerTest {
    private val normalizer = CodeableConceptNormalizer()

    private val tenant = mockk<Tenant>()

    @Test
    fun `element type is correct`() {
        assertEquals(CodeableConcept::class, normalizer.elementType)
    }

    @Test
    fun `text is non-null and non-empty`() {
        val codeableConcept =
            CodeableConcept(
                text = FHIRString("concept text"),
            )

        val response = normalizer.normalize(codeableConcept, tenant)
        assertEquals(codeableConcept, response.element)
        assertFalse(response.removeFromElement)
    }

    @Test
    fun `text is empty`() {
        val codeableConcept =
            CodeableConcept(
                text = FHIRString(""),
                coding = listOf(Coding(display = FHIRString("coding"))),
            )

        val response = normalizer.normalize(codeableConcept, tenant)

        val expectedCodeableConcept =
            CodeableConcept(
                text = FHIRString("coding"),
                coding = listOf(Coding(display = FHIRString("coding"))),
            )

        assertEquals(expectedCodeableConcept, response.element)
        assertFalse(response.removeFromElement)
    }

    @Test
    fun `text value is null`() {
        val codeableConcept =
            CodeableConcept(
                text = FHIRString(null),
                coding = listOf(Coding(display = FHIRString("coding"))),
            )

        val response = normalizer.normalize(codeableConcept, tenant)

        val expectedCodeableConcept =
            CodeableConcept(
                text = FHIRString("coding"),
                coding = listOf(Coding(display = FHIRString("coding"))),
            )

        assertEquals(expectedCodeableConcept, response.element)
        assertFalse(response.removeFromElement)
    }

    @Test
    fun `text is null`() {
        val codeableConcept =
            CodeableConcept(
                text = null,
                coding = listOf(Coding(display = FHIRString("coding"))),
            )

        val response = normalizer.normalize(codeableConcept, tenant)

        val expectedCodeableConcept =
            CodeableConcept(
                text = FHIRString("coding"),
                coding = listOf(Coding(display = FHIRString("coding"))),
            )

        assertEquals(expectedCodeableConcept, response.element)
        assertFalse(response.removeFromElement)
    }

    @Test
    fun `no coding`() {
        val codeableConcept =
            CodeableConcept(
                text = null,
                coding = listOf(),
            )

        val response = normalizer.normalize(codeableConcept, tenant)

        assertEquals(codeableConcept, response.element)
        assertFalse(response.removeFromElement)
    }

    @Test
    fun `single user selected coding`() {
        val codeableConcept =
            CodeableConcept(
                text = null,
                coding = listOf(Coding(userSelected = FHIRBoolean.TRUE, display = FHIRString("coding"))),
            )

        val response = normalizer.normalize(codeableConcept, tenant)

        val expectedCodeableConcept =
            CodeableConcept(
                text = FHIRString("coding"),
                coding = listOf(Coding(userSelected = FHIRBoolean.TRUE, display = FHIRString("coding"))),
            )

        assertEquals(expectedCodeableConcept, response.element)
        assertFalse(response.removeFromElement)
    }

    @Test
    fun `multiple user selected codings`() {
        val codeableConcept =
            CodeableConcept(
                text = null,
                coding =
                    listOf(
                        Coding(userSelected = FHIRBoolean.TRUE, display = FHIRString("coding")),
                        Coding(userSelected = FHIRBoolean.TRUE, display = FHIRString("other-coding")),
                    ),
            )

        val response = normalizer.normalize(codeableConcept, tenant)

        assertEquals(codeableConcept, response.element)
        assertFalse(response.removeFromElement)
    }

    @Test
    fun `multiple non-user selected codings`() {
        val codeableConcept =
            CodeableConcept(
                text = null,
                coding =
                    listOf(
                        Coding(userSelected = FHIRBoolean.FALSE, display = FHIRString("coding")),
                        Coding(userSelected = FHIRBoolean.FALSE, display = FHIRString("other-coding")),
                    ),
            )

        val response = normalizer.normalize(codeableConcept, tenant)

        assertEquals(codeableConcept, response.element)
        assertFalse(response.removeFromElement)
    }

    @Test
    fun `coding with null display`() {
        val codeableConcept =
            CodeableConcept(
                text = null,
                coding = listOf(Coding(display = null)),
            )

        val response = normalizer.normalize(codeableConcept, tenant)

        assertEquals(codeableConcept, response.element)
        assertFalse(response.removeFromElement)
    }

    @Test
    fun `coding with display with null value`() {
        val codeableConcept =
            CodeableConcept(
                text = null,
                coding = listOf(Coding(display = FHIRString(null))),
            )

        val response = normalizer.normalize(codeableConcept, tenant)

        assertEquals(codeableConcept, response.element)
        assertFalse(response.removeFromElement)
    }

    @Test
    fun `coding with empty display`() {
        val codeableConcept =
            CodeableConcept(
                text = null,
                coding = listOf(Coding(display = FHIRString(""))),
            )

        val response = normalizer.normalize(codeableConcept, tenant)

        assertEquals(codeableConcept, response.element)
        assertFalse(response.removeFromElement)
    }
}
